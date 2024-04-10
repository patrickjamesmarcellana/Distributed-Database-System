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
            HashSet<Integer> toDelete = new HashSet<>();
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

                    // we will only delete rows that have:
                    //   1. that have an is_delete entry in the log, indicating that a delete operation was performed and:
                    //   2. does not contain any succeeding operations in the log that causes it to rise from the dead (e.g. create)
                    PreparedStatement deletedQuery = sourceConnection.prepareStatement("SELECT DISTINCT appointment_id FROM appointments_log a WHERE ? < a.event_id AND a.event_id <= ? AND a.is_delete <=> 1 AND a.event_id NOT IN (SELECT DISTINCT appointment_id FROM appointments_log a_next WHERE a.event_id < a_next.event_id AND a_next.event_id <= ? AND NOT(a.is_delete <=> 1));");
                    deletedQuery.setObject(1, lastReadEventId);
                    deletedQuery.setObject(2, serverMaxEventId);
                    deletedQuery.setObject(3, serverMaxEventId);
                    ResultSet deletedRowsResults = deletedQuery.executeQuery();
                    while(deletedRowsResults.next()) {
                        toDelete.add(deletedRowsResults.getInt(1));
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
                    // prevent any insertion of items that are marked for deletion
                    if(toDelete.contains(appointment.getId())) {
                        continue;
                    }

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
                for(int id : toDelete) {
                    PreparedStatement deleteStatement = destinationConnection.prepareStatement("DELETE FROM appointments WHERE id = ?;");
                    deleteStatement.setInt(1, id);
                    deleteStatement.executeUpdate();
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

     @Scheduled(fixedDelay=60000)
    public synchronized void replicationTask() {
        System.out.println("Starting replication task #1 - load data to slave");
        replicationSubtask(node2JdbcTemplate, new HashSet<>(List.of("Luzon")), node1JdbcTemplate, node3JdbcTemplate);
        replicationSubtask(node3JdbcTemplate, new HashSet<>(List.of("Visayas", "Mindanao")), node1JdbcTemplate, node2JdbcTemplate);
        replicationSubtask(node1JdbcTemplate, new HashSet<>(List.of("Luzon", "Visayas", "Mindanao")), node2JdbcTemplate, node3JdbcTemplate);
    }

    boolean wasDown = true;
    private void ensureConsistency() {
        if(wasDown) {
            replicationTask();
            wasDown = false;
        }
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
            case "sleep-before" -> {
                // sleep in Java instead of SQL
                Thread.sleep(5000);
                // Commit or Rollback
                switch(data.getCommitOrRollback()) {
                    case "commit" -> {
                        connection.commit();
                    } default -> { // rollback
                        connection.rollback();
                    }
                }
            }
            case "sleep-after" -> {
                // Commit or Rollback
                switch(data.getCommitOrRollback()) {
                    case "commit" -> {
                        connection.commit();
                    } default -> { // rollback
                        connection.rollback();
                    }
                }
                Thread.sleep(5000);
            }
            default -> { // not-sleep
                switch(data.getCommitOrRollback()) {
                    case "commit" -> {
                        connection.commit();
                    } default -> { // rollback
                        connection.rollback();
                    }
                }
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

        Connection node2Connection = null;
        Connection node3Connection = null;

        if(connection.getMetaData().getURL().contains("20189")) { // hack to check if using the master node
            try {
                node2Connection = node2JdbcTemplate.getDataSource().getConnection();
                node2Connection.setAutoCommit(false);
            } catch (SQLException e) {}
            try {
                node3Connection = node3JdbcTemplate.getDataSource().getConnection();
                node3Connection.setAutoCommit(false);
            } catch (SQLException e) {}
        }

        // set transaction isolation level
        setTransactionIsolationLevel(connection, data.getIsolationLevel());

        // start transaction
        connection.setAutoCommit(false);

        // get initial island and prepare locks
        PreparedStatement islandQuery = connection.prepareStatement("SELECT * FROM appointments WHERE id = ? FOR UPDATE;");
        islandQuery.setInt(1, data.getId());
        ResultSet islandQueryResult = islandQuery.executeQuery();
        islandQueryResult.next();
        String initialIsland = islandQueryResult.getString("island");

        try {
            PreparedStatement lockStatement = node2Connection.prepareStatement("SELECT * FROM appointments WHERE id = ? FOR UPDATE;");
            lockStatement.setInt(1, data.getId());
            lockStatement.executeQuery();
        } catch (SQLException e) {
            // consider node as down if failed to obtain lock
            e.printStackTrace();
            node2Connection = null;
        }
        try {
            PreparedStatement lockStatement = node3Connection.prepareStatement("SELECT * FROM appointments WHERE id = ? FOR UPDATE;");
            lockStatement.setInt(1, data.getId());
            lockStatement.executeQuery();
        } catch (SQLException e) {
            // consider node as down if failed to obtain lock
            e.printStackTrace();
            node3Connection = null;
        }

        // Update
        PreparedStatement query = connection.prepareStatement(data.getTransaction());
        query.setInt(1, data.getId());
        query.executeUpdate();

        PreparedStatement logQuery = connection.prepareStatement("INSERT INTO mco2.`appointments_log` (appointment_id) VALUES (?);");
        logQuery.setInt(1, data.getId());
        logQuery.executeUpdate();

        if(data.getSleepOrNot().equals("sleep-before")) {
            // sleep in Java instead of SQL
            Thread.sleep(5000);
        }
        switch(data.getCommitOrRollback()) {
            case "commit" -> {
                connection.commit();
                if(data.getSleepOrNot().equals("sleep-after")) {
                    // sleep in Java instead of SQL
                    Thread.sleep(8000);
                }

                PreparedStatement islandQuery2 = connection.prepareStatement("SELECT * FROM appointments WHERE id = ?;");
                islandQuery2.setInt(1, data.getId());
                ResultSet islandQuery2Result = islandQuery.executeQuery();
                islandQuery2Result.next();
                Appointments newIslandData = extractResult(islandQuery2Result);
                String finalIsland = islandQuery2Result.getString("island");

                System.out.println(initialIsland);
                System.out.println(finalIsland);

                Connection deleteFrom = null, addTo = null;
                if(!initialIsland.equals(finalIsland)) {
                    if(initialIsland.equals("Luzon")) {
                        deleteFrom = node2Connection;
                        addTo = node3Connection;

                    } else {
                        deleteFrom = node3Connection;
                        addTo = node2Connection;
                    }
                } else {
                    if(initialIsland.equals("Luzon")) {
                        deleteFrom = node3Connection;
                        addTo = node2Connection;

                    } else {
                        deleteFrom = node2Connection;
                        addTo = node3Connection;
                    }
                }

                // delete data from initial slave
                try {
                    PreparedStatement deleteIslandQuery = deleteFrom.prepareStatement("DELETE FROM appointments WHERE id = ?");
                    deleteIslandQuery.setInt(1, data.getId());
                    deleteIslandQuery.executeUpdate();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }

                // add data to new slave
                try {
                    PreparedStatement insertIslandQuery = upsertAppointment(addTo, newIslandData, false);
                    insertIslandQuery.executeUpdate();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }

                // commit changes to appointments table of all nodes
                try {
                    node2Connection.commit();
                } catch (Exception ignored) {}
                try {
                    node3Connection.commit();
                } catch (Exception ignored) {}
            } default -> { // rollback
                if(data.getSleepOrNot().equals("sleep-after")) {
                    // sleep in Java instead of SQL
                    Thread.sleep(8000);
                }
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
        try {
            node2Connection.close();
        } catch (Exception ignored) {}
        try {
            node3Connection.close();
        } catch (Exception ignored) {}
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
            case "sleep-before" -> {
                // sleep in Java instead of SQL
                Thread.sleep(5000);
                // Commit or Rollback
                switch(data.getCommitOrRollback()) {
                    case "commit" -> {
                        connection.commit();    // commit changes to appointments table of selected node
                        tryUpdatingSlaveNodes(data.getTransaction(), data.getId(), data.getIsolationLevel());

                        PreparedStatement logQuery = connection.prepareStatement("INSERT INTO mco2.`appointments_log` (appointment_id, is_delete) VALUES (?, 1);");
                        logQuery.setInt(1, data.getId());
                        logQuery.executeUpdate();
                        connection.commit();   // commit changes to appointment_logs table
                    } default -> { // rollback
                        connection.rollback();
                    }
                }
            }
            case "sleep-after" -> {
                // Commit or Rollback
                switch(data.getCommitOrRollback()) {
                    case "commit" -> {
                        connection.commit();    // commit changes to appointments table of selected node
                        Thread.sleep(8000);

                        tryUpdatingSlaveNodes(data.getTransaction(), data.getId(), data.getIsolationLevel());

                        PreparedStatement logQuery = connection.prepareStatement("INSERT INTO mco2.`appointments_log` (appointment_id, is_delete) VALUES (?, 1);");
                        logQuery.setInt(1, data.getId());
                        logQuery.executeUpdate();
                        connection.commit();    // commit changes to appointment_logs table
                    } default -> { // rollback
                        connection.rollback();
                        Thread.sleep(5000);
                    }
                }

            }
            default -> { // not-sleep
                switch(data.getCommitOrRollback()) {
                    case "commit" -> {
                        connection.commit();    // commit changes to appointments table of selected node
                        tryUpdatingSlaveNodes(data.getTransaction(), data.getId(), data.getIsolationLevel());

                        PreparedStatement logQuery = connection.prepareStatement("INSERT INTO mco2.`appointments_log` (appointment_id, is_delete) VALUES (?, 1);");
                        logQuery.setInt(1, data.getId());
                        logQuery.executeUpdate();
                        connection.commit();   // commit changes to appointment_logs table
                    } default -> { // rollback
                        connection.rollback();
                    }
                }
            }
        }

        connection.close();
    }

    public List<Appointments> findAllAppointments(String node, String transaction, String operation) throws SQLException {
        // special selection of connection -> no need for island, just selected server/node
        Connection connection;
        switch(node) {
            case "20189" -> {
                connection = node1JdbcTemplate.getDataSource().getConnection();
            }
            case "20190" -> {
                connection = node2JdbcTemplate.getDataSource().getConnection();
            }
            default -> { //20191
                connection = node3JdbcTemplate.getDataSource().getConnection();
            }
        }

        // find all appointments
        PreparedStatement query = connection.prepareStatement(transaction);
        ResultSet queryResult = query.executeQuery();
        List<Appointments> appointments = new ArrayList<>();
        while (queryResult.next()) {
            // store result
            Appointments appointment = extractResult(queryResult);
            appointments.add(appointment);
        }

        connection.close();
        return appointments;
    }

    // nodePort = {20189, 20190, 20191}
    public Connection getConnection(String nodePort, int id) throws SQLException {
        // ensure consistency of all nodes
        ensureConsistency();

        switch(nodePort) {
            case "20189" -> {

                try {
                    // check first if master/chosen server is up
                    // unhandled condition: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                    Connection connection = node1JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    queryResult.next();
                    return connection;
                } catch (Exception e) {
                    wasDown = true;
                    e.printStackTrace();
                }

                // it means the initial connection failed -> switch to another connection

                // check node slave 1 if id of island is here
                try {
                    Connection connection = node2JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    boolean isIslandHere = queryResult.next();
                    if (isIslandHere) {
                        return connection;
                    }
                } catch (Exception e) {
                    wasDown = true;
                    e.printStackTrace();
                }

                // (if the other two are down) or (if the master is down and slave 1 does not contain island), choose server 3
                // unhandled condition when both servers are down: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                Connection connection = node3JdbcTemplate.getDataSource().getConnection();
                return connection;
            }
            case "20190" -> {

                try {
                    // check first if chosen server is up
                    Connection connection = node2JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    // if server is up, check if island is here
                    boolean isIslandHere = queryResult.next();
                    if (isIslandHere) {
                        return connection;
                    }
                } catch (Exception e) {
                    wasDown = true;
                    e.printStackTrace();
                }

                // switch to another connection if initial slave connection failed/does not contain island

                // check master if it is up; assumes that master contains all data
                try {
                    Connection connection = node1JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    queryResult.next();
                    return connection;
                } catch (Exception e) {
                    wasDown = true;
                    e.printStackTrace();
                }

                // (if the other two are down) or (if the master is down and slave 1 does not contain island), choose slave 2
                // unhandled condition when both servers are down: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                Connection connection = node3JdbcTemplate.getDataSource().getConnection();
                return connection;
            }
            default -> { // 20191

                try {
                    // check first if chosen server is up
                    Connection connection = node3JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    // if server is up, check if island is here
                    boolean isIslandHere = queryResult.next();
                    if (isIslandHere) {
                        return connection;
                    }
                } catch (Exception e) {
                    wasDown = true;
                    e.printStackTrace();
                }

                // switch to another connection if initial slave connection failed/does not contain island

                // check master if it is up; assumes that master contains all data
                try {
                    Connection connection = node1JdbcTemplate.getDataSource().getConnection();
                    PreparedStatement findQuery = connection.prepareStatement("SELECT island FROM appointments WHERE id = ?;");
                    findQuery.setInt(1, id);
                    ResultSet queryResult = findQuery.executeQuery();
                    queryResult.next();
                    return connection;
                } catch (Exception e) {
                    wasDown = true;
                    e.printStackTrace();
                }

                // (if the other two are down) or (if the master is down and slave 2 does not contain island), choose slave 1
                // unhandled condition when both servers are down: if chosen server is a mismatch (doesn't contain id), assumption that entered id and node are always compatible
                Connection connection = node2JdbcTemplate.getDataSource().getConnection();
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

    public void tryUpdatingSlaveNodes(String transaction, int id, String isolationLevel) {
        // try to update slave nodes directly for replication
        try {
            Connection slave1Connection = node2JdbcTemplate.getDataSource().getConnection();
            // set transaction isolation level
            setTransactionIsolationLevel(slave1Connection, isolationLevel);
            // start transaction
            slave1Connection.setAutoCommit(false);
            // Update
            PreparedStatement slave1Query = slave1Connection.prepareStatement(transaction);
            slave1Query.setInt(1, id);
            slave1Query.executeUpdate();
            slave1Connection.commit();
            slave1Connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Connection slave2Connection = node3JdbcTemplate.getDataSource().getConnection();
            // set transaction isolation level
            setTransactionIsolationLevel(slave2Connection, isolationLevel);
            // start transaction
            slave2Connection.setAutoCommit(false);
            // Update
            PreparedStatement slave2Query = slave2Connection.prepareStatement(transaction);
            slave2Query.setInt(1, id);
            slave2Query.executeUpdate();
            slave2Connection.commit();
            slave2Connection.close();
        } catch (Exception e) {
            e.printStackTrace();
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

    public PreparedStatement insertAppointment(Connection connection, Appointments appointments) throws SQLException {
        PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO appointments (id, status, timequeued, queuedate, starttime, endtime, appttype, isvirtual, px_age, px_gender, clinic_hospitalname, clinic_ishospital, clinic_city, clinic_province, clinic_regionname, doctor_mainspecialty, doctor_age, island, modified_by, last_modified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        insertStatement.setInt(1, appointments.getId());
        insertStatement.setString(2, appointments.getStatus());
        insertStatement.setObject(3, appointments.getTimequeued());
        insertStatement.setObject(4, appointments.getQueuedate());
        insertStatement.setObject(5, appointments.getStarttime());
        insertStatement.setObject(6, appointments.getEndtime());
        insertStatement.setString(7, appointments.getAppttype());
        insertStatement.setString(8, appointments.getIsvirtual());
        insertStatement.setInt(9, appointments.getPx_age());
        insertStatement.setString(10, appointments.getPx_gender());
        insertStatement.setString(11, appointments.getClinic_hospitalname());
        insertStatement.setString(12, appointments.getClinic_ishospital());
        insertStatement.setString(13, appointments.getClinic_city());
        insertStatement.setString(14, appointments.getClinic_province());
        insertStatement.setString(15, appointments.getClinic_regionname());
        insertStatement.setString(16, appointments.getDoctor_mainspecialty());
        insertStatement.setInt(17, appointments.getDoctor_age());
        insertStatement.setString(18, appointments.getIsland());
        insertStatement.setString(19, appointments.getModified_by());
        insertStatement.setObject(20, appointments.getLast_modified());

        return insertStatement;
    }
}