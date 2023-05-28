package il.blackraven.klita;

import com.github.f4b6a3.ulid.Ulid;
import il.blackraven.klita.orm.*;
import il.blackraven.klita.workers.JobWorker;
import il.blackraven.klita.workers.NotificationWorker;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static il.blackraven.klita.orm.States.*;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class KlitaLaundryBot extends AbilityBot {

    private static final Logger log = LogManager.getLogger(KlitaLaundryBot.class);
    ResponseHandler responseHandler;

    private static final long INIT_DELAY = 1;
    private static final long JOB_WORKER_RATE = 5;
    private static final long NOTIFICATION_WORKER_RATE = 5;

    private static final Locales DEFAULT_LOCALE = Locales.en_US;

    public KlitaLaundryBot(String bot_token, String bot_username) {
        super(bot_token, bot_username);
        responseHandler = new ResponseHandler(sender);

        ScheduledExecutorService jobWorker = Executors.newSingleThreadScheduledExecutor();
        jobWorker.scheduleAtFixedRate(new JobWorker(),
                INIT_DELAY,
                JOB_WORKER_RATE,
                TimeUnit.SECONDS);


        ScheduledExecutorService notificationWorker = Executors.newSingleThreadScheduledExecutor();
        notificationWorker.scheduleAtFixedRate(new NotificationWorker(this),
                INIT_DELAY,
                NOTIFICATION_WORKER_RATE,
                TimeUnit.SECONDS);
    }

    @Override
    public long creatorId() {
        return Long.parseLong(BotConfig.getProperty("bot.creatorId"));
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        Update update = updates.get(updates.size() - 1);
        long chatIdLong = getChatId(update);
        String chatId = String.valueOf(getChatId(update));
        log.info(String.format("Update from %s received", chatId));
        User user =
                update.hasMessage() ? update.getMessage().getFrom() :
                update.hasCallbackQuery() ? update.getCallbackQuery().getFrom() : new User(0L, "TEST", false);
        Locales userLocale = Localisation.getUserLocale(chatIdLong, user.getLanguageCode());

        if (update.hasCallbackQuery()) {
            log.debug(update.getCallbackQuery().getMessage().getText() + ": " + update.getCallbackQuery().getData());
            EventLogger.log(chatIdLong, update.getCallbackQuery().getMessage().getMessageId(), Event.CALLBACK, update.getCallbackQuery().getData(), update);

            //Callbacks have structure:
            //STATE::CALLBACK_NAME[::ARGUMENTS]
            //Tried to save state into DB, but faced with problem, when user may call new action
            //while being in some state (ex. Wait notification about end and call /report command)
            //So after end report it's difficult to return to previous state.
            //Instead trying to
            String cbData = update.getCallbackQuery().getData();
            String[] cbArray = cbData.split("::");
            States state = States.valueOf(cbArray[0]);
            String callback = cbArray[1];
            int machineId;
            Machine machine;
            MachineTypes type;
            String program;
            ArrayList<Machine> machines;


            AnswerCallbackQuery ans = new AnswerCallbackQuery();
            ans.setShowAlert(false);
            ans.setCallbackQueryId(update.getCallbackQuery().getId());
            silent.execute(ans);

            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());


            switch (state) {
                case USE_CHOOSE_MACHINE:
                    type = MachineTypes.valueOf(cbArray[2]);
                    machineId = Integer.parseInt(cbArray[3]);
                    if (type.equals(MachineTypes.wash)) {
                        editMessageText.setText(Localisation.getMessage("USE_CHOOSE_PROGRAM_WASH", userLocale));
                    } else if (type.equals(MachineTypes.dry)) {
                        editMessageText.setText(Localisation.getMessage("USE_CHOOSE_PROGRAM_DRY", userLocale));
                    }
                    editMessageText.setReplyMarkup(KeyboardFactory.machineChooseProgram(machineId, userLocale));
                    silent.execute(editMessageText);
                    EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                    break;
                case USE_CHOOSE_PROGRAM:
                    type = MachineTypes.valueOf(cbArray[2]);
                    machineId = Integer.parseInt(cbArray[3]);
                    program = cbArray[4];

                    if (program.equals(ProgramCallbacks.CUSTOM_DURATION.name())) {
                        
                    }

                    DataUtils.putNewJob(prepareNewJob(JobStatus.JOB_STARTED, getChatId(update), machineId, program));
                    DataUtils.machineChangeStatus(machineId, MachineStatuses.BUSY);

                    String localVerb = Localisation.getMessage("VERB_" + type.toString().toUpperCase(), userLocale);
                    editMessageText.setText(Localisation.getMessage("USE_AWAIT_NOTIFICATION_BASE", userLocale, localVerb));
                    String cancelCallback = String.valueOf(type.equals(MachineTypes.wash) ?
                            MachineCallbacks.CANCEL_NEW_WASH :
                            MachineCallbacks.CANCEL_NEW_DRY);
                    InlineKeyboardMarkup keyboard = KeyboardFactory.chooseMachineAll(USE_AWAIT_NOTIFICATION, true, cancelCallback, userLocale);
                    editMessageText.setReplyMarkup(keyboard);
                    if (keyboard == null) {
                        editMessageText.setText(editMessageText.getText() + "\n" +
                                Localisation.getMessage("USE_CHOOSE_MACHINE_NO_AVAILABLE", userLocale));
                    } else {
                        editMessageText.setText(editMessageText.getText() + "\n" +
                                        Localisation.getMessage("USE_AWAIT_NOTIFICATION_NEW_ONE", userLocale));
                    }
                    silent.execute(editMessageText);
                    EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                    break;
                case USE_AWAIT_NOTIFICATION:
                    MachineCallbacks machineCallback = MachineCallbacks.valueOf(callback);
                    type = MachineTypes.valueOf(cbArray[2]);
                    switch (machineCallback) {
                        case CHOOSE_ANOTHER_ONE:
                            if (type.equals(MachineTypes.wash)) {
                                machineId = Integer.parseInt(cbArray[3]);
                                //TODO вынести все действия в отдельные функции, здесь переиспользовать хэндлер для выбора программы
                                editMessageText.setText(Localisation.getMessage("USE_CHOOSE_PROGRAM_WASH", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.machineChooseProgram(machineId, userLocale));
                            } else if (type.equals(MachineTypes.dry)) {
                                machineId = Integer.parseInt(cbArray[3]);
                                editMessageText.setText(Localisation.getMessage("USE_CHOOSE_PROGRAM_DRY", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.machineChooseProgram(machineId, userLocale));
                            }
                            break;
                        case CANCEL_NEW_WASH:
                            editMessageText.setText(Localisation.getMessage("USE_AWAIT_NOTIFICATION_BASE", userLocale,
                                    Localisation.getMessage("NOUN_WASH", userLocale)));
                            editMessageText.setReplyMarkup(null);
                            break;
                        case CANCEL_NEW_DRY:
                            editMessageText.setText(Localisation.getMessage("USE_AWAIT_NOTIFICATION_BASE", userLocale,
                                    Localisation.getMessage("NOUN_DRY", userLocale)));
                            editMessageText.setReplyMarkup(null);
                            break;
                    }
                    silent.execute(editMessageText);
                    EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                    break;

                case USE_END:
                    JobCallbacks jobCallback = JobCallbacks.valueOf(callback);
                    UUID jobId = UUID.fromString(cbArray[3]);
                    Job job = DataUtils.jobBrowse(jobId);
                    DataUtils.declineNotification(job.getNotificationId());
                    DataUtils.jobChangeStatus(jobId, JobStatus.JOB_ENDED);
                    DataUtils.machineChangeStatus(job.getMachineId(), MachineStatuses.IDLE);
                    switch (jobCallback) {
                        case JOB_ST_NEW:
                            //TODO reuse "/use" handler
                            InlineKeyboardMarkup keyboardNew = KeyboardFactory.chooseMachineAll(States.USE_CHOOSE_MACHINE,
                                    false,
                                    null,
                                    userLocale);
                            editMessageText.setReplyMarkup(keyboardNew);
                            if (keyboardNew == null) {
                                editMessageText.setText(Localisation.getMessage("USE_CHOOSE_MACHINE_NO_AVAILABLE", userLocale));
                            } else {
                                editMessageText.setText(Localisation.getMessage("USE_CHOOSE_MACHINE", userLocale));
                            }
                            silent.execute(editMessageText);
                            EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            break;
                        case JOB_END:
                            editMessageText.setText(update.getCallbackQuery().getMessage().getText());
                            editMessageText.setReplyMarkup(null);
                            silent.execute(editMessageText);
                            EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            break;
                    }

                case REPORT_START:

                    if (cbArray[cbArray.length - 1].equals(ReportCallbacks.DISAGREE_OVERWRITE.name())) {
                        DeleteMessage deleteReportMessage = new DeleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());
                        silent.execute(deleteReportMessage);
                        EventLogger.log(chatIdLong, deleteReportMessage.getMessageId(), Event.CALLBACK_REPLY, "DELETE MESSAGE", deleteReportMessage);
                        break;
                    }

                    ReportCallbacks reportCallback = ReportCallbacks.valueOf(callback);
                    switch (reportCallback) {
                        case ALL_IDLE:
                            machines = DataUtils.getAllMachines();

                            // if machine busy by someone's job, warn user
                            if (!cbArray[cbArray.length - 1].equals(ReportCallbacks.AGREE_OVERWRITE.name())) {
                                List<Machine> busyMachines = machines.stream().filter(m -> m.getStatus() == MachineStatuses.BUSY).collect(Collectors.toList());
                                if (!busyMachines.isEmpty()) {
                                    List<Job> startedJobs = DataUtils.browseAllActiveJobs();
                                    if (startedJobs.stream().anyMatch(j -> j.getUserTgId() != 0)) {
                                        editMessageText.setText(Localisation.getMessage("REPORT_WARN_ONE_OF_MACHINES_IN_USE", userLocale));
                                        editMessageText.setReplyMarkup(KeyboardFactory.reportOverwriteJobKeyboard(cbData, userLocale));
                                        silent.execute(editMessageText);
                                        EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                                        break;
                                    }
                                }
                            }

                            machines.forEach(m -> DataUtils.machineChangeStatus(m.getId(), MachineStatuses.IDLE));

                            editMessageText.setText(Localisation.getMessage("REPORT_START_ALL_IDLE", userLocale));
                            editMessageText.setReplyMarkup(null);
                            silent.execute(editMessageText);
                            EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            break;
                        case CANCEL_REPORT:
                            editMessageText.setText(Localisation.getMessage("REPORT_START_CANCEL", userLocale));
                            editMessageText.setReplyMarkup(null);
                            silent.execute(editMessageText);
                            EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            break;

                        case SET_ALL:
                            DeleteMessage deleteSetAllMessage = new DeleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());
                            silent.execute(deleteSetAllMessage);
                            EventLogger.log(chatIdLong, deleteSetAllMessage.getMessageId(), Event.CALLBACK_REPLY, "DELETE MESSAGE", deleteSetAllMessage);
                            machines = DataUtils.getAllMachines();
                            for (Machine setMachine : machines) {
                                //TODO reuse SET_SPECIFIC_MACHINE handler
                                // if machine busy by someone's job, warn user
                                if (!cbArray[cbArray.length - 1].equals(ReportCallbacks.AGREE_OVERWRITE.name())) {
                                    if (setMachine.getStatus() == MachineStatuses.BUSY) {
                                        Job machineJob = DataUtils.browseActiveJobByMachineId(setMachine.getId());
                                        if (machineJob != null && machineJob.getUserTgId() != 0) {
                                            String specificCallback = REPORT_START + "::" +
                                                    ReportCallbacks.SET_SPECIFIC_MACHINE + "::" +
                                                    setMachine.getId();
                                            SendMessage tmpMessage = new SendMessage();
                                            tmpMessage.setChatId(chatId);
                                            tmpMessage.setText(Localisation.getMessage("REPORT_WARN_MACHINE_IN_USE", userLocale, setMachine.getLaundry_id()));
                                            tmpMessage.setReplyMarkup(KeyboardFactory.reportOverwriteJobKeyboard(specificCallback, userLocale));
                                            silent.execute(tmpMessage);
                                            EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                                            continue;
                                        }
                                    }
                                }
                                SendMessage tmpMessage = new SendMessage();
                                tmpMessage.setChatId(chatId);
                                tmpMessage.setText(Localisation.getMessage("REPORT_START_SET_MACHINE", userLocale, setMachine.getLaundry_id()));
                                tmpMessage.setReplyMarkup(KeyboardFactory.reportMachineBusyKeyboard(setMachine.getId(), REPORT_SET_ALL, userLocale));
                                silent.execute(tmpMessage);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            }
                            break;
                        case SET_SPECIFIC_MACHINE:
                            machineId = Integer.parseInt(cbArray[2]);
                            machine = DataUtils.getMachineById(machineId);
                            // if machine busy by someone's job, warn user
                            if (!cbArray[cbArray.length - 1].equals(ReportCallbacks.AGREE_OVERWRITE.name())) {
                                if (machine.getStatus() == MachineStatuses.BUSY) {
                                    Job machineJob = DataUtils.browseActiveJobByMachineId(machineId);
                                    if (machineJob != null && machineJob.getUserTgId() != 0) {
                                        editMessageText.setText(Localisation.getMessage("REPORT_WARN_MACHINE_IN_USE", userLocale, machine.getLaundry_id()));
                                        editMessageText.setReplyMarkup(KeyboardFactory.reportOverwriteJobKeyboard(cbData, userLocale));
                                        silent.execute(editMessageText);
                                        EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                                        break;
                                    }
                                }
                            }
                            if (machine == null) {
                                log.error("No machine with id " + machineId);
                                break;
                            }
                            editMessageText.setText(Localisation.getMessage("REPORT_START_SET_MACHINE", userLocale, machine.getLaundry_id()));
                            editMessageText.setReplyMarkup(KeyboardFactory.reportMachineBusyKeyboard(machine.getId(), REPORT_SET_SPECIFIC_MACHINE, userLocale));
                            silent.execute(editMessageText);
                            EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            break;
                    }
                    break;

                case REPORT_SET_SPECIFIC_MACHINE:
                case REPORT_SET_ALL:
                    reportCallback = ReportCallbacks.valueOf(callback);
                    machineId = Integer.parseInt(cbArray[2]);
//                    Job machineJob;
                    switch (reportCallback) {
                        case MACHINE_FREE:
                            DataUtils.machineChangeStatus(machineId, MachineStatuses.IDLE);
//                            machineJob = DataUtils.browseActiveJobByMachineId(machineId);
//                            if (machineJob != null) {
//                                DataUtils.jobChangeStatus(machineJob.getId(), JobStatus.JOB_ENDED);
//                            }
                            break;
                        case CANCEL_REPORT:
                            break;
                        case OUT_OF_ORDER:
                            DataUtils.machineChangeStatus(machineId, MachineStatuses.OUT_OF_ORDER);
                            break;
                        default:
                            DataUtils.machineChangeStatus(machineId, MachineStatuses.BUSY);
                            int duration = ReportCallbacks.getCallbackDuration(reportCallback);
                            Job emptyJob = prepareEmptyJob(JobStatus.JOB_END_NOT_CONFIRMED, machineId, duration);
                            DataUtils.putNewJob(emptyJob);
                            break;
                    }
                    try {
                        DeleteMessage deleteReportMessage = DeleteMessage.builder().chatId(chatId).messageId(update.getCallbackQuery().getMessage().getMessageId()).build();
                        sender.execute(deleteReportMessage);
                        EventLogger.log(chatIdLong, deleteReportMessage.getMessageId(), Event.CALLBACK_REPLY, "DELETE MESSAGE", deleteReportMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    break;

                case SETTINGS:
                    switch (SettingsCallbacks.valueOf(callback)) {
                        case LANGUAGE:
                            if (cbArray[2].equals("SHOW")) {
                                editMessageText.setText(Localisation.getMessage("SETTINGS_LANGUAGE", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.languageChooseKeyboard(userLocale));
                                silent.execute(editMessageText);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                                break;
                            } else if (cbArray[2].equals("SET")) {
                                Locales newLocale = Locales.valueOf(cbArray[3]);
                                Localisation.changeUserLocale(chatIdLong, newLocale);
                                userLocale = newLocale;

                                editMessageText.setText(Localisation.getMessage("SETTINGS_LANGUAGE", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.languageChooseKeyboard(userLocale));
                                silent.execute(editMessageText);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            } else if (cbArray[2].equals("BACK")) {
                                //TODO ? reuse /settings handler
                                editMessageText.setText(Localisation.getMessage("SETTINGS_INTRO", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.settingsMainKeyboard(userLocale));
                                silent.execute(editMessageText);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            }
                            break;
                        case NOTIFICATIONS:
                            if (cbArray[2].equals("SHOW")) {
                                editMessageText.setText(Localisation.getMessage("SETTINGS_NOTIFICATIONS", userLocale, "5", "minutes"));//TODO
                                editMessageText.setReplyMarkup(null);
                                silent.execute(editMessageText);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                                break;
                            } else if (cbArray[2].equals("SET")) {
                                //TODO ? reuse /settings handler
                                editMessageText.setText(Localisation.getMessage("SETTINGS_INTRO", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.settingsMainKeyboard(userLocale));
                                silent.execute(editMessageText);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            } else if (cbArray[2].equals("BACK")) {
                                //TODO ? reuse /settings handler
                                editMessageText.setText(Localisation.getMessage("SETTINGS_INTRO", userLocale));
                                editMessageText.setReplyMarkup(KeyboardFactory.settingsMainKeyboard(userLocale));
                                silent.execute(editMessageText);
                                EventLogger.log(chatIdLong, editMessageText.getMessageId(), Event.CALLBACK_REPLY, editMessageText.getText(), editMessageText);
                            }

                        case CLOSE:
                            DeleteMessage deleteSettingsMessage = DeleteMessage.builder().chatId(chatId).messageId(update.getCallbackQuery().getMessage().getMessageId()).build();
                            silent.execute(deleteSettingsMessage);
                            EventLogger.log(chatIdLong, deleteSettingsMessage.getMessageId(), Event.CALLBACK_REPLY, "DELETE MESSAGE", deleteSettingsMessage);
                            break;
                    }
                    break;
            }

        }

        if (update.hasMessage()) {
            EventLogger.log(chatIdLong, update.getMessage().getMessageId(), Event.COMMAND, update.getMessage().getText(), update);
            switch (update.getMessage().getText()) {
                case "/start":
                    if (DataUtils.getUser(chatId) == null) {
                        BotUser newUser = new BotUser(chatIdLong,
                                user.getFirstName(),
                                Localisation.getUserLocale(chatIdLong, user.getLanguageCode()));
                        DataUtils.addNewUser(newUser);
                    }
                    SendMessage message = new SendMessage();
                    message.setText(Localisation.getMessage("START_MESSAGE", userLocale));
                    message.setChatId(chatId);
                    try {
                        Message sentMsg = sender.execute(message);
                        EventLogger.log(chatIdLong, sentMsg.getMessageId(), Event.COMMAND_REPLY, sentMsg.getText(), sentMsg);
                    } catch (TelegramApiException tae) {
                        tae.printStackTrace();
                    }
                    break;


                case "/use":
                    SendMessage msgUse = new SendMessage();
                    msgUse.setChatId(chatId);

                    InlineKeyboardMarkup keyboard = KeyboardFactory.chooseMachineAll(States.USE_CHOOSE_MACHINE,
                            false,
                            null,
                            userLocale);
                    msgUse.setReplyMarkup(keyboard);
                    if (keyboard == null) {
                        msgUse.setText(Localisation.getMessage("USE_CHOOSE_MACHINE_NO_AVAILABLE", userLocale));
                    } else {
                        msgUse.setText(Localisation.getMessage("USE_CHOOSE_MACHINE", userLocale));
                    }

                    try {
                        Message sentMsg = sender.execute(msgUse);
                        EventLogger.log(chatIdLong, sentMsg.getMessageId(), Event.COMMAND_REPLY, sentMsg.getText(), sentMsg);
                    } catch (TelegramApiException tae) {
                        tae.printStackTrace();
                    }
                    break;


                case "/status":
                    SendMessage msgStatus = new SendMessage();
                    msgStatus.setChatId(chatId);
                    StringBuilder sb = new StringBuilder();
                    sb.append(Localisation.getMessage("STATUS_FIRST_LINE", userLocale)).append("\n");
                    ArrayList<Machine> machines = DataUtils.getAllMachines();
                    for (Machine machine :
                            machines) {
                        sb.append(MachineTypes.getTypeEmoji(machine.getType()));
                        sb.append("№");
                        sb.append(machine.getLaundry_id());
                        sb.append(": ");
                        sb.append(MachineStatuses.getStatusEmoji(machine.getStatus()));
                        sb.append(" ");
                        sb.append(Localisation.getMessage(MachineStatuses.getStatusName(machine.getStatus()), userLocale));
                        sb.append(" ");
                        if (machine.getStatus() == MachineStatuses.BUSY) {
                            Job activeJob = DataUtils.browseActiveJobByMachineId(machine.getId());
                            long leftTime = 0L;
                            if (activeJob != null) {
                                Timestamp endTime = activeJob.getExpiry();
                                leftTime = TimeUnit.MILLISECONDS.toMinutes(endTime.getTime() - System.currentTimeMillis());
                            }
                            if (leftTime < -1) {
                                sb.append(Localisation.getMessage("STATUS_JOB_END_BUSY", userLocale,
                                        Math.abs(leftTime),
                                        Localisation.getMessage("NOUN_MINUTES", userLocale)));
                            } else if (leftTime < 1) {
                                sb.append(Localisation.getMessage("STATUS_JOB_END_BUSY_RIGHT_NOW", userLocale));
                            } else if (leftTime < 60) {
                                sb.append(Localisation.getMessage("STATUS_TIME_LEFT", userLocale,
                                        leftTime,
                                        Localisation.getMessage("NOUN_MINUTES", userLocale)));
                            } else {
                                sb.append(Localisation.getMessage("STATUS_TIME_LEFT", userLocale,
                                        leftTime,
                                        Localisation.getMessage("NOUN_HOURS", userLocale)));
                            }
                        } else {
                            long timeFromLastUpd = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() -
                                    machine.getStatusLastUpdate().getTime());
                            if (timeFromLastUpd < 1) {
                                sb.append(Localisation.getMessage("STATUS_TIME_UPD_RIGHT_NOW", userLocale));
                            } else if (timeFromLastUpd < 60) {
                                sb.append(Localisation.getMessage("STATUS_IDLE_LAST_UPD", userLocale,
                                        timeFromLastUpd,
                                        Localisation.getMessage("NOUN_MINUTES", userLocale)));
                            } else {
                                sb.append(Localisation.getMessage("STATUS_IDLE_LAST_UPD", userLocale,
                                        timeFromLastUpd / 60,
                                        Localisation.getMessage("NOUN_HOURS", userLocale)));
                            }
                        }
                        sb.append("\n");
                    }
                    msgStatus.setText(sb.toString());
                    try {
                        Message sentMsg = sender.execute(msgStatus);
                        EventLogger.log(chatIdLong, sentMsg.getMessageId(), Event.COMMAND_REPLY, sentMsg.getText(), sentMsg);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;


                case "/report":
                    SendMessage reportMessage = new SendMessage();
                    reportMessage.setChatId(chatId);
                    reportMessage.setText(Localisation.getMessage("REPORT_INTRO", userLocale));
                    reportMessage.setReplyMarkup(KeyboardFactory.reportStartKeyboard(userLocale));
                    try {
                        Message sentMsg = sender.execute(reportMessage);
                        EventLogger.log(chatIdLong, sentMsg.getMessageId(), Event.COMMAND_REPLY, sentMsg.getText(), sentMsg);
                    } catch (TelegramApiException tae) {
                        tae.printStackTrace();
                    }
                    break;

                case "/settings":
                    SendMessage settingsMessage = new SendMessage();
                    settingsMessage.setChatId(chatId);
                    settingsMessage.setText(Localisation.getMessage("SETTINGS_INTRO", userLocale));
                    settingsMessage.setReplyMarkup(KeyboardFactory.settingsMainKeyboard(userLocale));
                    try {
                        Message sentMsg = sender.execute(settingsMessage);
                        EventLogger.log(chatIdLong, sentMsg.getMessageId(), Event.COMMAND_REPLY, sentMsg.getText(), sentMsg);
                    } catch (TelegramApiException tae) {
                        tae.printStackTrace();
                    }
                    break;

                default:
                    SendMessage msgDefault = new SendMessage();
                    log.debug("Unknown command: " + update.getMessage().getText());
                    msgDefault.setText(Localisation.getMessage("UNKNOWN_COMMAND", userLocale, update.getMessage().getText()));
                    msgDefault.setChatId(chatId);
                    try {
                        Message sentMsg = sender.execute(msgDefault);
                        EventLogger.log(chatIdLong, sentMsg.getMessageId(), Event.COMMAND_REPLY, sentMsg.getText(), sentMsg);
                    } catch (TelegramApiException tae) {
                        tae.printStackTrace();
                    }
            }
        }
    }


    private Job prepareNewJob(int status, long tgId, int machineId, String internalProgramName) {

        UUID uuid = Ulid.fast().toUuid();
        Timestamp putTime = new Timestamp(System.currentTimeMillis());

        Machine machine = DataUtils.getMachineById(machineId);
        Program program = DataUtils.getProgramByMachineAndInternalName(machine.getId(), internalProgramName);
        if (machine == null || program == null) {
            log.warn(String.format("Program %s on machine %s does not exist", internalProgramName, machineId));
            throw new NullPointerException();
        }
        Timestamp expiry = new Timestamp(putTime.getTime() + TimeUnit.MINUTES.toMillis(program.getDuration()));

        return new Job(uuid, status, putTime, expiry, machine.getId(), program.getId(), tgId, null);
    }

    private Job prepareEmptyJob(int status, int machineId, int duration) {

        Job job = prepareNewJob(status, 0, machineId, "unknown_prog");
        Timestamp expiry = new Timestamp(job.getPutTime().getTime() + TimeUnit.MINUTES.toMillis(duration));
        job.setExpiry(expiry);

        return job;
    }

    @Override
    public void onClosing() {
        super.onClosing();
    }

}
