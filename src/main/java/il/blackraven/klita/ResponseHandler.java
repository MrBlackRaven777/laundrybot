package il.blackraven.klita;

import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class ResponseHandler {

    private final MessageSender sender;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;
    }

    public void replyToUse(long chatId, String buttonId) {
        String washNum = buttonId.substring(buttonId.lastIndexOf("_"));
        switch (buttonId) {
            case "wash_1":
                replyToWash(chatId, washNum);
                break;
            case "wash_2":
                replyToWash(chatId, washNum);
                break;
        }
    }

    public void replyToWash(long chatId, String washNum) {
        SendMessage reply = new SendMessage();
        reply.setChatId(chatId);
        reply.setText("You have chosen washing machine number " + washNum);
        try {
            sender.execute(reply);
        } catch (TelegramApiException tae) {
            tae.printStackTrace();
        }
    }
}
