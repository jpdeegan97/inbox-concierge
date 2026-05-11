package com.northfield.inboxconcierge.auth;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/session")
    public Map<String, Object> getSession(OAuth2AuthenticationToken token) {
        if (token == null) {
            return Map.of("authenticated", false);
        }
        return Map.of(
            "authenticated", true,
            "user", Map.of(
                "email", token.getPrincipal().getAttribute("email"),
                "displayName", token.getPrincipal().getAttribute("name")
            )
        );
    }
}
