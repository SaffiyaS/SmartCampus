package com.smartcampus.resource;

import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor parentSensor;

    @Context
    private UriInfo uriInfo;

    public SensorReadingResource(Sensor parentSensor) {
        this.parentSensor = parentSensor;
    }

    @GET
    public List<SensorReading> listReadings() {
        List<SensorReading> readings =
                DataStore.readings().get(parentSensor.getId());
        if (readings == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(readings);
    }

    @POST
    public Response addReading(SensorReading reading) {
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"SensorReading body is required\"}")
                    .build();
        }
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        DataStore.readings()
                .computeIfAbsent(parentSensor.getId(), k -> new ArrayList<>())
                .add(reading);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(reading.getId())
                .build();
        return Response.created(location).entity(reading).build();
    }
}
