package com.community.events.persistence;

import java.util.ArrayList;
import java.util.List;

import com.community.events.events.Event;
import com.community.events.notifications.Activity;
import com.community.events.registrations.Registration;

public class StoreData {

    private List<Event> events = new ArrayList<>();
    private List<Registration> registrations = new ArrayList<>();
    private List<Activity> activities = new ArrayList<>();

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public List<Registration> getRegistrations() { return registrations; }
    public void setRegistrations(List<Registration> registrations) { this.registrations = registrations; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }
}
