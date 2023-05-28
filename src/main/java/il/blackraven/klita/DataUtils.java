package il.blackraven.klita;

import com.github.f4b6a3.ulid.Ulid;
import il.blackraven.klita.orm.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public final class DataUtils {

    private static final Logger log = LogManager.getLogger(DataUtils.class);

    private static Connection connection;

    private DataUtils() {
        throw new AssertionError();
    }

    public static void init() {
        String url = String.format("jdbc:postgresql://%s:%s/%s",
                BotConfig.getProperty("db.host"),
                BotConfig.getProperty("db.port"),
                BotConfig.getProperty("db.name"));
        try {
            Connection conn = DriverManager.getConnection(
                    url, BotConfig.getProperty("db.user"), BotConfig.getProperty("db.password"));
            if (conn != null) {
                connection = conn;
                log.info("Connected to the database!");
            } else {
                log.warn("Failed to make connection!");
            }

        } catch (SQLException e) {
            log.error(String.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            init();
        }
        return connection;
    }


    public static ArrayList<Machine> getAllMachines() {
        String query = "SELECT * FROM machines ORDER BY laundry_id ASC";
        ArrayList<Machine> machines = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                machines.add(convertResultToMachine(resultSet));
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return machines;
    }

    public static ArrayList<Machine> getAllIdleMachines() {
        String query = "SELECT * FROM machines WHERE status=? ORDER BY laundry_id ASC";
        ArrayList<Machine> machines = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, MachineStatuses.IDLE);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                machines.add(convertResultToMachine(resultSet));
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return machines;
    }

    public static Machine getMachineById(String id) {
        return getMachineById(Integer.parseInt(id));
    }

    public static Machine getMachineById(int id) {
        String query = "SELECT * FROM machines WHERE id=?";
        ResultSet resultSet;
        Machine machine = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                log.warn("No machine with id '" + id + "'");
                return null;
            }
            machine = convertResultToMachine(resultSet);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return machine;
    }

    public static Machine getMachineByLaundryId(int id) {
        return getMachineByLaundryId(String.valueOf(id));
    }

    public static Machine getMachineByLaundryId(String id) {
        String query = "SELECT * FROM machines WHERE laundry_id=?";
        ResultSet resultSet;
        Machine machine = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, id);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                log.warn("No machine with laundry_id '" + id + "'");
                return null;
            }
            machine = convertResultToMachine(resultSet);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return machine;
    }

    public static ArrayList<Machine> getMachinesByType(MachineTypes type) {
        String query = "SELECT * FROM machines WHERE type=?";
        ArrayList<Machine> machines = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, type, Types.OTHER);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                machines.add(convertResultToMachine(resultSet));
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return machines;
    }

    public static boolean machineChangeStatus(int machineId, int status) {
        //TODO tests
        String query = "UPDATE machines SET status=?, status_last_update=? WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, status);
            statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            statement.setInt(3, machineId);
            int rows = statement.executeUpdate();
            log.info(String.format("Machine %s status changed to '%s'. %s rows affected", machineId, status, rows));
            return true;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return false;
        }
    }

    private static Machine convertResultToMachine(ResultSet resultSet) {
        try {
            int m_id = resultSet.getInt("id");
            MachineTypes m_type = MachineTypes.valueOf(resultSet.getString("type"));
            String laundry_id = resultSet.getString("laundry_id");
            BigDecimal cost = resultSet.getBigDecimal("cost");
            int status = resultSet.getInt("status");
            Timestamp status_last_update = resultSet.getTimestamp("status_last_update");
            Machine machine = new Machine(m_id, m_type, laundry_id, cost, status, status_last_update);
            return machine;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    public static ArrayList<Program> getAllPrograms() {
        String query = "SELECT * FROM programs";
        ArrayList<Program> programs = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                UUID id = UUID.fromString(resultSet.getObject("id").toString());
                int machineId = resultSet.getInt("machine_id");
                String internalName = resultSet.getString("internal_name");
                String publicName = resultSet.getString("public_name");
                int temperature = resultSet.getInt("temperature");
                int duration = resultSet.getInt("duration");
                programs.add(new Program(id, machineId, internalName, publicName, temperature, duration));
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return programs;
    }

    public static ArrayList<Program> getMachinePrograms(int machineId) {
        Machine machine = getMachineById(machineId);
        if (machine == null) return new ArrayList<>();
        return getMachinePrograms(machine);
    }

    public static ArrayList<Program> getMachinePrograms(Machine machine) {
        String query = "SELECT * FROM programs WHERE machine_id=? AND internal_name != 'unknown_prog' ORDER BY temperature ASC";
        ArrayList<Program> programs = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, machine.getId());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                UUID id = UUID.fromString(resultSet.getString("id"));
                int machineId = resultSet.getInt("machine_id");
                String internalName = resultSet.getString("internal_name");
                String publicName = resultSet.getString("public_name");
                int temperature = resultSet.getInt("temperature");
                int duration = resultSet.getInt("duration");
                programs.add(new Program(id, machineId, internalName, publicName, temperature, duration));
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return programs;
    }

    public static Program getProgramByUUID(UUID programId) {
        String query = "SELECT * FROM programs WHERE id=?";
        ArrayList<Program> programs = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, programId, Types.OTHER);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                UUID id = UUID.fromString(resultSet.getObject("id").toString());
                int machineId = resultSet.getInt("machine_id");
                String internalName = resultSet.getString("internal_name");
                String publicName = resultSet.getString("public_name");
                int temperature = resultSet.getInt("temperature");
                int duration = resultSet.getInt("duration");
                return new Program(id, machineId, internalName, publicName, temperature, duration);
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return null;
    }

    public static Program getProgramByMachineAndInternalName(int machineId, String internalName) {
        String query = "SELECT * FROM programs WHERE machine_id=? AND internal_name=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, machineId);
            statement.setString(2, internalName);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) return null;
            UUID id = UUID.fromString(resultSet.getObject("id").toString());
            String publicName = resultSet.getString("public_name");
            int temperature = resultSet.getInt("temperature");
            int duration = resultSet.getInt("duration");
            return new Program(id, machineId, internalName, publicName, temperature, duration);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return null;
    }

    public static BotUser getUser(String userId) {
        String query = "SELECT * FROM users WHERE tg_id=?";
        ResultSet resultSet;
        BotUser botUser = null;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, Long.parseLong(userId));
            resultSet = statement.executeQuery();
            if (!resultSet.next()) return null;
            long tgId = Long.parseLong(userId);
            String name = resultSet.getString("name");
            Timestamp regDate = resultSet.getTimestamp("reg_date");
            Locales locale = Locales.valueOf(resultSet.getString("locale"));
            botUser = new BotUser(tgId, name, regDate, locale);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return botUser;
    }

    public static boolean addNewUser(BotUser botUser) {
        String query = "INSERT INTO users (tg_id, name, reg_date, locale) VALUES(?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, botUser.getTgId());
            statement.setString(2, botUser.getName());
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            statement.setObject(4, botUser.getLocale(), Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("New user added. %s rows affected", rows));
            return true;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(String userId) {
        String query = "DELETE FROM users WHERE tg_id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, Long.parseLong(userId));
            int rows = statement.executeUpdate();
            log.info(String.format("User %s deleted. %s rows affected", userId, rows));
            return true;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return false;
        }
    }

    public static Locales getUserLocale(String userId) {
        return getUserLocale(Long.parseLong(userId));
    }

    public static Locales getUserLocale(long userId) {
        String query = "SELECT locale FROM users WHERE tg_id=?";
        ResultSet resultSet;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) return Locales.valueOf(resultSet.getString("locale"));
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return null;
    }

    public static boolean userChangeLocale(String userId, Locales locale) {
        String query = "UPDATE users SET locale=? WHERE tg_id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, locale, Types.OTHER);
            statement.setLong(2, Long.parseLong(userId));
            int rows = statement.executeUpdate();
            log.info(String.format("User %s locale changed to '%s'. %s rows affected", userId, locale, rows));
            return true;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return false;
        }
    }

    public static Job putNewJob(Job job) {
        String query = "INSERT INTO jobs (id, status, put_time, expiry, machine_id, program_id, user_tg_id) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setObject(1, job.getId(), Types.OTHER);
            statement.setInt(2, job.getStatus());
            statement.setTimestamp(3, job.getPutTime());
            statement.setTimestamp(4, job.getExpiry());
            statement.setInt(5, job.getMachineId());
            statement.setObject(6, job.getProgramId(), Types.OTHER);
            statement.setLong(7, job.getUserTgId());
            int rows = statement.executeUpdate();

            log.info(String.format("New job added. %s rows affected", rows));
            return job;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return job;
        }
    }

    public static Job jobChangeStatus(UUID jobUUID, int newStatus) {
        return jobChangeStatus(jobUUID.toString(), newStatus);
    }

    public static Job jobChangeStatus(String jobUUID, int newStatus) {
        String query = "UPDATE jobs SET status=? WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, newStatus);
            statement.setObject(2, UUID.fromString(jobUUID), Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("Job %s status changed to '%s'. %s rows affected", jobUUID, newStatus, rows));
            return jobBrowse(jobUUID);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    public static Job jobChangeNotificationId(UUID jobUUID, UUID notificationUUID) {
        String query = "UPDATE jobs SET notification_id=? WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, notificationUUID, Types.OTHER);
            statement.setObject(2, jobUUID, Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("Job %s notification_id changed to '%s'. %s rows affected", jobUUID, notificationUUID, rows));
            return jobBrowse(jobUUID);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    public static Job jobBrowse(UUID jobUUID) {
        return jobBrowse(jobUUID.toString());
    }

    public static Job jobBrowse(String jobUUID) {
        String query = "SELECT * FROM jobs WHERE id=?";
        ResultSet resultSet;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, UUID.fromString(jobUUID), Types.OTHER);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return convertResultToJob(resultSet);
            }
            return null;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return null;
    }

    public static ArrayList<Job> browseAllActiveJobs() {
        //TODO tests
        String query = "SELECT * FROM (SELECT * FROM jobs ORDER BY id DESC) AS jobs WHERE jobs.status!=?";
        ResultSet resultSet;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, JobStatus.JOB_ENDED);
            resultSet = statement.executeQuery();
            ArrayList<Job> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(convertResultToJob(resultSet));
            }
            return jobs;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static ArrayList<Job> browseAllJobsWithStatus(int status) {
        String query = "SELECT * FROM (SELECT * FROM jobs ORDER BY id DESC) AS jobs WHERE jobs.status=? ";
        ResultSet resultSet;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, status);
            resultSet = statement.executeQuery();
            ArrayList<Job> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(convertResultToJob(resultSet));
            }
            return jobs;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static Job browseActiveJobByMachineId(int machineId) {
        //TODO tests
        ArrayList<Job> activeJobs = browseAllActiveJobs();
        return activeJobs.stream().filter(job -> job.getMachineId() == machineId).findFirst().orElse(null);
    }

    public static Job getJob(String jobUUID) {
        Job job = jobBrowse(jobUUID);
        if (job == null) {
            log.warn("No job with uuid " + jobUUID + ". Nothing to get");
            return null;
        }
        String query = "DELETE FROM jobs WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, UUID.fromString(jobUUID), Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("Job %s deleted. %s rows affected", jobUUID, rows));
            return job;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    private static Job convertResultToJob(ResultSet resultSet) {
        try {
            UUID id = UUID.fromString(resultSet.getObject("id").toString());
            int status = resultSet.getInt("status");
            Timestamp putTime = resultSet.getTimestamp("put_time");
            Timestamp expiry = resultSet.getTimestamp("expiry");
            int machineId = resultSet.getInt("machine_id");
            UUID programId = UUID.fromString(resultSet.getObject("program_id").toString());
            long userTgId = resultSet.getLong("user_tg_id");
            //TODO think about more elegant mapping
            Object notificationIdObj = resultSet.getObject("notification_id");
            UUID notificationId = notificationIdObj != null ? UUID.fromString(String.valueOf(notificationIdObj))
                    : null;
            Job job = new Job(id,
                    status,
                    putTime,
                    expiry,
                    machineId,
                    programId,
                    userTgId,
                    notificationId);
            return job;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    public static boolean putNotification(Notification notification) {
        String query = "INSERT INTO notifications (id, job_id, user_tg_id, message_id, message, type, delivery_count, max_delivery_count, next_delivery_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, notification.getId(), Types.OTHER);
            statement.setObject(2, notification.getJobId(), Types.OTHER);
            statement.setLong(3, notification.getUserTgId());
            statement.setInt(4, notification.getMessageId());
            statement.setString(5, notification.getMessage());
            statement.setObject(6, notification.getType(), Types.OTHER);
            statement.setInt(7, notification.getDeliveryCount());
            statement.setInt(8, notification.getMaxDeliveryCount());
            statement.setTimestamp(9, notification.getNextDeliveryTime());
            int rows = statement.executeUpdate();
            log.info(String.format("New notification added. %s rows affected", rows));
            return true;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return false;
        }
    }

    public static Notification browseNotificationById(UUID notificationId) {
        String query = "SELECT * FROM notifications WHERE id=?";
        ResultSet resultSet;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, notificationId, Types.OTHER);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return convertResultToNotification(resultSet);
            }
            return null;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return null;
    }


    public static Notification browseNotificationByJobId(UUID jobUUID) {
        String query = "SELECT * FROM notifications WHERE job_id=?";
        ResultSet resultSet;
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, jobUUID, Types.OTHER);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return convertResultToNotification(resultSet);
            }
            return null;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return null;
    }

    public static ArrayList<Notification> browseAllUpcomingNotifications() {
        String query = "SELECT * FROM notifications WHERE next_delivery_time IS NOT NULL AND next_delivery_time <= ?";
        ResultSet resultSet;
        ArrayList<Notification> notifications = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                notifications.add(convertResultToNotification(resultSet));
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
        }
        return notifications;
    }

    public static Notification rescheduleNotification(UUID notificationId, long time) {
        String query = "UPDATE notifications SET delivery_count=delivery_count+1, next_delivery_time=? WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            Timestamp newTimestamp = new Timestamp(System.currentTimeMillis() + time);
            statement.setTimestamp(1, newTimestamp);
            statement.setObject(2, notificationId, Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("Notification %s next_delivery_time changed to '%s'. %s rows affected", notificationId.toString(), newTimestamp, rows));
            return browseNotificationById(notificationId);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    public static Notification changeNotificationMessageId(UUID notificationId, int messageId) {
        String query = "UPDATE notifications SET message_id=? WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, messageId);
            statement.setObject(2, notificationId, Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("Notification %s message_id changed to '%s'. %s rows affected", notificationId, messageId, rows));
            return browseNotificationById(notificationId);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }

    public static Notification declineNotification(UUID notificationId) {
        String query = "UPDATE notifications SET next_delivery_time=NULL WHERE id=?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, notificationId, Types.OTHER);
            int rows = statement.executeUpdate();
            log.info(String.format("Notification %s was declined. %s rows affected", notificationId, rows));
            return browseNotificationById(notificationId);
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }


    private static Notification convertResultToNotification(ResultSet resultSet) {
        try {
            UUID id = UUID.fromString(resultSet.getObject("id").toString());
            UUID jobId = UUID.fromString(resultSet.getObject("job_id").toString());
            long userTgId = resultSet.getLong("user_tg_id");
            int messageId = resultSet.getInt("message_id");
            String message = resultSet.getString("message");
            NotificationType notificationType = NotificationType.valueOf(resultSet.getString("type"));
            int deliveryCount = resultSet.getInt("delivery_count");
            int maxDeliveryCount = resultSet.getInt("max_delivery_count");
            Timestamp nextDeliveryTime = resultSet.getTimestamp("next_delivery_time");

            Notification notification = new Notification(id,
                    jobId,
                    userTgId,
                    messageId,
                    message,
                    notificationType,
                    deliveryCount,
                    maxDeliveryCount,
                    nextDeliveryTime);
            return notification;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return null;
        }
    }


    public static boolean addEventLog(Event event) {
        String query = "INSERT INTO event_log (id, event_timestamp, type, user_tg_id, message_id, message, event)" +
                " VALUES(?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            UUID uuid = Ulid.fast().toUuid();
            statement.setObject(1, uuid, Types.OTHER);
            statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            statement.setInt(3, event.getEventType());
            statement.setLong(4, event.getChatId());
            statement.setLong(5, event.getMessageId());
            statement.setString(6, event.getMessage());
            statement.setObject(7, event.toString(), Types.OTHER);
            int rows = statement.executeUpdate();
            log.debug(String.format("New log %s added. %s rows affected", uuid, rows));
            return true;
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
            sqle.printStackTrace();
            return false;
        }
    }


    public static void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
                log.info("Connection closed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

