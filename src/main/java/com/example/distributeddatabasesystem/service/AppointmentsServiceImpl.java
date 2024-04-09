package com.example.distributeddatabasesystem.service;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.lang.Math.log;
import static java.lang.Math.max;


@EnableAutoConfiguration
@Service
public class AppointmentsServiceImpl implements AppointmentsService {
    @Autowired
    @Qualifier("node1JdbcTemplate")
    JdbcTemplate node1JdbcTemplate;

    @Autowired
    @Qualifier("node2JdbcTemplate")
    JdbcTemplate node2JdbcTemplate;

    @Autowired
    @Qualifier("node3JdbcTemplate")
    JdbcTemplate node3JdbcTemplate;

    private void replicationSubtask(JdbcTemplate destination, HashSet<String> destionationIslands, JdbcTemplate source1, JdbcTemplate source2) {
        Connection destinationConnection = null;
        try {
            destinationConnection = Objects.requireNonNull(destination.getDataSource()).getConnection();
            destinationConnection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            destinationConnection.setAutoCommit(false);

            System.out.println("Connected to " + destinationConnection.getMetaData().getURL());
        } catch (SQLException e) {
            System.out.println("Failed to connect to destination node");
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if(destinationConnection != null) {
            ArrayList<PreparedStatement> updateLastModifiedOfSources = new ArrayList<>();
            ArrayList<JdbcTemplate> sources = new ArrayList<>(List.of(source1, source2));
            HashMap<Integer, Appointments> masterAppointments = new HashMap<>();
            for(JdbcTemplate source : sources) {
                try {
                    Connection sourceConnection = Objects.requireNonNull(source.getDataSource()).getConnection();
                    sourceConnection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

                    PreparedStatement lastReadTimeStampQuery = destinationConnection.prepareStatement("SELECT last_modified FROM mco2.node_params WHERE url = ?;");
                    lastReadTimeStampQuery.setString(1, sourceConnection.getMetaData().getURL());
                    ResultSet lastReadTimestampQueryResult = lastReadTimeStampQuery.executeQuery();
                    long lastReadEventId = lastReadTimestampQueryResult.next() ? lastReadTimestampQueryResult.getLong("last_modified") : -1;
                    System.out.println("Latest data from " + sourceConnection.getMetaData().getURL() + " was from " + lastReadEventId);

                    ResultSet serverMaxEventIdQuery = sourceConnection.prepareStatement("SELECT MAX(event_id) FROM appointments_log").executeQuery();
                    long serverMaxEventId = serverMaxEventIdQuery.next() ? serverMaxEventIdQuery.getLong(1) : -1;
                    if(serverMaxEventIdQuery.wasNull()) { // serverMaxEventId is set to 0 if the result is actually NULL
                        serverMaxEventId = -1;
                    }

                    PreparedStatement otherNodeQuery = sourceConnection.prepareStatement("SELECT * FROM appointments WHERE id IN (SELECT DISTINCT appointment_id FROM appointments_log WHERE ? < event_id AND event_id <= ?);"); // island not included (to detect island changes)
                    otherNodeQuery.setObject(1, lastReadEventId);
                    otherNodeQuery.setObject(2, serverMaxEventId);

                    ResultSet otherNodeResults = otherNodeQuery.executeQuery();
                    while(otherNodeResults.next()) {
                        Appointments appointment = extractResult(otherNodeResults);
                        Integer id = appointment.getId();
                        System.out.println("Found data of id=" + id);

                        if(!masterAppointments.containsKey(id)) {
                            masterAppointments.put(id, appointment);
                        } else {
                            // prioritize later appointment
                            if(appointment.getLast_modified().isAfter(masterAppointments.get(id).getLast_modified())) {
                                masterAppointments.put(id, appointment);
                            }
                        }
                    }

                    // if there is new data, update the last modified timestamp of the destination node
                    PreparedStatement updateLastModified = destinationConnection.prepareStatement("INSERT INTO mco2.node_params (url, last_modified) VALUES(?, ?) ON DUPLICATE KEY UPDATE last_modified = VALUES(last_modified);");
                    updateLastModified.setString(1, sourceConnection.getMetaData().getURL());
                    updateLastModified.setObject(2, serverMaxEventId);
                    updateLastModifiedOfSources.add(updateLastModified);

                    sourceConnection.close();
                } catch (SQLException | NullPointerException e) {
                    e.printStackTrace();
                }
            }

            // load to destination
            try {
                System.out.println("Writing to " + destinationConnection.getMetaData().getURL());
                for(Appointments appointment : masterAppointments.values()) {
                    if(destionationIslands.contains(appointment.getIsland())) {
                        // save appointment
                        System.out.println("Writing data of id=" + appointment.getId());
                        PreparedStatement upsertStatement = upsertAppointment(destinationConnection, appointment, false /* do not update timestamp */);
                        upsertStatement.executeUpdate();
                    } else {
                        // delete appointment if exists (used on slave nodes)
                        PreparedStatement deleteStatement = destinationConnection.prepareStatement("DELETE FROM appointments WHERE id = ?;");
                        deleteStatement.setInt(1, appointment.getId());
                        deleteStatement.executeUpdate();
                    }
                }

                for(PreparedStatement updateLastModified : updateLastModifiedOfSources) {
                    updateLastModified.executeUpdate();
                }

                destinationConnection.commit();
                destinationConnection.close();

            } catch (SQLException e) {
                System.out.println("Failed to write to destination node");
                e.printStackTrace();
            }
        }



    }

    @Scheduled(fixedDelay=5000)
    public void replicationTask() {
        System.out.println("Starting replication task #1 - load data to slave");
        replicationSubtask(node2JdbcTemplate, new HashSet<>(List.of("Luzon")), node1JdbcTemplate, node3JdbcTemplate);
        replicationSubtask(node3JdbcTemplate, new HashSet<>(List.of("Visayas", "Mindanao")), node1JdbcTemplate, node2JdbcTemplate);
        replicationSubtask(node1JdbcTemplate, new HashSet<>(List.of("Luzon", "Visayas", "Mindanao")), node2JdbcTemplate, node3JdbcTemplate);
    }

//    @Override
//    public Appointments saveAppointment(Appointments appointment) {
//        try {
//            node1JdbcTemplate.update("INSERT INTO appointments (status, timequeued, queuedate, starttime, endtime, appttype, isvirtual, px_age, px_gender, clinic_hospitalname, clinic_ishospital, clinic_city, clinic_province, clinic_regionname, doctor_mainspecialty, doctor_age, island) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
//                    appointment.getStatus(),
//                    appointment.getTimequeued(),
//                    appointment.getQueuedate(),
//                    appointment.getStarttime(),
//                    appointment.getEndtime(),
//                    appointment.getAppttype(),
//                    appointment.getIsvirtual(),
//                    appointment.getPx_age(),
//                    appointment.getPx_gender(),
//                    appointment.getClinic_hospitalname(),
//                    appointment.getClinic_ishospital(),
//                    appointment.getClinic_city(),
//                    appointment.getClinic_province(),
//                    appointment.getClinic_regionname(),
//                    appointment.getDoctor_mainspecialty(),
//                    appointment.getDoctor_age(),
//                    appointment.getIsland());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
////        return appointmentsRepository.save(appointment);
//        return appointment;
//    }
//
//    @Override
//    public Appointments getAppointment(Integer id) {
//        return appointmentsRepository.findById(id).get();
//    }
//
//    @Override
//    public List<Appointments> getAllAppointments() {
//        return appointmentsRepository.findAll();
//    }
//
//    @Override
//    public void delete(Integer id) {
//        appointmentsRepository.deleteById(id);
//    }

    @Override
    public Appointments read(Transaction data) throws SQLException, InterruptedException {
        // determine which node to use in getConnection
        Connection connection = getConnection(data.getNode(), data.getId());

        // set transaction isolation level
        setTransactionIsolationLevel(connection, data.getIsolationLevel());

        // start transaction
        connection.setAutoCommit(false);

        // Read
        PreparedStatement query = connection.prepareStatement(data.getTransaction());
        query.setInt(1, data.getId());
        ResultSet queryResult = query.executeQuery();
        queryResult.next();

        // Sleep or Not Sleep
        switch(data.getSleepOrNot()) {
            case "sleep" -> {
                // sleep in Java instead of SQL
                Thread.sleep(5000);
            } default -> { // not-sleep
                // don't do anything
            }
        }

        // Commit or Rollback
        switch(data.getCommitOrRollback()) {
            case "commit" -> {
                connection.commit();
            } default -> { // rollback
                connection.rollback();
            }
        }

        // store result
        Appointments appointment = extractResult(queryResult);
        connection.close();
        return appointment;
    }

    @Override
    public Appointments update(Transaction data) throws SQLException, InterruptedException {
        // determine which node to use in getConnection
        Connection connection = getConnection(data.getNode(), data.getId());

        // set transaction isolation level
        setTransactionIsolationLevel(connection, data.getIsolationLevel());

        // start transaction
        connection.setAutoCommit(false);

        // Update
        PreparedStatement query = connection.prepareStatement(data.getTransaction());
        query.setInt(1, data.getId());
        query.executeUpdate();

        // Sleep or Not Sleep
        switch(data.getSleepOrNot()) {
            case "sleep" -> {
                // sleep in Java instead of SQL
                Thread.sleep(5000);
            } default -> { // not-sleep
                // don't do anything
            }
        }

        // Commit or Rollback
        switch(data.getCommitOrRollback()) {
            case "commit" -> {
                connection.commit();
                // Update
                PreparedStatement logQuery = connection.prepareStatement("INSERT INTO mco2.`appointments_log` (appointment_id) VALUES (?);");
                logQuery.setInt(1, data.getId());
                logQuery.executeUpdate();
            } default -> { // rollback
                connection.rollback();
            }
        }

        // retrieve updated row
        PreparedStatement findQuery = connection.prepareStatement("SELECT * FROM appointments WHERE id = ?;");
        findQuery.setInt(1, data.getId());
        ResultSet queryResult = findQuery.executeQuery();
        queryResult.next();

        // store result
        Appointments appointment = extractResult(queryResult);
        connection.close();
        return appointment;
    }

    @Override
    public void delete(Transaction data) throws SQLException, InterruptedException {
        // determine which node to use in getConnection
        Connection connection = getConnection(data.getNode(), data.getId());

        // set transaction isolation level
        setTransactionIsolationLevel(connection, data.getIsolationLevel());

        // start transaction
        connection.setAutoCommit(false);

        // Delete
        PreparedStatement query = connection.prepareStatement(data.getTransaction());
        query.setInt(1, data.getId());
        query.executeUpdate();

        // Sleep or Not Sleep
        switch(data.getSleepOrNot()) {
            case "sleep" -> {
                // sleep in Java instead of SQL
                Thread.sleep(5000);
            } default -> { // not-sleep
                // don't do anything
            }
        }

        // Commit or Rollback
        switch(data.getCommitOrRollback()) {
            case "commit" -> {
                connection.commit();
                PreparedStatement logQuery = connection.prepareStatement("INSERT INTO mco2.`appointments_log` (appointment_id) VALUES (?);");
                logQuery.setInt(1, data.getId());
                logQuery.executeUpdate();
            } default -> { // rollback
                connection.rollback();
            }
        }
        connection.close();
    }

    // nodePort = {20189, 20190, 20191}
    public Connection getConnection(String nodePort, int id) throws SQLException {
        // TODO: Handle Global Recovery here
        switch(nodePort) {
            case "20189" -> {
                Connection connection = node1JdbcTemplate.getDataSource().getConnection();
                try {
                    // check first if master/chosen server is up
                    // unhandled condition: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    queryResult.next();
                    return connection;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // it means the initial connection failed -> switch to another connection

                // check node slave 1 if id of island is here
                try {
                    connection = node2JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    boolean isIslandHere = queryResult.next();
                    if (isIslandHere) {
                        return connection;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // (if the other two are down) or (if the master is down and slave 1 does not contain island), choose server 3
                // unhandled condition when both servers are down: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                connection = node3JdbcTemplate.getDataSource().getConnection();
                return connection;
            }
            case "20190" -> {
                Connection connection = node2JdbcTemplate.getDataSource().getConnection();
                try {
                    // check first if chosen server is up
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    // if server is up, check if island is here
                    boolean isIslandHere = queryResult.next();
                    if (isIslandHere) {
                        return connection;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // switch to another connection if initial slave connection failed/does not contain island

                // check master if it is up; assumes that master contains all data
                try {
                    connection = node1JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    queryResult.next();
                    return connection;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // (if the other two are down) or (if the master is down and slave 1 does not contain island), choose slave 2
                // unhandled condition when both servers are down: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                connection = node3JdbcTemplate.getDataSource().getConnection();
                return connection;
            }
            default -> { // 20191
                Connection connection = node3JdbcTemplate.getDataSource().getConnection();
                try {
                    // check first if chosen server is up
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    // if server is up, check if island is here
                    boolean isIslandHere = queryResult.next();
                    if (isIslandHere) {
                        return connection;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // switch to another connection if initial slave connection failed/does not contain island

                // check master if it is up; assumes that master contains all data
                try {
                    connection = node1JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    queryResult.next();
                    return connection;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // (if the other two are down) or (if the master is down and slave 2 does not contain island), choose slave 1
                // unhandled condition when both servers are down: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                connection = node2JdbcTemplate.getDataSource().getConnection();
                return connection;
            }
        }
    }

    public void setTransactionIsolationLevel(Connection connection, String isolationLevel) throws SQLException {
        switch (isolationLevel) {
            case "READ UNCOMMITTED" -> {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            }
            case "READ COMMITTED" -> {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
            case "REPEATABLE READ" -> {
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            }
            default -> { // SERIALIZABLE
                connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            }
        }
    }

    public Appointments extractResult(ResultSet queryResult) throws SQLException {
        Appointments appointment = new Appointments();
        appointment.setId(queryResult.getInt("id"));
        appointment.setStatus(queryResult.getString("status"));
        appointment.setTimequeued(queryResult.getObject("timequeued", LocalDateTime.class));
        appointment.setQueuedate(queryResult.getObject("queuedate", LocalDateTime.class));
        appointment.setStarttime(queryResult.getObject("starttime", LocalDateTime.class));
        appointment.setEndtime(queryResult.getObject("endtime", LocalDateTime.class));
        appointment.setAppttype(queryResult.getString("appttype"));
        appointment.setIsvirtual(queryResult.getString("isvirtual"));
        appointment.setPx_age(queryResult.getInt("px_age"));
        appointment.setPx_gender(queryResult.getString("px_gender"));
        appointment.setClinic_hospitalname(queryResult.getString("clinic_hospitalname"));
        appointment.setClinic_ishospital(queryResult.getString("clinic_ishospital"));
        appointment.setClinic_city(queryResult.getString("clinic_city"));
        appointment.setClinic_province(queryResult.getString("clinic_province"));
        appointment.setClinic_regionname(queryResult.getString("clinic_regionname"));
        appointment.setDoctor_mainspecialty(queryResult.getString("doctor_mainspecialty"));
        appointment.setDoctor_age(queryResult.getInt("doctor_age"));
        appointment.setIsland(queryResult.getString("island"));
        appointment.setLast_modified(queryResult.getObject("last_modified", LocalDateTime.class));
        appointment.setModified_by(queryResult.getString("modified_by"));

        return appointment;
    }

    public PreparedStatement deleteAppointment(Connection connection, Appointments appointments, boolean updateLastModifiedTimestamp) throws SQLException {
        PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM appointments WHERE id = ?;");
        deleteStatement.setInt(1, appointments.getId());
        return deleteStatement;
    }

    public PreparedStatement upsertAppointment(Connection connection, Appointments appointments, boolean updateLastModifiedTimestamp) throws SQLException {
        PreparedStatement upsertStatement = connection.prepareStatement("INSERT INTO appointments (id, status, timequeued, queuedate, starttime, endtime, appttype, isvirtual, px_age, px_gender, clinic_hospitalname, clinic_ishospital, clinic_city, clinic_province, clinic_regionname, doctor_mainspecialty, doctor_age, island, modified_by, last_modified) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + (updateLastModifiedTimestamp ? "UNIX_TIMESTAMP()" : "?") + ")" +
                    "ON DUPLICATE KEY UPDATE " +
                    "id = VALUES(id), " +
                    "status = VALUES(status), " + // good thing regex exists
                    "timequeued = VALUES(timequeued), " +
                    "queuedate = VALUES(queuedate), " +
                    "starttime = VALUES(starttime), " +
                    "endtime = VALUES(endtime), " +
                    "appttype = VALUES(appttype), " +
                    "isvirtual = VALUES(isvirtual), " +
                    "px_age = VALUES(px_age), " +
                    "px_gender = VALUES(px_gender), " +
                    "clinic_hospitalname = VALUES(clinic_hospitalname), " +
                    "clinic_ishospital = VALUES(clinic_ishospital), " +
                    "clinic_city = VALUES(clinic_city), " +
                    "clinic_province = VALUES(clinic_province), " +
                    "clinic_regionname = VALUES(clinic_regionname), " +
                    "doctor_mainspecialty = VALUES(doctor_mainspecialty), " +
                    "doctor_age = VALUES(doctor_age), " +
                    "island = VALUES(island), " +
                    "modified_by = VALUES(modified_by), " +
                    "last_modified = " + (updateLastModifiedTimestamp ? "UNIX_TIMESTAMP()" : "VALUES(last_modified)")
                    );

        upsertStatement.setInt(1, appointments.getId());
        upsertStatement.setString(2, appointments.getStatus());
        upsertStatement.setObject(3, appointments.getTimequeued());
        upsertStatement.setObject(4, appointments.getQueuedate());
        upsertStatement.setObject(5, appointments.getStarttime());
        upsertStatement.setObject(6, appointments.getEndtime());
        upsertStatement.setString(7, appointments.getAppttype());
        upsertStatement.setString(8, appointments.getIsvirtual());
        upsertStatement.setInt(9, appointments.getPx_age());
        upsertStatement.setString(10, appointments.getPx_gender());
        upsertStatement.setString(11, appointments.getClinic_hospitalname());
        upsertStatement.setString(12, appointments.getClinic_ishospital());
        upsertStatement.setString(13, appointments.getClinic_city());
        upsertStatement.setString(14, appointments.getClinic_province());
        upsertStatement.setString(15, appointments.getClinic_regionname());
        upsertStatement.setString(16, appointments.getDoctor_mainspecialty());
        upsertStatement.setInt(17, appointments.getDoctor_age());
        upsertStatement.setString(18, appointments.getIsland());
        upsertStatement.setString(19, appointments.getModified_by());

        if(!updateLastModifiedTimestamp) {
            upsertStatement.setObject(20, appointments.getLast_modified());
        }

        return upsertStatement;
    }
}