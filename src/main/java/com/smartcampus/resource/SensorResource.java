package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorStatus;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Context
    private UriInfo uriInfo;

    @GET
    public Collection<Sensor> listSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = DataStore.sensors().values();
        if (type == null || type.isBlank()) {
            return new ArrayList<>(all);
        }
        List<Sensor> filtered = all.stream()
                .filter(s -> type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
        return filtered;
    }

    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with id '" + sensorId + "' does not exist.");
        }
        return sensor;
    }

    /**
     * Sub-resource locator for readings under a specific sensor.
     *
     * IMPORTANT: this method intentionally carries NO @GET / @POST
     * annotation. Only @Path is permitted on a sub-resource locator so
     * that JAX-RS hands off the remaining path to the returned
     * SensorReadingResource instance, whose own @GET / @POST methods
     * handle the requests. The locator also wires the parent Sensor
     * into the sub-resource so side effects (e.g. updating
     * currentValue) can be applied cleanly.
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with id '" + sensorId + "' does not exist.");
        }
        return new SensorReadingResource(sensor);
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Sensor body is required\"}")
                    .build();
        }
        // Validate that the referenced room actually exists before creating the sensor
        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            throw new LinkedResourceNotFoundException(
                    "Sensor.roomId is required and must reference an existing room.");
        }
        Room room = DataStore.rooms().get(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Referenced room '" + roomId + "' does not exist.");
        }

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        if (sensor.getStatus() == null) {
            sensor.setStatus(SensorStatus.ACTIVE);
        }
        DataStore.sensors().put(sensor.getId(), sensor);

        // Keep the room's sensorIds list in sync
        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();
        return Response.created(location).entity(sensor).build();
    }
}
