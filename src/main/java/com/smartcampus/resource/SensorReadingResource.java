package com.smartcampus.resource;

import com.smartcampus.model.Sensor;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor parentSensor;

    public SensorReadingResource(Sensor parentSensor) {
        this.parentSensor = parentSensor;
    }
}
