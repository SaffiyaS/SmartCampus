# Smart Campus API

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

| Scenario                                   | Status |
|--------------------------------------------|--------|
| Resource not found                         | 404    |
| Sensor references unknown room             | 422    |
| Room has sensors on DELETE                 | 409    |
| POST reading while sensor MAINTENANCE/OFFL | 403    |
| Unhandled error                            | 500    |

All error bodies are JSON of the form:

```json
{ "status": 409, "error": "Conflict", "message": "..." }
```
