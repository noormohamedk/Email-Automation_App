package com.wenxt.emailautomation.controller;

import com.wenxt.emailautomation.model.ApiResponse;
import com.wenxt.emailautomation.model.AuthRequest;
import com.wenxt.emailautomation.model.AuthResponse;
import com.wenxt.emailautomation.model.EmailRequest;
import com.wenxt.emailautomation.model.Person;
import com.wenxt.emailautomation.model.UserSession;
import com.wenxt.emailautomation.service.AuthService;
import com.wenxt.emailautomation.service.EmailService;
import com.wenxt.emailautomation.service.PersonDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmailController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";

    private final EmailService emailService;
    private final PersonDirectoryService directoryService;
    private final AuthService authService;

    public EmailController(EmailService emailService, PersonDirectoryService directoryService,
            AuthService authService) {
        this.emailService = emailService;
        this.directoryService = directoryService;
        this.authService = authService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("Backend is healthy", "OK");
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        if (response == null) {
            return ApiResponse.error("Invalid username or password");
        }
        return ApiResponse.ok("Login successful", response);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<String> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        UserSession session = requireSession(token);
        if (session == null) {
            return ApiResponse.error("Unauthorized");
        }
        authService.logout(token);
        return ApiResponse.ok("Logged out", "OK");
    }

    @GetMapping("/auth/me")
    public ApiResponse<UserSession> me(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        UserSession session = requireSession(token);
        if (session == null) {
            return ApiResponse.error("Unauthorized");
        }
        return ApiResponse.ok("Session active", session);
    }

    @GetMapping("/persons")
    public ApiResponse<List<Person>> persons(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        return ApiResponse.ok("Fetched persons", directoryService.getAllPersons());
    }

    @GetMapping("/search")
    public ApiResponse<List<Person>> search(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestParam(value = "query", defaultValue = "") String query) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        List<Person> results = directoryService.searchPersons(query);
        if (results.isEmpty()) {
            return ApiResponse.error("No person found for: " + query);
        }
        return ApiResponse.ok("Found " + results.size() + " result(s)", results);
    }

    @PostMapping("/persons")
    public ApiResponse<Person> createPerson(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody Person person) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        if (person.getEmail() == null || person.getEmail().isBlank()) {
            return ApiResponse.error("Email is required");
        }
        Person saved = directoryService.createPerson(person);
        return ApiResponse.ok("Person saved", saved);
    }

    @PutMapping("/persons/{email}")
    public ApiResponse<Person> updatePerson(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable("email") String email,
            @RequestBody Person person) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }

        Person updated = directoryService.updatePerson(email, person);
        if (updated == null) {
            return ApiResponse.error("Person not found");
        }
        return ApiResponse.ok("Person updated", updated);
    }

    @DeleteMapping("/persons/{email}")
    public ApiResponse<String> deletePerson(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable("email") String email) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN)) {
            return ApiResponse.error("Forbidden: admin role required");
        }

        boolean deleted = directoryService.deletePerson(email);
        if (!deleted) {
            return ApiResponse.error("Person not found");
        }
        return ApiResponse.ok("Person deleted", email);
    }

    @PostMapping("/send")
    public ApiResponse<String> send(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody EmailRequest request) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ApiResponse.error("Email is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ApiResponse.error("Message is required");
        }

        String result = emailService.sendEmail(request);
        boolean failed = isFailedSendResult(result);
        directoryService.recordSend(request, Instant.now().toString(), failed ? "Failed" : "Sent");
        if (failed) {
            return ApiResponse.error(result);
        }
        return ApiResponse.ok("Email request sent", result);
    }

    @PostMapping("/add-and-send")
    public ApiResponse<String> addAndSend(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody EmailRequest request) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ApiResponse.error("Email is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ApiResponse.error("Message is required");
        }

        directoryService.createPerson(PersonDirectoryService.fromRequest(request, "Queue"));
        String result = emailService.addAndSend(request);
        boolean failed = isFailedSendResult(result);
        directoryService.recordSend(request, Instant.now().toString(), failed ? "Failed" : "Sent");
        if (failed) {
            return ApiResponse.error(result);
        }
        return ApiResponse.ok("Person added and email sent", result);
    }

    @GetMapping("/logs")
    public ApiResponse<List<Person>> logs(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        return ApiResponse.ok("Fetched " + directoryService.getAllPersons().size() + " record(s)",
                directoryService.getAllPersons());
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN, ROLE_OPERATOR)) {
            return ApiResponse.error("Unauthorized");
        }
        return ApiResponse.ok("Stats fetched", directoryService.getStats());
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public String exportCsv(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN)) {
            return "error\nUnauthorized\n";
        }
        return directoryService.exportCsv();
    }

    @PostMapping(value = "/import/csv", consumes = { MediaType.TEXT_PLAIN_VALUE, "text/csv" })
    public ApiResponse<Map<String, Integer>> importCsv(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody String csvBody) {
        if (!authService.hasAnyRole(token, ROLE_ADMIN)) {
            return ApiResponse.error("Forbidden: admin role required");
        }
        return ApiResponse.ok("CSV import complete", directoryService.importCsv(csvBody));
    }

    private UserSession requireSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return authService.getSession(token);
    }

    private boolean isFailedSendResult(String result) {
        if (result == null) {
            return true;
        }
        return result.startsWith("ERROR");
    }
}
