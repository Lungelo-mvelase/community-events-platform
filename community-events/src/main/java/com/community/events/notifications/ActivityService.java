package com.community.events.notifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.community.events.persistence.JsonStore;

@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final JsonStore store;

    public ActivityService(JsonStore store) {
        this.store = store;
    }

    // Adds an entry to the list
    public void addActivity(String message) {
        Activity activity = new Activity();
        activity.setTime(LocalDateTime.now());
        activity.setMessage(message);
        store.getData().getActivities().add(activity);
        log.info("op=recordActivity message=\"{}\"", message);
    }

    // The newest 20 entries, newest first
    public List<Activity> getLatestActivities() {
        List<Activity> all = store.getData().getActivities();
        List<Activity> result = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && result.size() < 20; i--) {
            result.add(all.get(i));
        }
        return result;
    }
}
