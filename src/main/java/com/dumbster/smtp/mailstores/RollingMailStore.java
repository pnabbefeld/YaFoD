package com.dumbster.smtp.mailstores;

import com.dumbster.smtp.MailMessage;
import com.dumbster.smtp.MailStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RollingMailStore implements MailStore {

    private final List<MailMessage> receivedMail;

    public RollingMailStore() {
        receivedMail = Collections.synchronizedList(new ArrayList<MailMessage>());
    }

    @Override
    public int getEmailCount() {
        return receivedMail.size();
    }

    @Override
    public void addMessage(MailMessage message) {
        System.out.println("\n\nReceived message:\n" + message);
        receivedMail.add(message);
        if (getEmailCount() > 100) {
            receivedMail.remove(0);
        }
    }

    @Override
    public MailMessage[] getMessages() {
        return receivedMail.toArray(new MailMessage[receivedMail.size()]);
    }

    @Override
    public MailMessage getMessage(int index) {
        return receivedMail.get(index);
    }

    @Override
    public void clearMessages() {
        this.receivedMail.clear();
    }
}
