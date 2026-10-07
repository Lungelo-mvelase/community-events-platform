package com.community.events.notifications;

import java.time.LocalDateTime;

// One line in the activity list
public class Activity {

    private LocalDateTime time;
    private String message;

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
