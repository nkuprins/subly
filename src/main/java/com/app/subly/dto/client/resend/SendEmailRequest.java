package com.app.subly.dto.client.resend;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendEmailRequest {
    private String from;
    private String to;
    private String subject;
    private String html;
}
