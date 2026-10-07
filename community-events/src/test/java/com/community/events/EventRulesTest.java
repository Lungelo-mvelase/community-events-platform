package com.community.events;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// These tests start the whole app and call the real endpoints.
// Each test run uses a fresh temporary JSON file, so your real data/store.json is never touched.
@SpringBootTest
@AutoConfigureMockMvc
class EventRulesTest {

    @DynamicPropertySource
    static void useTempFile(DynamicPropertyRegistry registry) throws IOException {
        Path folder = Files.createTempDirectory("events-test");
        registry.add("app.data-file", () -> folder.resolve("store.json").toString());
        registry.add("app.seed-enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    // ---------- helper methods ----------

    // A date 10 days in the future, like 2030-01-31T18:00
    private String futureDate() {
        return LocalDateTime.now().plusDays(10).withSecond(0).withNano(0).toString();
    }

    private String json(String title, String dateTime, String capacity) {
        return "{\"title\":\"" + title + "\",\"description\":\"test\",\"dateTime\":\"" + dateTime
                + "\",\"capacity\":" + capacity + "}";
    }

    // Creates an event as organiser-1 and returns its id
    private String createEvent(String capacity) throws Exception {
        String response = mvc.perform(post("/api/events")
                        .header("X-Role", "ORGANISER")
                        .header("X-User-Id", "organiser-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Test event", futureDate(), capacity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void publishEvent(String id) throws Exception {
        mvc.perform(post("/api/events/" + id + "/publish").header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    // ---------- tests ----------

    @Test
    void zeroCapacityIsRejected() throws Exception {
        mvc.perform(post("/api/events")
                        .header("X-Role", "ORGANISER")
                        .header("X-User-Id", "organiser-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Bad event", futureDate(), "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void dateInThePastIsRejected() throws Exception {
        mvc.perform(post("/api/events")
                        .header("X-Role", "ORGANISER")
                        .header("X-User-Id", "organiser-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Old event", "2000-01-01T10:00", "5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visitorCannotSeeUnpublishedEvent() throws Exception {
        String id = createEvent("5");   // new events are PENDING_REVIEW

        mvc.perform(get("/api/events/" + id).header("X-Role", "VISITOR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishingMakesEventVisibleToVisitor() throws Exception {
        String id = createEvent("5");
        publishEvent(id);

        mvc.perform(get("/api/events/" + id).header("X-Role", "VISITOR"))
                .andExpect(status().isOk());
    }

    @Test
    void registrationIsRejectedForUnpublishedEvent() throws Exception {
        String id = createEvent("5");

        mvc.perform(post("/api/events/" + id + "/registrations"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_PUBLISHED"));
    }

    @Test
    void registrationIsRejectedWhenEventIsFull() throws Exception {
        String id = createEvent("1");   // only one place
        publishEvent(id);

        // first registration works
        mvc.perform(post("/api/events/" + id + "/registrations"))
                .andExpect(status().isCreated());

        // second one is rejected
        mvc.perform(post("/api/events/" + id + "/registrations"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_FULL"));
    }
}
