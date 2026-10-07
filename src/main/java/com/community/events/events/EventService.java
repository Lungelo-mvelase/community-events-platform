package com.community.events.events;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.community.events.api.ApiException;
import com.community.events.notifications.ActivityService;
import com.community.events.persistence.JsonStore;

// All the business rules for events live here.
@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final JsonStore store;
    private final ActivityService activityService;

    public EventService(JsonStore store, ActivityService activityService) {
        this.store = store;
        this.activityService = activityService;
    }

    // Which events can this role see in the list?
    public List<Event> getEvents(String role, String userId) {
        List<Event> result = new ArrayList<>();

        for (Event event : store.getData().getEvents()) {
            if (role.equals("ADMIN")) {
                result.add(event);                                  // admin sees everything
            } else if (role.equals("ORGANISER")) {
                if (event.getOrganiserId().equals(userId)) {        // organiser sees their own events
                    result.add(event);
                }
            } else {
                if (event.getStatus() == EventStatus.PUBLISHED) {   // visitor sees published only
                    result.add(event);
                }
            }
        }
        return result;
    }

    // Get one event (404 if it does not exist/ this role may not see it)
    public Event getEvent(String id, String role, String userId) {
        Event event = findEvent(id);

        boolean allowed = false;
        if (role.equals("ADMIN")) {
            allowed = true;
        } else if (event.getStatus() == EventStatus.PUBLISHED) {
            allowed = true;
        } else if (role.equals("ORGANISER") && event.getOrganiserId().equals(userId)) {
            allowed = true;
        }

        if (!allowed) {
            throw eventNotFound();
        }
        return event;
    }

    public Event createEvent(EventRequest request, String role, String userId) {
        if (!role.equals("ORGANISER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only organisers can create events.");
        }
        if (userId == null || userId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_USER_ID", "Organiser id is required.");
        }
        validate(request);

        Event event = new Event();
        event.setId("EVT-" + (store.getData().getEvents().size() + 1001));
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setDateTime(request.getDateTime());
        event.setCapacity(request.getCapacity());
        event.setStatus(EventStatus.PENDING_REVIEW);   // new events always start as pending
        event.setOrganiserId(userId);

        store.getData().getEvents().add(event);
        store.save();

        log.info("op=createEvent eventId={} outcome=SUCCESS", event.getId());
        return event;
    }

    public Event updateEvent(String id, EventRequest request, String role, String userId) {
        if (!role.equals("ORGANISER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only organisers can update events.");
        }
        Event event = findEvent(id);
        if (!event.getOrganiserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only update your own events.");
        }
        validate(request);
        if (request.getCapacity() < event.getRegisteredCount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Capacity cannot be lower than the number of registrations already made.");
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setDateTime(request.getDateTime());
        event.setCapacity(request.getCapacity());
        store.save();

        log.info("op=updateEvent eventId={} outcome=SUCCESS", id);
        return event;
    }

    public Event publishEvent(String id, String role) {
        if (!role.equals("ADMIN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only administrators can publish events.");
        }
        Event event = findEvent(id);
        if (event.getStatus() != EventStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only events pending review can be published.");
        }

        event.setStatus(EventStatus.PUBLISHED);
        activityService.addActivity("Event " + id + " was published.");
        store.save();

        log.info("op=publishEvent eventId={} outcome=SUCCESS", id);
        return event;
    }

    // Looks for an event by id. Throws 404 if there is none.
    public Event findEvent(String id) {
        for (Event event : store.getData().getEvents()) {
            if (event.getId().equals(id)) {
                return event;
            }
        }
        throw eventNotFound();
    }

    // The validation rules from the assignment
    private void validate(EventRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw badRequest("Event title is required.");
        }
        if (request.getTitle().length() > 200) {
            throw badRequest("Event title must be at most 200 characters.");
        }
        if (request.getDescription() != null && request.getDescription().length() > 2000) {
            throw badRequest("Event description must be at most 2000 characters.");
        }
        if (request.getDateTime() == null) {
            throw badRequest("Event date/time is required.");
        }
        if (request.getDateTime().isBefore(LocalDateTime.now())) {
            throw badRequest("Event date/time must not be in the past.");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw badRequest("Capacity must be a whole number greater than zero.");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private ApiException eventNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Event not found.");
    }
}
