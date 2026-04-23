# SmartCampus_Course-work_w2120283-20231715

# Smart Campus API

A JAX-RS (Jersey 2.x) RESTful API for managing campus rooms, sensors, and sensor readings. Packaged as a WAR for deployment on Apache Tomcat.

Base path: `/api/v1`

## Error responses

All errors are returned as JSON. Stack traces are never exposed.

| Status | Condition                                                                 |
| ------ | ------------------------------------------------------------------------- |
| 403    | `SensorUnavailableException` — POST reading on a sensor in `MAINTENANCE`. |
| 404    | Resource not found (Room, Sensor).                                        |
| 409    | `RoomNotEmptyException` — DELETE room that still has sensors attached.    |
| 422    | `LinkedResourceNotFoundException` — Sensor references a non-existent room.|
| 500    | Any unhandled `Throwable`.                                                |

Example body:

```json
{
  "status": 409,
  "error": "Room not empty",
  "message": "Room R1 cannot be deleted: it still has 2 sensor(s) attached."
}
```
