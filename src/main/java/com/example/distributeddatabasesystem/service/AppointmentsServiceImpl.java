package com.example.distributeddatabasesystem.service;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.model.Transaction;
import com.example.distributeddatabasesystem.repository.AppointmentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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


@EnableAutoConfiguration
@Service
public class AppointmentsServiceImpl implements AppointmentsService {

    @Autowired
    private AppointmentsRepository appointmentsRepository;

    @Autowired
    @Qualifier("node1JdbcTemplate")
    JdbcTemplate node1JdbcTemplate;

    @Autowired
    @Qualifier("node2JdbcTemplate")
    JdbcTemplate node2JdbcTemplate;

    @Autowired
    @Qualifier("node3JdbcTemplate")
    JdbcTemplate node3JdbcTemplate;

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
    public Appointments read(Transaction data) throws SQLException {
        // determine which node to use in getConnection
        Connection connection = getConnection(data.getNode());

        // set transaction isolation level
        setTransactionIsolationLevel(connection, data.getIsolationLevel());

        // start transaction
        connection.setAutoCommit(false);

        // Read
        PreparedStatement query = connection.prepareStatement(data.getTransaction());
        query.setInt(1, data.getId());
        System.out.println(query.toString());
        ResultSet queryResult = query.executeQuery();
        queryResult.next();

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
        return appointment;
    }

    // nodePort = {20189, 20190, 20191}
    public Connection getConnection(String nodePort) throws SQLException {
        // TODO: Handle Global Recovery here
        switch(nodePort) {
            case "20189" -> {
                return node1JdbcTemplate.getDataSource().getConnection();
            }
            case "20190" -> {
                return node2JdbcTemplate.getDataSource().getConnection();
            }
            default -> { // 20191
                return node3JdbcTemplate.getDataSource().getConnection();
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

        return appointment;
    }
}
