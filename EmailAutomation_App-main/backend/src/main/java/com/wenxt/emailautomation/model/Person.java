package com.wenxt.emailautomation.model;

public class Person {

    private String name;
    private String email;
    private String provider;
    private String message;
    private String aiGenerated;
    private String schedule;
    private String status;
    private String sentTime;

    public Person() {
    }

    public Person(String name, String email, String provider, String message, String aiGenerated, String schedule,
            String status, String sentTime) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.message = message;
        this.aiGenerated = aiGenerated;
        this.schedule = schedule;
        this.status = status;
        this.sentTime = sentTime;
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

    public String getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(String aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSentTime() {
        return sentTime;
    }

    public void setSentTime(String sentTime) {
        this.sentTime = sentTime;
    }

}
