package il.blackraven.klita;

import com.github.f4b6a3.ulid.Ulid;
import il.blackraven.klita.orm.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static il.blackraven.klita.DataUtils.browseNotificationById;
import static org.junit.jupiter.api.Assertions.*;


public class DataUtilsTest {
    private static Connection connection;
    @BeforeAll
    public static void setUp() {
        DataUtils.init();
        connection = DataUtils.getConnection();
    }

    @Test
    public void dataConnectionTest(){
        Connection conn = DataUtils.getConnection();
        assertNotNull(conn);
    }

    @Test
    public void getAllMachinesTest() {
        ArrayList<Machine> machines = DataUtils.getAllMachines();
        assertEquals(5, machines.size());
        assertEquals(1, machines.get(0).getId());
        assertEquals(MachineTypes.wash, machines.get(0).getType());
        assertEquals(MachineTypes.dry, machines.get(3).getType());
    }

    @Test
    void getMachineByLaundryIdTest() {
        //TODO
        for (int i = 1; i < 6; i++) {
            Machine machine = DataUtils.getMachineByLaundryId(String.valueOf(i));
            assertEquals(i, machine.getId());
        }
    }

    @Test
    public void getMachinesByTypeTest() {
        ArrayList<Machine> washMachines = DataUtils.getMachinesByType(MachineTypes.wash);
        ArrayList<Machine> dryMachines = DataUtils.getMachinesByType(MachineTypes.dry);
        assertEquals(3, washMachines.size());
        assertEquals(2, dryMachines.size());
        assertEquals(3L, washMachines.stream().filter(m -> m.getType().equals(MachineTypes.wash)).count());
        assertEquals(2L, dryMachines.stream().filter(m -> m.getType().equals(MachineTypes.dry)).count());
    }

    @Test
    public void machineChangeStatusTest() {
        Machine machine = DataUtils.getMachineByLaundryId("1");
        int id = machine.getId();
        assertEquals(machine.getStatus(), MachineStatuses.IDLE);

        assertTrue(DataUtils.machineChangeStatus(id, MachineStatuses.BUSY));
        assertEquals(DataUtils.getMachineByLaundryId("1").getStatus(), MachineStatuses.BUSY);

        assertTrue(DataUtils.machineChangeStatus(id, MachineStatuses.OUT_OF_ORDER));
        assertEquals(DataUtils.getMachineByLaundryId("1").getStatus(), MachineStatuses.OUT_OF_ORDER);
        assertTrue(DataUtils.machineChangeStatus(id, MachineStatuses.IDLE));
    }

    @Test
    public void getAllProgramsTest() {
        ArrayList<Program> programs = DataUtils.getAllPrograms();
        assertEquals(29, programs.size());
    }

    @Test
    public void getMachineProgramsTest() {
        Machine machine = DataUtils.getMachineByLaundryId("2");
        ArrayList<Program> programsByObj = DataUtils.getMachinePrograms(machine);
        assertEquals(6, programsByObj.size());
        ArrayList<Program> programsByStr = DataUtils.getMachinePrograms(machine.getId());
        assertEquals(6, programsByStr.size());
        assertEquals(programsByObj.toString(), programsByStr.toString());
    }
    @Test
    public void getProgramByUUIDTest(){
        UUID uuid = UUID.fromString("13131313-1313-1313-1313-131313131313");
        Program program = DataUtils.getProgramByUUID(uuid);
        assertEquals(2, program.getMachineId());
        assertEquals("wash_hot_90", program.getInternalName());
        assertEquals("\uD83D\uDD25 Hot 90", program.getPublicName());
        assertEquals(90, program.getTemperature());
        assertEquals(40, program.getDuration());
    }

    @Test
    void getProgramByMachineAndInternalName() {

        Program washProgram = DataUtils.getProgramByMachineAndInternalName(2, "wash_cold_40");
        assertNotNull(washProgram);
        assertEquals(2, washProgram.getMachineId());
        assertEquals("wash_cold_40", washProgram.getInternalName());
        assertEquals("❄️ Cold 40", washProgram.getPublicName());
        assertEquals(40, washProgram.getTemperature());
        assertEquals(38, washProgram.getDuration());

        Program dryProgram = DataUtils.getProgramByMachineAndInternalName(5, "dry_high_temp_60");
        assertEquals(5, dryProgram.getMachineId());
        assertEquals("dry_high_temp_60", dryProgram.getInternalName());
        assertEquals("\uD83E\uDD75 High temp", dryProgram.getPublicName());
        assertEquals(60, dryProgram.getTemperature());
        assertEquals(40, dryProgram.getDuration());

        Program nullProgram = DataUtils.getProgramByMachineAndInternalName(8, "dry_high_temp_60");
        assertNull(nullProgram);

        nullProgram = DataUtils.getProgramByMachineAndInternalName(2, "no_such_program");
        assertNull(nullProgram);
    }

    @Test
    public void userMethodsTest(){
        long tgId = 4561237890L;
        BotUser botUser = new BotUser(tgId, "TestUserNEW", Locales.en_US);
        assertTrue(DataUtils.addNewUser(botUser));

        BotUser getBotUser = DataUtils.getUser(String.valueOf(tgId));
        assertEquals(tgId, getBotUser.getTgId());
        assertEquals("TestUserNEW", getBotUser.getName());
        assertTrue(getBotUser.getRegDate().compareTo(new Timestamp(System.currentTimeMillis())) < 0);
        assertEquals(Locales.en_US, getBotUser.getLocale());

        assertTrue(DataUtils.userChangeLocale(String.valueOf(tgId), Locales.ru_RU));
        getBotUser = DataUtils.getUser(String.valueOf(tgId));
        assertEquals(Locales.ru_RU, getBotUser.getLocale());

        assertTrue(DataUtils.deleteUser(String.valueOf(tgId)));
    }

    @Test
    public void jobMethodsTest() {
        long tgId = 9876543210L;

        Machine testMachine = DataUtils.getMachineByLaundryId("1");
        Program testProgram = DataUtils.getMachinePrograms(testMachine).get(0);
        Job putJob = new Job(JobStatus.JOB_STARTED, testMachine.getId(), testProgram.getId(), tgId);
        DataUtils.putNewJob(putJob);
        assertEquals(JobStatus.JOB_STARTED, putJob.getStatus());
        assertNotNull(putJob.getPutTime());
        assertNotNull(putJob.getExpiry());
        assertEquals(testProgram.getDuration() * 60L * 1000L, putJob.getExpiry().getTime() - putJob.getPutTime().getTime());

        Job browseJob = DataUtils.jobBrowse(putJob.getId().toString());
        assertNotNull(browseJob);
        assertEquals(JobStatus.JOB_STARTED, browseJob.getStatus());
        assertNotNull(browseJob.getPutTime());
        assertNotNull(browseJob.getExpiry());
        assertEquals(testProgram.getDuration() * 60L * 1000L, browseJob.getExpiry().getTime() - browseJob.getPutTime().getTime());

        DataUtils.jobChangeStatus(putJob.getId().toString(), JobStatus.JOB_ENDED);
        assertEquals(JobStatus.JOB_ENDED, DataUtils.jobBrowse(putJob.getId().toString()).getStatus());

        Job getJob = DataUtils.getJob(putJob.getId().toString());
        assertNotNull(getJob);
        assertEquals(JobStatus.JOB_ENDED, getJob.getStatus());
        assertNotNull(getJob.getPutTime());
        assertNotNull(getJob.getExpiry());
        assertEquals(testProgram.getDuration() * 60L * 1000L, getJob.getExpiry().getTime() - getJob.getPutTime().getTime());
        assertNull(DataUtils.jobBrowse(putJob.getId().toString()));
    }

    @Test
    public void browseAllStartedJobsTest() {
        long tgId = 9876543210L;

        for (int i = 1; i < 6; i++) {
            Machine testMachine = DataUtils.getMachineByLaundryId(String.valueOf(i));
            Program testProgram = DataUtils.getMachinePrograms(testMachine).get(new Random().nextInt(3));
            Job putJob = new Job(JobStatus.JOB_STARTED, testMachine.getId(), testProgram.getId(), tgId);
            DataUtils.putNewJob(putJob);
        }
        ArrayList<Job> jobList = DataUtils.browseAllJobsWithStatus(JobStatus.JOB_STARTED);
        assertEquals(5, jobList.size());
        for (Job job : jobList) {
            assertEquals(JobStatus.JOB_STARTED, job.getStatus());
            assertTrue(job.getPutTime().getTime() < System.currentTimeMillis());
            assertTrue(job.getExpiry().getTime() > System.currentTimeMillis());
            System.out.println(job.getId());
        }
    }

    @Test
    public void notificationsTest() throws InterruptedException {
        long tgId = 9876543210L;

        Machine testMachine = DataUtils.getMachineByLaundryId("1");
        Program testProgram = DataUtils.getMachinePrograms(testMachine).get(0);
        Job putJob = new Job(JobStatus.JOB_STARTED, testMachine.getId(), testProgram.getId(), tgId);
        DataUtils.putNewJob(putJob);

        Notification notification0 = new Notification(Ulid.fast().toUuid(),
                putJob.getId(),
                tgId,
                "Some message 0",
                NotificationType.JOB_ABOUT_TO_END,
                0,
                1,
                new Timestamp(System.currentTimeMillis()));

        assertTrue(DataUtils.putNotification(notification0));
        Notification getNotification = browseNotificationById(notification0.getId());
        assertEquals("Some message 0", getNotification.getMessage());

        Notification notification1 = new Notification(Ulid.fast().toUuid(),
                putJob.getId(),
                tgId,
                "Some message 1",
                NotificationType.JOB_ABOUT_TO_END,
                0,
                1,
                new Timestamp(System.currentTimeMillis()));
        Notification notification2 = new Notification(Ulid.fast().toUuid(),
                putJob.getId(),
                tgId,
                "Some message 2",
                NotificationType.JOB_ABOUT_TO_END,
                0,
                1,
                new Timestamp(System.currentTimeMillis()));

        Notification notification3 = new Notification(Ulid.fast().toUuid(),
                putJob.getId(),
                tgId,
                "Some message 3",
                NotificationType.JOB_END,
                0,
                1,
                new Timestamp(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5)));

        Notification notification4 = new Notification(Ulid.fast().toUuid(),
                putJob.getId(),
                tgId,
                "Some message 4",
                NotificationType.JOB_END,
                0,
                1,
                new Timestamp(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)));

        Notification notification5 = new Notification(Ulid.fast().toUuid(),
                putJob.getId(),
                tgId,
                "Some message 5",
                NotificationType.JOB_END,
                0,
                1,
                null);

        assertTrue(DataUtils.putNotification(notification1));
        assertTrue(DataUtils.putNotification(notification2));
        assertTrue(DataUtils.putNotification(notification3));
        assertTrue(DataUtils.putNotification(notification4));
        assertTrue(DataUtils.putNotification(notification5));

        ArrayList<Notification> notifications = DataUtils.browseAllUpcomingNotifications();
        assertEquals(3, notifications.size());
        Thread.sleep(5000);
        notifications = DataUtils.browseAllUpcomingNotifications();
        assertEquals(4, notifications.size());
        Thread.sleep(5000);
        notifications = DataUtils.browseAllUpcomingNotifications();
        assertEquals(5, notifications.size());

        for (Notification notification :
                notifications) {
            DataUtils.rescheduleNotification(notification.getId(), 5000);
        }
        notifications = DataUtils.browseAllUpcomingNotifications();
        assertEquals(0, notifications.size());
        Thread.sleep(6000);

        notifications = DataUtils.browseAllUpcomingNotifications();
        assertEquals(5, notifications.size());
        for (Notification notification :
                notifications) {
            assertEquals(1, notification.getDeliveryCount());
        }

        for (Notification notification :
                notifications) {
            DataUtils.declineNotification(notification.getId());
        }
        notifications = DataUtils.browseAllUpcomingNotifications();
        assertEquals(0, notifications.size());
        for (Notification notification :
                notifications) {
            assertNull(notification.getNextDeliveryTime());
        }
    }

    @Test
    public void addEventLogTest() {
        Event event = new Event(0, 0, Event.COMMAND, "Test event log", new Object());
        assertTrue(EventLogger.log(event));
    }

    @AfterEach
    public void clean() {
        DataUtils.deleteUser(String.valueOf(4561237890L));

        String deleteAllNotificationsQuery = "TRUNCATE TABLE notifications CASCADE";
        String deleteAllJobsQuery = "TRUNCATE TABLE jobs CASCADE";
        try {
            PreparedStatement deleteAllNotificationsStatement = connection.prepareStatement(deleteAllNotificationsQuery);
            PreparedStatement deleteAllJobsStatement = connection.prepareStatement(deleteAllJobsQuery);
            deleteAllNotificationsStatement.executeUpdate();
            deleteAllJobsStatement.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    @AfterAll
    public static void cleanup() {
        DataUtils.close();
    }



}
