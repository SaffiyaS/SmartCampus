package com.smartcampus.resource;

import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Sensor body is required\"}")
                    .build();
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        DataStore.sensors().put(sensor.getId(), sensor);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();
        return Response.created(location).entity(sensor).build();
    }
}
