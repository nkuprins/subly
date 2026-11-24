package com.app.subly.client;

public interface EmailClient {
     void sendEmail(String from, String to, String subject, String template);
}
