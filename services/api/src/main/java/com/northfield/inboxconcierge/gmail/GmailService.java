package com.northfield.inboxconcierge.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Thread;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GmailService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GmailService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    private Gmail getGmailClient(OAuth2AuthenticationToken authentication) throws GeneralSecurityException, IOException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("No authorized client found or access token missing");
        }
        
        String accessToken = client.getAccessToken().getTokenValue();

        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
        }).setApplicationName("Inbox Concierge").build();
    }

    public List<Thread> fetchRecentThreads(OAuth2AuthenticationToken authentication, int limit) {
        try {
            Gmail gmail = getGmailClient(authentication);
            ListThreadsResponse response = gmail.users().threads().list("me")
                    .setMaxResults((long) limit)
                    .execute();
            
            List<Thread> threads = response.getThreads();
            List<Thread> fullThreads = new ArrayList<>();
            if (threads != null) {
                for (Thread t : threads) {
                    try {
                        fullThreads.add(gmail.users().threads().get("me", t.getId()).setFormat("metadata").execute());
                    } catch (Exception skipped) {
                        // ignore broken threads
                    }
                }
            }
            return fullThreads;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Gmail threads", e);
        }
    }
}
