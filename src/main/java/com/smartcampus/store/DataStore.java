package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DataStore {

    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();
    private static final Map<String, Sensor> SENSORS = new ConcurrentHashMap<>();
    private static final Map<String, List<SensorReading>> READINGS = new ConcurrentHashMap<>();

    private DataStore() {
    }

    public static Map<String, Room> rooms() {
        return ROOMS;
    }

    public static Map<String, Sensor> sensors() {
        return SENSORS;
    }

    public static Map<String, List<SensorReading>> readings() {
        return READINGS;
    }
}
