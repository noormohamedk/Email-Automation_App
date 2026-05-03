package com.wenxt.emailautomation.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.wenxt.emailautomation.model.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "EmailAutomation";
    private static final String SHEET_RANGE = "Sheet1!A:H";

    @Value("${google.sheets.id}")
    private String spreadsheetId;

    private Sheets getSheetsService() throws Exception {
        InputStream credentialsStream = new ClassPathResource("credentials.json").getInputStream();
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsStream)
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Fetch all rows from Google Sheet and map to Person list.
     * Returns empty list if credentials.json is missing or sheets unavailable.
     */
    public List<Person> getAllPersons() {
        try {
            Sheets service = getSheetsService();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, SHEET_RANGE)
                    .execute();

            List<List<Object>> rows = response.getValues();
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }

            List<Person> persons = new ArrayList<>();
            // Skip header row (row 0)
            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                Person p = new Person(
                        getCell(row, 0), // Name
                        getCell(row, 1), // Email
                        getCell(row, 2), // Provider
                        getCell(row, 3), // Message
                        getCell(row, 4), // AI Generated
                        getCell(row, 5), // Schedule
                        getCell(row, 6), // Status
                        getCell(row, 7) // Sent Time
                );
                persons.add(p);
            }
            return persons;

        } catch (Exception e) {
            // Graceful fallback — credentials.json not yet added or sheet unavailable
            System.err.println("[GoogleSheetsService] Could not read sheet: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search persons by name (case-insensitive) or exact email.
     */
    public List<Person> searchPersons(String query) {
        List<Person> all = getAllPersons();
        if (query == null || query.isBlank())
            return all;

        String q = query.trim().toLowerCase();
        List<Person> results = new ArrayList<>();

        for (Person p : all) {
            boolean matchEmail = p.getEmail() != null && p.getEmail().toLowerCase().contains(q);
            boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(q);
            if (matchEmail || matchName) {
                results.add(p);
            }
        }
        return results;
    }

    /**
     * Add a new person row to the Google Sheet.
     */
    public void addPerson(Person person) {
        try {
            Sheets service = getSheetsService();
            List<Object> row = Arrays.asList(
                    safe(person.getName()),
                    safe(person.getEmail()),
                    safe(person.getProvider()),
                    safe(person.getMessage()),
                    "", // AI Generated — empty at add time
                    safe(person.getSchedule()),
                    "⏳ Queue", // Default status
                    "" // Sent Time — empty at add time
            );
            ValueRange body = new ValueRange().setValues(Collections.singletonList(row));
            service.spreadsheets().values()
                    .append(spreadsheetId, SHEET_RANGE, body)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (Exception e) {
            System.err.println("[GoogleSheetsService] Could not add person: " + e.getMessage());
        }
    }

    /**
     * Get stats: count of each status value.
     */
    public Map<String, Long> getStats() {
        List<Person> all = getAllPersons();
        long queue = all.stream().filter(p -> p.getStatus() != null && p.getStatus().contains("Queue")).count();
        long sent = all.stream().filter(p -> p.getStatus() != null && p.getStatus().contains("Sent")).count();
        long viewed = all.stream().filter(p -> p.getStatus() != null && p.getStatus().contains("Viewed")).count();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("queue", queue);
        stats.put("sent", sent);
        stats.put("viewed", viewed);
        stats.put("total", (long) all.size());
        return stats;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private String getCell(List<Object> row, int index) {
        if (row == null || index >= row.size())
            return "";
        Object val = row.get(index);
        return val == null ? "" : val.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

}
