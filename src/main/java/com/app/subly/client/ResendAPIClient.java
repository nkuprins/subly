package com.app.subly.client;

import com.app.subly.config.ResendAPIClientConfiguration;
import com.app.subly.dto.client.resend.SendEmailRequest;
import com.app.subly.exception.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

// A simple client to call Resend API for sending emails.
@Component
@Slf4j
public class ResendAPIClient implements EmailClient {
    private final RestClient client;
    private final ResendAPIClientConfiguration configuration;

    public ResendAPIClient(RestClient.Builder builder, ResendAPIClientConfiguration configuration) {
        this.client = builder.defaultHeader("Authorization", "Bearer " + configuration.getApiKey())
                .build();
        this.configuration = configuration;
    }

    @Override
    public void sendEmail(String from, String to, String subject, String template) {
        log.debug("Sending email from={}, to={}", from, to);
        try {
            this.client
                    .post()
                    .uri(this.configuration.getEmailUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createSendEmailRequest(from, to, subject, template))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Successfully sent email from={}, to={}", from, to);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("Client 401 Error: {}", ex.getMessage());
            throw new InvalidApiKeyException("Authorization failed. Please provide the right API key.", ex);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Client 403 Error: {}", ex.getMessage());
            throw new ApiKeyAccessException("Permissions error: " + ex.getMessage(), ex);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.error("Client 429 Error: {}", ex.getMessage());
            throw new RateLimitExceededException(
                    "You send requests too often or monthly/daily limit has been exceeded", ex);
        } catch (HttpClientErrorException ex) {
            log.error("Client Error: {}", ex.getMessage());
            throw new EmailClientException("Some client occurred during sending email: " + ex.getMessage(), ex);
        } catch (HttpServerErrorException ex) {
            log.error("Server Error: {}", ex.getMessage());
            throw new APIServerException("Some server error occurred: " + ex.getMessage(), ex);
        }
    }

    private SendEmailRequest createSendEmailRequest(String from, String to, String subject, String template) {
        SendEmailRequest request = new SendEmailRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setSubject(subject);
        request.setHtml(template);
        return request;
    }
}
