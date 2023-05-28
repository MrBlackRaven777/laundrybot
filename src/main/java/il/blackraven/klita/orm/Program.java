package il.blackraven.klita.orm;

//import com.google.gson.Gson;

import com.google.gson.Gson;

import java.util.UUID;

public class Program {
    private UUID id;
    private int machineId;
    private String internalName;
    private String publicName;
    private int temperature;
    private int duration;

    public Program(UUID id, int machineId, String internalName, String publicName, int temperature, int duration) {
        this.id = id;
        this.machineId = machineId;
        this.internalName = internalName;
        this.publicName = publicName;
        this.temperature = temperature;
        this.duration = duration;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getMachineId() {
        return machineId;
    }

    public void setMachineId(int machineId) {
        this.machineId = machineId;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
