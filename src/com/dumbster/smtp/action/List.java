package com.dumbster.smtp.action;

import com.dumbster.smtp.MailMessage;
import com.dumbster.smtp.MailStore;
import com.dumbster.smtp.Response;
import com.dumbster.smtp.SmtpState;

public class List implements Action {

    private Integer messageIndex = null;

    public List(String params) {
        try {
            Integer tempMI = Integer.valueOf(params);
            if (tempMI > -1) {
                this.messageIndex = tempMI;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        StringBuilder result = new StringBuilder();
        if (messageIndex != null && messageIndex < mailStore.getEmailCount()) {
            result.append("\n-------------------------------------------\n");
            result.append(mailStore.getMessage(messageIndex).toString());
        }
        result.append("There are ");
        result.append(mailStore.getEmailCount());
        result.append(" message(s).");
        return new Response(250, result.toString(), SmtpState.GREET);
    }

    @Override
    public String toString() {
        return "LIST";
    }
}
