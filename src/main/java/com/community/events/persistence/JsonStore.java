package com.community.events.persistence;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.community.events.events.Event;
import com.community.events.events.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

// The Database
@Component
public class JsonStore {

    private final ObjectMapper objectMapper;
    private final File file;
    private StoreData data;

    public JsonStore(ObjectMapper objectMapper,
                    @Value("${app.data-file}") String dataFile,
                    @Value("${app.seed-enabled}") boolean seedEnabled) throws IOException {
        this.objectMapper = objectMapper;
        this.file = new File(dataFile);

        if (file.exists() && file.length() > 0) {
            data = objectMapper.readValue(file, StoreData.class);
        } else {
            data = new StoreData();
            if (seedEnabled) {
                addSampleEvents();
            }
            save();
        }
    }

    public StoreData getData() {
        return data;
    }

    public void save() {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("Could not save data file", e);
        }
    }

    // Sample data
    private void addSampleEvents() {
        LocalDateTime start = LocalDateTime.now().plusDays(14).withMinute(0).withSecond(0).withNano(0);
        data.getEvents().add(makeEvent("EVT-1001", "Community Garden Day", "Help plant vegetables.",
                start, 20, EventStatus.PUBLISHED, "organiser-1"));
        data.getEvents().add(makeEvent("EVT-1002", "Neighbourhood Book Swap", "Bring a book, take a book.",
                start.plusDays(2), 15, EventStatus.PENDING_REVIEW, "organiser-1"));
        data.getEvents().add(makeEvent("EVT-1003", "Tiny Workshop", "Only 1 place - good for testing the full rule.",
                start.plusDays(3), 1, EventStatus.PUBLISHED, "organiser-2"));
        data.getEvents().add(makeEvent("EVT-1004", "Open Mic Night", "An evening of music and poetry.",
                start.plusDays(5), 50, EventStatus.PENDING_REVIEW, "organiser-2"));
    }

    private Event makeEvent(String id, String title, String description, LocalDateTime dateTime,
                            int capacity, EventStatus status, String organiserId) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setDescription(description);
        event.setDateTime(dateTime);
        event.setCapacity(capacity);
        event.setStatus(status);
        event.setOrganiserId(organiserId);
        return event;
    }
}
