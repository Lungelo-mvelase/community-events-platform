package com.community.events.registrations;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.community.events.api.ApiException;
import com.community.events.events.Event;
import com.community.events.events.EventService;
import com.community.events.events.EventStatus;
import com.community.events.notifications.ActivityService;
import com.community.events.persistence.JsonStore;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final JsonStore store;
    private final EventService eventService;
    private final ActivityService activityService;

    public RegistrationService(JsonStore store, EventService eventService, ActivityService activityService) {
        this.store = store;
        this.eventService = eventService;
        this.activityService = activityService;
    }

    public synchronized Registration register(String eventId) {
        Event event = eventService.findEvent(eventId);          // 404 if not found
        log.info("op=registerInterest eventId={} step=eventFound", eventId);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            log.warn("op=registerInterest eventId={} outcome=REJECTED reason=NOT_PUBLISHED", eventId);
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_PUBLISHED",
                    "Registration is not available because this event has not been published.");
        }

        if (event.getRegisteredCount() >= event.getCapacity()) {
            log.warn("op=registerInterest eventId={} outcome=REJECTED reason=EVENT_FULL", eventId);
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_FULL",
                    "Registration is no longer available because this event is full.");
        }

        // Create the registration
        Registration registration = new Registration();
        registration.setId(UUID.randomUUID().toString());
        registration.setEventId(eventId);
        registration.setRegisteredAt(LocalDateTime.now());

        store.getData().getRegistrations().add(registration);
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        activityService.addActivity("Registration " + registration.getId() + " was created for event " + eventId + ".");
        store.save();
        log.info("op=registerInterest eventId={} registrationId={} outcome=SUCCESS", eventId, registration.getId());

        return registration;
    }
}
