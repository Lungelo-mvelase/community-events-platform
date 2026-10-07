package com.community.events.events;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Endpoints for events.
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> getEvents(@RequestHeader(value = "X-Role", required = false) String role,
                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return eventService.getEvents(cleanRole(role), userId);
    }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable String id,
                        @RequestHeader(value = "X-Role", required = false) String role,
                        @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return eventService.getEvent(id, cleanRole(role), userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)   // 201
    public Event createEvent(@RequestBody EventRequest request,
                            @RequestHeader(value = "X-Role", required = false) String role,
                            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return eventService.createEvent(request, cleanRole(role), userId);
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable String id, @RequestBody EventRequest request,
                            @RequestHeader(value = "X-Role", required = false) String role,
                            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return eventService.updateEvent(id, request, cleanRole(role), userId);
    }

    @PostMapping("/{id}/publish")
    public Event publishEvent(@PathVariable String id,
                            @RequestHeader(value = "X-Role", required = false) String role) {
        return eventService.publishEvent(id, cleanRole(role));
    }

    // Unknown values are also treated as visitor.
    private String cleanRole(String role) {
        if (role == null) {
            return "VISITOR";
        }
        role = role.trim().toUpperCase();
        if (role.equals("ADMIN") || role.equals("ORGANISER")) {
            return role;
        }
        return "VISITOR";
    }
}
