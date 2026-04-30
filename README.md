# Smart Campus API

**Module:** 5COSC022W Client-Server Architectures (2025/26)
**Student:** M.S.F.Saffiya
**Student ID:** 20231715
**GitHub:** _\<your repo URL>_

A JAX-RS (Jersey 2.41) RESTful API for managing rooms, sensors and
sensor readings on a university campus. Packaged as a WAR and
deployed on **Apache Tomcat 9**. Storage is purely in-memory
(`ConcurrentHashMap`); there is no database.

## Stack

- Java 11
- JAX-RS via Jersey 2.41 (`javax.ws.rs.*`)
- Jackson for JSON
- Packaging: WAR
- Servlet container: Tomcat 9.x

## Build & Deploy

```bash
mvn clean package
# produces target/SmartCampusAPI.war
# drop into <tomcat>/webapps/ or deploy via NetBeans
```

Base URL once deployed: `http://localhost:8080/SmartCampusAPI/api/v1`

## Project layout

```
src/main/java/com/smartcampus/
  app/SmartCampusApplication.java     -- @ApplicationPath("/api/v1")
  model/Room.java
  model/Sensor.java
  model/SensorStatus.java
  model/SensorReading.java
  store/DataStore.java                -- static ConcurrentHashMaps
  resource/DiscoveryResource.java     -- GET /api/v1
  resource/RoomResource.java          -- /rooms
  resource/SensorResource.java        -- /sensors (+ sub-resource locator)
  resource/SensorReadingResource.java -- /sensors/{id}/readings
  exception/
    RoomNotEmptyException
    LinkedResourceNotFoundException
    SensorUnavailableException
  mapper/
    RoomNotEmptyExceptionMapper        (409)
    LinkedResourceNotFoundExceptionMapper (422)
    SensorUnavailableExceptionMapper   (403)
    NotFoundExceptionMapper            (404)
    ThrowableExceptionMapper           (500)
  filter/LoggingFilter.java           -- ContainerRequest/ResponseFilter
src/main/webapp/WEB-INF/web.xml       -- Jersey servlet mapping
```

## Endpoints

### Discovery

```bash
curl -s http://localhost:8080/SmartCampusAPI/api/v1/
```

### Rooms

```bash
# list
curl -s http://localhost:8080/SmartCampusAPI/api/v1/rooms

# create
curl -i -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"R-101","name":"Lecture Theatre A","capacity":120}'

# get one
curl -s http://localhost:8080/SmartCampusAPI/api/v1/rooms/R-101

# delete (409 if sensors attached)
curl -i -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/R-101
```

### Sensors

```bash
# list all / filter by type
curl -s "http://localhost:8080/SmartCampusAPI/api/v1/sensors"
curl -s "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=temperature"

# create (422 if roomId does not exist)
curl -i -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"S-1","type":"temperature","status":"ACTIVE","roomId":"R-101"}'

# get one
curl -s http://localhost:8080/SmartCampusAPI/api/v1/sensors/S-1
```

### Readings (sub-resource)

```bash
# list readings for a sensor
curl -s http://localhost:8080/SmartCampusAPI/api/v1/sensors/S-1/readings

# post a reading (updates sensor.currentValue;
# 403 if sensor is MAINTENANCE or OFFLINE)
curl -i -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/S-1/readings \
  -H "Content-Type: application/json" \
  -d '{"timestamp":1730000000000,"value":21.7}'
```

## Error codes

| Scenario                                            | Status |
|-----------------------------------------------------|--------|
| Resource not found                                  | 404    |
| Sensor references unknown room                      | 422    |
| Room has sensors on DELETE                          | 409    |
| POST reading while sensor is MAINTENANCE or OFFLINE | 403    |
| Unhandled error                                     | 500    |

All error bodies are JSON of the form:

```json
{ "status": 409, "error": "Conflict", "message": "..." }
```

---

# Conceptual Coursework Report

## Part 1 – Setup & Discovery

### 1.1 JAX-RS Resource Lifecycle

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? How does this affect how you manage in-memory data?

**Answer:** By default, JAX-RS uses a per-request lifecycle, not a singleton one. That means Jersey creates a brand new instance of `RoomResource`, `SensorResource`, or `SensorReadingResource` every time a request comes in, runs the matching method, sends the response, and then throws the instance away. If I tried to keep my rooms or sensors inside a normal field on one of those resource classes, that data would simply disappear after each request.

To work around this, all of my actual data lives in a separate class called `DataStore`, with `static` maps that survive across requests:

```java
private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();
private static final Map<String, Sensor> SENSORS = new ConcurrentHashMap<>();
private static final Map<String, List<SensorReading>> READINGS = new ConcurrentHashMap<>();
```

Tomcat is multi-threaded, so two requests can be hitting these maps at exactly the same moment. A regular `HashMap` would be unsafe in that situation and could lose data or even crash. I used `ConcurrentHashMap` because it is built for concurrent access. It also exposes safe atomic methods like `computeIfAbsent`, which I rely on inside `SensorReadingResource` to grow each sensor's history list without two threads stepping on each other.

### 1.2 HATEOAS

**Question:** Why is the provision of hypermedia (links and navigation within responses) considered a hallmark of advanced RESTful design? How does this benefit client developers compared to static documentation?

**Answer:** HATEOAS is considered the most mature form of REST because the API ends up describing itself at runtime. Instead of giving the client developer a separate PDF or webpage of URLs to copy into their code, the server includes the next available links inside every response. My `DiscoveryResource` shows this directly:

```json
{
  "name": "Smart Campus API",
  "version": "1.0.0",
  "_links": {
    "self":    "http://localhost:8080/SmartCampusAPI/api/v1",
    "rooms":   "http://localhost:8080/SmartCampusAPI/api/v1/rooms",
    "sensors": "http://localhost:8080/SmartCampusAPI/api/v1/sensors"
  }
}
```

The big practical benefit for a client developer is that they do not have to hard-code my URLs into their app. They start at one entry point, read the `_links`, and follow whichever path they need. If I later rename `/rooms` to something else, or move the whole API to a different host, anyone using HATEOAS just keeps working. Anyone who copy-pasted the URLs out of static documentation will quietly break and won't know until something fails. Static docs also drift out of date the moment the code changes, but the links inside a real response are by definition always current.

## Part 2 – Room Management

### 2.1 IDs vs. Full Objects in Collection Responses

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.

**Answer:** Returning only IDs makes the response payload tiny, but it pushes a much bigger problem onto the client. To actually display anything useful, the client now has to make one extra HTTP call per room to fetch the details. This is the classic "N+1 requests" problem. If a campus has 200 rooms, the client ends up making 201 HTTP calls just to load one screen. That is slow, wastes connections, and feels laggy on the user's end, especially on mobile networks.

Returning the full objects in a single response uses slightly more bandwidth, but the trade-off is worth it because a `Room` is small (just an id, name, capacity, and the list of attached sensor IDs). One slightly bigger response is much faster than 200 small ones. That is why `RoomResource.listRooms()` returns the full objects:

```java
@GET
public Collection<Room> listRooms() {
    return new ArrayList<>(DataStore.rooms().values());
}
```

If a future feature ever genuinely needs only IDs, for example to populate a lightweight dropdown, the cleaner solution is to add a query parameter like `?fields=id` rather than breaking the default behaviour for everyone else.

### 2.2 DELETE Idempotency

**Question:** Is the DELETE operation idempotent in your implementation? Justify by describing what happens if a client sends the exact same DELETE request multiple times.

**Answer:** Yes, my DELETE is idempotent. It is important to know that idempotency in HTTP is about the **end state** of the server, not about always returning the same status code.

The first time a client sends `DELETE /rooms/R-101`, my code finds the room, removes it from `DataStore`, and returns **204 No Content**. The second time the client sends the exact same request, the room is already gone, so my code throws `NotFoundException` and `NotFoundExceptionMapper` turns that into a **404 Not Found** response. The status codes are different, but the actual server state is identical in both cases: the room R-101 does not exist. The client could repeat the request a hundred more times and the server would stay in that same state. That is the definition of idempotent.

For comparison, `POST /rooms` is not idempotent. Sending the same POST twice creates two separate rooms, which is exactly the situation idempotency is meant to avoid.

The relevant logic is inside `RoomResource.deleteRoom`:

```java
Room room = DataStore.rooms().get(roomId);
if (room == null) {
    throw new NotFoundException("Room not found: " + roomId);
}
if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
    throw new RoomNotEmptyException(...);
}
DataStore.rooms().remove(roomId);
return Response.noContent().build();
```

## Part 3 – Sensor Operations & Linking

### 3.1 `@Consumes(APPLICATION_JSON)` and Content-Type Mismatch

**Question:** Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`, to a method annotated `@Consumes(MediaType.APPLICATION_JSON)`.

**Answer:** JAX-RS checks the request's `Content-Type` header before it even calls my method. If the header does not match any of the types listed in `@Consumes`, JAX-RS rejects the request itself and replies with **HTTP 415 Unsupported Media Type**. My `SensorResource.createSensor()` body is never executed, so I never have to worry about a piece of XML or plain text accidentally being treated as a `Sensor` object.

This is useful for three reasons. The error happens immediately at the protocol level, so the server never crashes mid-parse on something it was never built to read. It also keeps the resource code clean, because I do not have to write defensive checks at the top of every POST method asking "is this even JSON?" And finally, the annotation acts as live documentation: tools like Postman and Swagger read it directly and automatically set the right `Content-Type` for the developer using the API.

### 3.2 `@QueryParam` vs. Path-Based Filtering

**Question:** You implemented filtering using `@QueryParam`. Contrast this with placing the filter inside the URL path (e.g. `/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:** In REST, the URL **path** is supposed to identify a resource, while the **query string** is for filtering or paging that resource. `/api/v1/sensors` is the full collection of sensors; adding `?type=CO2` just asks for a subset of the same collection. If I instead made the type part of the path like `/sensors/type/CO2`, I would be implying that "type" is a permanent sub-folder inside sensors, which is not really true. It is a temporary view, not a different resource.

The query-parameter approach also keeps my code much simpler. One method handles every case:

```java
@GET
public Collection<Sensor> listSensors(@QueryParam("type") String type) {
    Collection<Sensor> all = DataStore.sensors().values();
    if (type == null || type.isBlank()) return new ArrayList<>(all);
    return all.stream()
              .filter(s -> type.equalsIgnoreCase(s.getType()))
              .collect(Collectors.toList());
}
```

When I want to add another filter later, like `?status=ACTIVE` or `?roomId=LIB-301`, I just add another parameter to the same method. With path-based filtering I would need a brand new endpoint for every combination, and the controller would explode in size.

## Part 4 – Sub-Resources

### 4.1 The Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller?

**Answer:** A sub-resource locator is a method that has only `@Path` on it (no `@GET` or `@POST`), and instead of returning a value to the client it returns another resource object. Jersey then takes the rest of the URL and matches it against that returned object. In my project, `SensorResource` has this locator:

```java
@Path("{sensorId}/readings")
public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
    Sensor sensor = DataStore.sensors().get(sensorId);
    if (sensor == null) {
        throw new NotFoundException("Sensor with id '" + sensorId + "' does not exist.");
    }
    return new SensorReadingResource(sensor);
}
```

This pattern keeps things tidy in three useful ways. First, each class only deals with one type of resource: `SensorResource` is about sensors, `SensorReadingResource` is about readings, and neither file grows out of control as new features are added. Second, the locator does the parent lookup once and passes the actual `Sensor` object into the sub-resource through its constructor, so every method inside `SensorReadingResource` already has the parent context it needs without re-fetching it each time. Third, the "does this sensor even exist?" check happens in one place — the locator — instead of being copied at the top of every reading method.

The alternative, where every nested path is mapped inside one giant controller, mixes two completely different resources in one file, duplicates the parent lookup, and quickly becomes very hard to maintain.

## Part 5 – Error Handling & Logging

### 5.2 Why 422 Unprocessable Entity Beats 404 for a Missing Reference

**Question:** Why is HTTP 422 often considered more semantically accurate than 404 when the issue is a missing reference inside an otherwise valid JSON payload?

**Answer:** A 404 response really means "the URL you hit does not exist." When a client sends `POST /api/v1/sensors` with a JSON body that points to a `roomId` which doesn't exist, the URL itself is perfectly fine — the sensors collection is right there and the server understood what the client was trying to do. The problem is one level deeper: the JSON parsed correctly, but a value inside it points to something that doesn't exist on the server side. Returning 404 in that situation is misleading because nothing about the URL was wrong.

That is exactly the case **HTTP 422 Unprocessable Entity** is designed for. It tells the client "I understood your request and your JSON, but I cannot do what you asked because the data inside it doesn't make sense." My `LinkedResourceNotFoundExceptionMapper` returns 422 for this reason, so client apps can clearly tell apart "I typed the wrong URL" (404) from "my payload references something that does not exist" (422). This actually matters in practice because some clients automatically retry 404s against backup hosts. Retrying a 422 the same way would never succeed, no matter how many hosts it tried.

### 5.4 Risks of Leaking Java Stack Traces

**Question:** From a cybersecurity standpoint, what are the risks of exposing internal Java stack traces to external API consumers? What specific information could an attacker gather from such a trace?

**Answer:** A raw Java stack trace is essentially a free reconnaissance gift to an attacker. From just one leaked trace, they can usually figure out:

- The exact framework and library versions in use, by looking at class names like `org.glassfish.jersey.server.ServerRuntime` or `com.fasterxml.jackson.databind.JsonMappingException`. They can then look those versions up in public CVE databases and pick a known exploit that works on them.
- The internal package and class structure of the app (for example `com.smartcampus.store.DataStore`), which gives them a map of how the system is organised and what to attack next.
- Hints about the operating system and deployment, when JDK paths or container paths show up in the trace.
- The whole technology stack — servlet container, JSON library, and so on — which makes it much easier to choose the right exploit toolkit.
- Which inputs reach which internal methods, useful for crafting a follow-up payload that triggers a deeper bug.

To stop any of this from leaking, I have a global `ThrowableExceptionMapper`. If the exception is already a JAX-RS `WebApplicationException`, it lets that response through so my more specific mappers (404, 409, 422, 403) keep working. For anything else, it logs the full trace on the server side at level `SEVERE` and returns only a clean, generic JSON 500 to the client:

```java
LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", exception);
return Response.status(500)
               .type(APPLICATION_JSON)
               .entity(Map.of("status", 500, "error", "Internal Server Error",
                              "message", "An unexpected error occurred. Please try again later."))
               .build();
```

This way I can still see the full stack trace in `catalina.<date>.log` when I'm debugging, but the outside world only sees a vague "something went wrong" message that gives nothing away.

### 5.5 Why Use JAX-RS Filters for Logging Instead of `Logger.info` Calls

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every resource method?

**Answer:** Logging is a cross-cutting concern, meaning it applies to the entire application but is not really part of any specific resource's job. If I sprinkled `Logger.info(...)` calls at the top and bottom of every method in every resource class, I would run into a lot of problems. The first one is coverage: as soon as a developer adds a new endpoint and forgets to add the log line, that endpoint becomes silently invisible. The second is consistency, because different developers would format the message slightly differently and that breaks anything that searches the logs. The third is that the actual business logic of the resource gets buried under boilerplate, making the code harder to read. And finally, refactors and renames easily leave behind orphaned or duplicated log lines.

A JAX-RS filter solves all of these at once. My entire logging setup lives in a single class, `LoggingFilter`, which implements both `ContainerRequestFilter` and `ContainerResponseFilter`. The runtime applies it automatically to every request and every response, no matter how many resource classes I add later:

```java
LOGGER.info(() -> "--> " + method + " " + uri);
...
LOGGER.info(() -> "<-- " + status + " " + method + " " + uri);
```

If I add a brand new endpoint tomorrow, it gets logged automatically with no extra work. If I want to add request IDs, response timing, or audit information later, I only have to change this one file rather than touching every resource method in the project.
