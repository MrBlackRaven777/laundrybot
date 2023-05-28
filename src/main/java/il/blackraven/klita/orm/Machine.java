package il.blackraven.klita.orm;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Machine {
    private int id;
    private MachineTypes type;
    private String laundry_id;
    private BigDecimal cost;
    private int status;
    private Timestamp statusLastUpdate;

    public Machine(int id, MachineTypes type, String laundry_id, BigDecimal cost, int status, Timestamp statusLastUpdate) {
        this.id = id;
        this.type = type;
        this.laundry_id = laundry_id;
        this.cost = cost;
        this.status = status;
        this.statusLastUpdate = statusLastUpdate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MachineTypes getType() {
        return type;
    }

    public void setType(MachineTypes type) {
        this.type = type;
    }

    public String getLaundry_id() {
        return laundry_id;
    }

    public void setLaundry_id(String laundry_id) {
        this.laundry_id = laundry_id;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Timestamp getStatusLastUpdate() {
        return statusLastUpdate;
    }

    public void setStatusLastUpdate(Timestamp statusLastUpdate) {
        this.statusLastUpdate = statusLastUpdate;
    }
}