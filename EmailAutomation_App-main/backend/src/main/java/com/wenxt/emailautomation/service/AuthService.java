package com.wenxt.emailautomation.service;

import com.wenxt.emailautomation.model.AuthRequest;
import com.wenxt.emailautomation.model.AuthResponse;
import com.wenxt.emailautomation.model.UserSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final class AppUser {
        private final String username;
        private final String password;
        private final String role;

        private AppUser(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }

    private final Map<String, AppUser> users = new ConcurrentHashMap<>();
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public AuthService() {
        users.put("admin", new AppUser("admin", "admin123", "ADMIN"));
        users.put("operator", new AppUser("operator", "operator123", "OPERATOR"));
    }

    public AuthResponse login(AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return null;
        }

        AppUser user = users.get(request.getUsername().trim().toLowerCase());
        if (user == null || !user.password.equals(request.getPassword())) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, user.username, user.role);
        sessions.put(token, session);
        return new AuthResponse(token, user.username, user.role);
    }

    public UserSession getSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return sessions.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public boolean hasAnyRole(String token, String... roles) {
        UserSession session = getSession(token);
        if (session == null) {
            return false;
        }
        Set<String> allowed = Set.of(roles);
        return allowed.contains(session.getRole());
    }
}
