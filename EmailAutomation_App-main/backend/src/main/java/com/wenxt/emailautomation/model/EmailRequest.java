package com.wenxt.emailautomation.model;

public class EmailRequest {

    private String name;
    private String email;
    private String provider; // "Gmail" or "Outlook"
    private String message; // Short message (AI will expand)
    private String schedule; // "0" / "180" / "3600" / "86400" seconds

    public EmailRequest() {
    }

    public EmailRequest(String name, String email, String provider, String message, String schedule) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.message = message;
        this.schedule = schedule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

}
