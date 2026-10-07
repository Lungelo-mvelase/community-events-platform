# Community Events Platform

A simple Spring Boot (Java 25) app with a plain HTML page as frontend.
Data is saved in one JSON file. No database, no login, no secrets, no environment variables.

## How to run

```bash
mvn spring-boot:run     # then open http://localhost:8080
mvn test                # run the tests
```
Needs JDK 25 and Maven 3.9+.

## How it works (short explanation)

```
Browser (index.html)
   |  fetch("/api/...") with headers X-Role and X-User-Id
   v
Controller   -> receives the HTTP request, passes it on        (EventController, RegistrationController, ActivityController)
Service      -> the business rules (validation, who may do what) (EventService, RegistrationService, ActivityService)
JsonStore    -> keeps the data in memory and saves it to data/store.json
```

Packages:

| Package | What is inside |
|---|---|
| `events` | `Event`, `EventStatus`, `EventService` (rules), `EventController` (endpoints) |
| `registrations` | `Registration` (only id, eventId, time), service, controller |
| `notifications` | `Activity` entries like "Event EVT-1002 was published." |
| `persistence` | `JsonStore` (read/write the JSON file) |
| `api` | error handling and the correlation ID filter for logs |

## Data stored

`./data/store.json` holds events, registrations and activity entries. It is created on first start with fictional
sample data and **is kept after a restart**. Delete the file to start fresh.
A registration contains only: id, event id, time. No personal data is stored or logged.

## Demo roles (a simplification)

The role selector on the page sends `X-Role` (VISITOR / ORGANISER / ADMIN) and `X-User-Id` (organiser-1 / organiser-2).
The backend checks these (only organisers create/edit their own events, only admins publish), but anyone could send
fake headers, so this is **not real security**, only a demo.

Sample events: EVT-1001 published, EVT-1002 pending (organiser-1), EVT-1003 published with capacity 1, EVT-1004 pending (organiser-2).

## API

| Request | Who | Result |
|---|---|---|
| `GET /api/events` | everyone (list depends on role) | 200 |
| `GET /api/events/{id}` | everyone (visitors only see published) | 200 / 404 |
| `POST /api/events` | organiser | 201 / 400 |
| `PUT /api/events/{id}` | organiser who owns it | 200 / 400 / 403 / 404 |
| `POST /api/events/{id}/publish` | admin | 200 / 403 / 404 / 409 |
| `POST /api/events/{id}/registrations` | anyone, anonymous | 201 / 404 / 409 |
| `GET /api/activities` | everyone | 200 |

Errors look like: `{"code":"EVENT_FULL","message":"Registration is no longer available because this event is full."}`

Validation: title required; date required and not in the past; capacity a whole number > 0;
registration only for published events that are not full.

## How to demonstrate

**Visitor:** choose *Visitor* -> only published events show -> click *Register interest* -> a registration ID appears and the activity list gets a new line.

**Organiser:** choose *Event Organiser* -> fill in the form and click *Create* -> choose *Visitor*: the event is not there -> choose organiser again -> click *Edit* and save.

**Administrator:** choose *Administrator* -> see pending events -> click *Publish* on EVT-1002 -> choose *Visitor*: it is now visible.

**Registration rules:** in the *Registration tester* type `EVT-1003` and click twice: first works, second says the event is full.
Type `EVT-1004`: rejected because it is not published.

**Logs:** register for EVT-1001 and look at the console. `cid` is the correlation ID of the request:

```
Request received: POST /api/events/EVT-1001/registrations
op=registerInterest eventId=EVT-1001 step=eventFound
op=recordActivity message="Registration ... was created for event EVT-1001."
op=registerInterest eventId=EVT-1001 registrationId=... outcome=SUCCESS
Request completed: status=201
```

## Tests

`EventRulesTest` checks: zero capacity rejected, past date rejected, visitors can't see unpublished events,
publishing makes an event visible, registration rejected for unpublished event, registration rejected when full.
Tests use a temporary file, so your real data is not touched.

## Simplifications

- Demo roles instead of real login.
- The whole JSON file is rewritten on every change (fine for a small demo).
- Only one backend service (the optional second service was not done).
- Only two statuses: PENDING_REVIEW and PUBLISHED.
