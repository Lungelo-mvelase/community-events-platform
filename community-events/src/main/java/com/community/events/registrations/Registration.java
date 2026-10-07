package com.community.events.registrations;

import java.time.LocalDateTime;

// An anonymous registration
public class Registration {

    private String id;
    private String eventId;
    private LocalDateTime registeredAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
