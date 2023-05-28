package il.blackraven.klita;

import il.blackraven.klita.orm.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class KeyboardFactory {
    public static InlineKeyboardMarkup chooseMachineAll(States state, boolean addCancelButton, String cancelButtonCallback, Locales locale) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> washRow = new ArrayList<>();
        List<InlineKeyboardButton> dryRow = new ArrayList<>();
        ArrayList<Machine> machines = DataUtils.getAllIdleMachines();
        if (machines.isEmpty()) return null;

        for (Machine machine : machines) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            String machineName = Localisation.getMessage(machine.getType().toString().toUpperCase() + "_MACHINE_TYPE_NAME", locale);
            btn.setText(MachineTypes.getTypeEmoji(machine.getType()) + " " +
                    firstLetterUpper(machineName) +
                    " №" + machine.getLaundry_id());

            btn.setCallbackData(state + "::" +
                    MachineCallbacks.CHOOSE_ANOTHER_ONE + "::" +
                    machine.getType() + "::" +
                    machine.getId());
            //STATE::CHOOSE_ANOTHER_ONE::MachineType::MACHINE_ID
            if (machine.getType().equals(MachineTypes.wash)) {
                washRow.add(btn);
            } else if (machine.getType().equals(MachineTypes.dry)) {
                dryRow.add(btn);
            }
        }
        rowsInline.add(washRow);
        rowsInline.add(dryRow);

        if (addCancelButton) {
            rowsInline.add(List.of(new InlineKeyboardButton(firstLetterUpper(Localisation.getMessage("CANCEL_BUTTON", locale)),
                    null,
                    state + "::" + cancelButtonCallback + "::" + MachineTypes.unknown,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)));
            //STATE::CANCEL_CALLBACK::MachineType.unknown - for compatibility with other buttons callbacks
        }
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup machineChooseProgram(int machineId, Locales locale) {
        int buttonsInRow = 2;

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        Machine machine = DataUtils.getMachineById(machineId);
        List<Program> programs = DataUtils.getMachinePrograms(machineId);
        int btnCounter = 0;
        for (Program program : programs) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            String programLabel = Localisation.getMessage("PROGRAM_NAME", locale,program.getPublicName(),
                    program.getDuration());
            btn.setText(programLabel);
            btn.setCallbackData(States.USE_CHOOSE_PROGRAM + "::" +
                    ProgramCallbacks.CHOOSE_PROGRAM + "::" +
                    machine.getType() + "::" +
                    machine.getId() + "::" +
                    program.getInternalName());
            if (btnCounter == buttonsInRow) {
                rowsInline.add(rowInline);
                rowInline = new ArrayList<>();
                btnCounter = 0;
            }
            rowInline.add(btn);
            btnCounter++;
        }
        rowsInline.add(rowInline);


        InlineKeyboardButton customDurationBtn = new InlineKeyboardButton();
        customDurationBtn.setText(Localisation.getMessage("CUSTOM_PROGRAM_DURATION",
                locale));
        customDurationBtn.setCallbackData(States.USE_CHOOSE_PROGRAM + "::" +
                ProgramCallbacks.CHOOSE_PROGRAM + "::" +
                machine.getType() + "::" +
                machine.getId() + "::" +
                ProgramCallbacks.CUSTOM_DURATION);
        rowsInline.add(List.of(customDurationBtn));

        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup endJobNotification(UUID jobId, MachineTypes machineType, Locales locale) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton btnEnd = new InlineKeyboardButton();
        btnEnd.setText(Localisation.getMessage("USE_END_JOB_END_BUTTON", locale));
        btnEnd.setCallbackData(States.USE_END + "::" +
                JobCallbacks.JOB_END + "::" +
                machineType + "::" +
                jobId.toString());
        rowInline.add(btnEnd);

        InlineKeyboardButton btnUseAgain = new InlineKeyboardButton();
        btnUseAgain.setText(Localisation.getMessage("USE_END_JOB_START_NEW_BUTTON", locale));
        btnUseAgain.setCallbackData(States.USE_END + "::" +
                JobCallbacks.JOB_ST_NEW + "::" + //changed from JOB_START_NEW to JOB_ST_NEW to satisfy telegram callback maximum length limitation (64 bytes)
                machineType + "::" +
                jobId.toString());
        rowInline.add(btnUseAgain);

        rowsInline.add(rowInline);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }


    public static InlineKeyboardMarkup reportStartKeyboard(Locales locale) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton btnAllFree = new InlineKeyboardButton();
        btnAllFree.setText(Localisation.getMessage("REPORT_START_ALL_IDLE_BUTTON", locale));
        btnAllFree.setCallbackData(States.REPORT_START + "::" +
                ReportCallbacks.ALL_IDLE);
        firstRow.add(btnAllFree);
        rowsInline.add(firstRow);

        List<InlineKeyboardButton> washRow = new ArrayList<>();
        List<InlineKeyboardButton> dryRow = new ArrayList<>();
        ArrayList<Machine> machines = DataUtils.getAllMachines();
        for (Machine machine : machines) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            String machineName = Localisation.getMessage(machine.getType().toString().toUpperCase() + "_MACHINE_TYPE_NAME", locale);
            btn.setText(MachineTypes.getTypeEmoji(machine.getType()) + " " +
                    firstLetterUpper(machineName) +
                    " №" + machine.getLaundry_id());
            btn.setCallbackData(States.REPORT_START + "::" +
                    ReportCallbacks.SET_SPECIFIC_MACHINE + "::" +
                    machine.getId());
            if (machine.getType().equals(MachineTypes.wash)) {
                washRow.add(btn);
            } else if (machine.getType().equals(MachineTypes.dry)) {
                dryRow.add(btn);
            }
        }
        rowsInline.add(washRow);
        rowsInline.add(dryRow);


        List<InlineKeyboardButton> setAllRow = new ArrayList<>();
        InlineKeyboardButton btnSetAll = new InlineKeyboardButton();
        btnSetAll.setText(Localisation.getMessage("REPORT_START_SET_ALL_BUTTON", locale));
        btnSetAll.setCallbackData(States.REPORT_START + "::" + ReportCallbacks.SET_ALL);
        setAllRow.add(btnSetAll);
        rowsInline.add(setAllRow);

        rowsInline.add(List.of(new InlineKeyboardButton(firstLetterUpper(Localisation.getMessage("CANCEL_BUTTON", locale)),
                null,
                String.valueOf(States.REPORT_START + "::" + ReportCallbacks.CANCEL_REPORT),
                null,
                null,
                null,
                null,
                null,
                null)));

        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }


    public static InlineKeyboardMarkup reportMachineBusyKeyboard(int machineId, States state, Locales locale) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        HashSet<ReportCallbacks> machineCallbacks = ReportCallbacks.getMachineCallbacks();
        boolean rowSwitch = false;
        List<InlineKeyboardButton> tmpRow = new ArrayList<>();
        for (ReportCallbacks cb : machineCallbacks) {
            InlineKeyboardButton tmpBtn = new InlineKeyboardButton();
            tmpBtn.setText(Localisation.getMessage("REPORT_BUTTON_" + cb.name(), locale));
            tmpBtn.setCallbackData(state + "::" +
                    cb.name() + "::" +
                    machineId);
            tmpRow.add(tmpBtn);
            if (rowSwitch) {
                rowsInline.add(tmpRow);
                tmpRow = new ArrayList<>();
            }
            rowSwitch = !rowSwitch;
        }
        if (rowSwitch) rowsInline.add(tmpRow); //add last line if it has only 1 element

        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup reportOverwriteJobKeyboard(String previousCallback, Locales locale) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton agreeButton = new InlineKeyboardButton();
        agreeButton.setText(Localisation.getMessage("REPORT_BUTTON_AGREE_CHANGE_STATUS", locale));
        agreeButton.setCallbackData(previousCallback + "::" +
                ReportCallbacks.AGREE_OVERWRITE);


        InlineKeyboardButton disagreeButton = new InlineKeyboardButton();
        disagreeButton.setText(Localisation.getMessage("REPORT_BUTTON_DECLINE_CHANGE_STATUS", locale));
        disagreeButton.setCallbackData(previousCallback + "::" +
                ReportCallbacks.DISAGREE_OVERWRITE);

        //for left-to-right locale change order of buttons
        if (locale.equals(Locales.he_IL)) {
            row.add(agreeButton);
            row.add(disagreeButton);
        } else {
            row.add(disagreeButton);
            row.add(agreeButton);
        }
        rowsInline.add(row);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup settingsMainKeyboard(Locales locale) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        boolean rowSwitch = false;
        List<InlineKeyboardButton> tmpRow = new ArrayList<>();

        //TODO
        List<String> settingsList = Settings.getCallbacks();

        for (String setting : settingsList) {
            InlineKeyboardButton tmpBtn = new InlineKeyboardButton();
            tmpBtn.setText(Localisation.getMessage("SETTINGS_" + setting + "_BUTTON", locale));
            tmpBtn.setCallbackData("SETTINGS" + "::" +
                    setting + "::" +
                    "SHOW");
            tmpRow.add(tmpBtn);
            if (rowSwitch) {
                rowsInline.add(tmpRow);
                tmpRow = new ArrayList<>();
            }
            rowSwitch = !rowSwitch;
        }
        if (rowSwitch) rowsInline.add(tmpRow); //add last line if it has only 1 element
        rowsInline.add(List.of(new InlineKeyboardButton(Localisation.getMessage("CLOSE_BUTTON", locale),
                null,
                "SETTINGS" + "::" + "CLOSE",
                null,
                null,
                null,
                null,
                null,
                null)));
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup languageChooseKeyboard(Locales userLocale) {

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<Locales> locales = Localisation.getAllLocales();
        boolean rowSwitch = false;
        List<InlineKeyboardButton> tmpRow = new ArrayList<>();

        for (Locales locale : locales) {
            InlineKeyboardButton tmpBtn = new InlineKeyboardButton();
            tmpBtn.setText(Localisation.getMessage("LANGUAGE_" + locale.name().toUpperCase(), userLocale));
            tmpBtn.setCallbackData("SETTINGS" + "::" +
                    "LANGUAGE" + "::" +
                    "SET" + "::" +
                    locale);
            tmpRow.add(tmpBtn);
            if (rowSwitch) {
                rowsInline.add(tmpRow);
                tmpRow = new ArrayList<>();
            }
            rowSwitch = !rowSwitch;
        }
        if (rowSwitch) rowsInline.add(tmpRow); //add last line if it has only 1 element
        rowsInline.add(List.of(new InlineKeyboardButton(Localisation.getMessage("BACK_BUTTON", userLocale),
                null,
                "SETTINGS" + "::" + "LANGUAGE" + "::" + "BACK",
                null,
                null,
                null,
                null,
                null,
                null)));
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;

    }

    private static String firstLetterUpper(String str) {
        return str.substring(0, 1).toUpperCase() +
                str.substring(1).toLowerCase();
    }

}
