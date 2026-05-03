package com.wenxt.emailautomation.service;

import com.wenxt.emailautomation.model.EmailRequest;
import com.wenxt.emailautomation.model.Person;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PersonDirectoryService {

    private final GoogleSheetsService sheetsService;
    private final Map<String, Person> personMap = new ConcurrentHashMap<>();

    public PersonDirectoryService(GoogleSheetsService sheetsService) {
        this.sheetsService = sheetsService;
        warmUpFromSheet();
    }

    public static Person fromRequest(EmailRequest request, String status) {
        Person person = new Person();
        person.setName(request.getName());
        person.setEmail(normalizeEmail(request.getEmail()));
        person.setProvider(request.getProvider());
        person.setMessage(request.getMessage());
        person.setSchedule(request.getSchedule());
        person.setStatus(status);
        person.setSentTime("");
        person.setAiGenerated("false");
        return person;
    }

    public List<Person> getAllPersons() {
        List<Person> persons = new ArrayList<>(personMap.values());
        persons.sort((a, b) -> String.valueOf(a.getEmail()).compareToIgnoreCase(String.valueOf(b.getEmail())));
        return persons;
    }

    public List<Person> searchPersons(String query) {
        if (query == null || query.isBlank()) {
            return getAllPersons();
        }

        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Person> results = new ArrayList<>();
        for (Person p : personMap.values()) {
            String name = safe(p.getName()).toLowerCase(Locale.ROOT);
            String email = safe(p.getEmail()).toLowerCase(Locale.ROOT);
            if (name.contains(q) || email.contains(q)) {
                results.add(p);
            }
        }
        results.sort((a, b) -> String.valueOf(a.getEmail()).compareToIgnoreCase(String.valueOf(b.getEmail())));
        return results;
    }

    public Person createPerson(Person person) {
        Person normalized = normalize(person);
        personMap.put(normalized.getEmail(), normalized);
        sheetsService.addPerson(normalized);
        return normalized;
    }

    public Person updatePerson(String originalEmail, Person person) {
        String oldKey = normalizeEmail(originalEmail);
        if (!personMap.containsKey(oldKey)) {
            return null;
        }

        Person merged = normalize(person);
        if (merged.getEmail().isBlank()) {
            merged.setEmail(oldKey);
        }

        personMap.remove(oldKey);
        personMap.put(merged.getEmail(), merged);
        return merged;
    }

    public boolean deletePerson(String email) {
        String key = normalizeEmail(email);
        return personMap.remove(key) != null;
    }

    public void recordSend(EmailRequest request, String sentTime, String status) {
        String key = normalizeEmail(request.getEmail());
        Person existing = personMap.get(key);
        if (existing == null) {
            existing = fromRequest(request, status);
        }

        existing.setName(safe(request.getName()));
        existing.setEmail(key);
        existing.setProvider(safe(request.getProvider()));
        existing.setMessage(safe(request.getMessage()));
        existing.setSchedule(defaultIfBlank(request.getSchedule(), "0"));
        existing.setStatus(status);
        existing.setSentTime(sentTime);
        personMap.put(key, existing);
        sheetsService.addPerson(existing);
    }

    public Map<String, Long> getStats() {
        long queue = personMap.values().stream()
                .filter(p -> safe(p.getStatus()).toLowerCase(Locale.ROOT).contains("queue")).count();
        long sent = personMap.values().stream()
                .filter(p -> safe(p.getStatus()).toLowerCase(Locale.ROOT).contains("sent")).count();
        long viewed = personMap.values().stream()
                .filter(p -> safe(p.getStatus()).toLowerCase(Locale.ROOT).contains("viewed")).count();
        long failed = personMap.values().stream()
                .filter(p -> safe(p.getStatus()).toLowerCase(Locale.ROOT).contains("failed")).count();

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("queue", queue);
        stats.put("sent", sent);
        stats.put("viewed", viewed);
        stats.put("failed", failed);
        stats.put("total", (long) personMap.size());
        return stats;
    }

    public String exportCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("name,email,provider,message,aiGenerated,schedule,status,sentTime\n");
        for (Person p : getAllPersons()) {
            csv.append(esc(p.getName())).append(',')
                    .append(esc(p.getEmail())).append(',')
                    .append(esc(p.getProvider())).append(',')
                    .append(esc(p.getMessage())).append(',')
                    .append(esc(p.getAiGenerated())).append(',')
                    .append(esc(p.getSchedule())).append(',')
                    .append(esc(p.getStatus())).append(',')
                    .append(esc(p.getSentTime())).append('\n');
        }
        return csv.toString();
    }

    public Map<String, Integer> importCsv(String csvBody) {
        int imported = 0;
        int skipped = 0;

        if (csvBody == null || csvBody.isBlank()) {
            Map<String, Integer> empty = new LinkedHashMap<>();
            empty.put("imported", 0);
            empty.put("skipped", 0);
            return empty;
        }

        String[] lines = csvBody.replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (i == 0 && line.toLowerCase(Locale.ROOT).contains("email")) {
                continue;
            }

            String[] cols = line.split(",", -1);
            if (cols.length < 2) {
                skipped++;
                continue;
            }

            Person p = new Person();
            p.setName(unquote(getCol(cols, 0)));
            p.setEmail(normalizeEmail(unquote(getCol(cols, 1))));
            p.setProvider(unquote(getCol(cols, 2)));
            p.setMessage(unquote(getCol(cols, 3)));
            p.setAiGenerated(unquote(getCol(cols, 4)));
            p.setSchedule(defaultIfBlank(unquote(getCol(cols, 5)), "0"));
            p.setStatus(defaultIfBlank(unquote(getCol(cols, 6)), "Queue"));
            p.setSentTime(unquote(getCol(cols, 7)));

            if (p.getEmail().isBlank()) {
                skipped++;
                continue;
            }

            personMap.put(p.getEmail(), p);
            imported++;
        }

        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("imported", imported);
        summary.put("skipped", skipped);
        return summary;
    }

    private void warmUpFromSheet() {
        List<Person> sheetPeople = sheetsService.getAllPersons();
        for (Person p : sheetPeople) {
            if (p.getEmail() == null || p.getEmail().isBlank()) {
                continue;
            }
            p.setEmail(normalizeEmail(p.getEmail()));
            p.setSchedule(defaultIfBlank(p.getSchedule(), "0"));
            p.setStatus(defaultIfBlank(p.getStatus(), "Queue"));
            personMap.put(p.getEmail(), p);
        }
    }

    private Person normalize(Person person) {
        Person p = new Person();
        p.setName(safe(person.getName()));
        p.setEmail(normalizeEmail(person.getEmail()));
        p.setProvider(defaultIfBlank(person.getProvider(), "Gmail"));
        p.setMessage(safe(person.getMessage()));
        p.setAiGenerated(defaultIfBlank(person.getAiGenerated(), "false"));
        p.setSchedule(defaultIfBlank(person.getSchedule(), "0"));
        p.setStatus(defaultIfBlank(person.getStatus(), "Queue"));
        p.setSentTime(safe(person.getSentTime()));
        return p;
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String getCol(String[] cols, int index) {
        if (index >= cols.length) {
            return "";
        }
        return cols[index];
    }

    private String unquote(String value) {
        String v = safe(value);
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace("\"\"", "\"");
    }

    private String esc(String value) {
        String v = safe(value).replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
