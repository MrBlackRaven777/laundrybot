package il.blackraven.klita.orm;

import com.github.f4b6a3.ulid.Ulid;
import il.blackraven.klita.DataUtils;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Job {

    private UUID id;
    private int status;
    private Timestamp putTime;
    private Timestamp expiry;
    private int machineId;
    private UUID programId;
    private long userTgId;

    @Nullable
    private UUID notificationId;

    public Job(int status, int machineId, UUID programId, long userTgId) {
        this.id = Ulid.fast().toUuid();
        this.status = status;
        this.putTime = new Timestamp(System.currentTimeMillis());
        Program program = DataUtils.getProgramByUUID(programId);
//        assert program != null;
        this.expiry = new Timestamp(this.putTime.getTime() + TimeUnit.MINUTES.toMillis(program.getDuration()));
        this.machineId = machineId;
        this.programId = programId;
        this.userTgId = userTgId;
    }

    public Job(UUID id, int status, Timestamp putTime, Timestamp expiry, int machineId, UUID programId, long userTgId, @Nullable UUID notificationId) {
        this.id = id;
        this.status = status;
        this.putTime = putTime;
        this.expiry = expiry;
        this.machineId = machineId;
        this.programId = programId;
        this.userTgId = userTgId;
        this.notificationId = notificationId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Timestamp getPutTime() {
        return putTime;
    }

    public void setPutTime(Timestamp putTime) {
        this.putTime = putTime;
    }

    public Timestamp getExpiry() {
        return expiry;
    }

    public void setExpiry(Timestamp expiry) {
        this.expiry = expiry;
    }

    public int getMachineId() {
        return machineId;
    }

    public void setMachineId(int machineId) {
        this.machineId = machineId;
    }

    public UUID getProgramId() {
        return programId;
    }

    public void setProgramId(UUID programId) {
        this.programId = programId;
    }

    public long getUserTgId() {
        return userTgId;
    }

    public void setUserTgId(long userTgId) {
        this.userTgId = userTgId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }
}
