package com.wenxt.emailautomation.service;

import com.wenxt.emailautomation.model.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class EmailService {

    private final WebClient webClient;

    @Value("${n8n.webhook.url}")
    private String webhookUrl;

    public EmailService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Send an email request to the n8n webhook.
     * Returns the raw response string from n8n.
     */
    public String sendEmail(EmailRequest request) {
        try {
            String response = webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("ERROR: " + e.getMessage()))
                    .block();

            if (response == null || response.isBlank()) {
                return "ERROR: Empty response from n8n. Check webhook workflow and email node configuration.";
            }
            return response;
        } catch (Exception e) {
            return "ERROR: Failed to call n8n webhook: " + e.getMessage();
        }
    }

    /**
     * Add a new person to the sheet via n8n webhook, then send email.
     * Uses the same webhook — n8n auto-adds if person not found.
     */
    public String addAndSend(EmailRequest request) {
        return sendEmail(request);
    }

}
