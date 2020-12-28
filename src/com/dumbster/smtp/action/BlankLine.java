package com.dumbster.smtp.action;

import com.dumbster.smtp.MailMessage;
import com.dumbster.smtp.MailStore;
import com.dumbster.smtp.Response;
import com.dumbster.smtp.SmtpState;

public class BlankLine implements Action {

    @Override
    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        if (null == smtpState) {
            return new Response(503, "Bad sequence of commands: " + this, smtpState);
        } else {
            switch (smtpState) {
                case DATA_HDR:
                    return new Response(-1, "", SmtpState.DATA_BODY);
                case DATA_BODY:
                    return new Response(-1, "", smtpState);
                default:
                    return new Response(503, "Bad sequence of commands: " + this, smtpState);
            }
        }
    }

    @Override
    public String toString() {
        return "Blank line";
    }
}
