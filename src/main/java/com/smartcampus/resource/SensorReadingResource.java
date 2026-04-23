package com.smartcampus.resource;

import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor parentSensor;

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
}
