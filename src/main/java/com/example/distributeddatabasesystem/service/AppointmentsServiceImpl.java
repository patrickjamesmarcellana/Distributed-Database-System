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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@EnableAutoConfiguration
@Transactional(propagation= Propagation.REQUIRED, isolation= Isolation.READ_COMMITTED)
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
    public Appointments read(Transaction data) {
        System.out.println(data.getNode());
        if(data.getOperation().equals("Read")) {
            // use jdbcTemplate.query() only
            switch (data.getNode()) {
                case "20189" -> {
                    System.out.println("Reached the correct node");
                    return node1JdbcTemplate.queryForObject(data.getTransaction(),
                                            new Object[]{data.getId()},
                                            new BeanPropertyRowMapper<>(Appointments.class));
                }
                case "20190" -> {
                    return node2JdbcTemplate.queryForObject(data.getTransaction(),
                                            new Object[]{data.getId()},
                                            new BeanPropertyRowMapper<>(Appointments.class));

                }
                case "20191" -> {
                    return node3JdbcTemplate.queryForObject(data.getTransaction(),
                                            new Object[]{data.getId()},
                                            new BeanPropertyRowMapper<>(Appointments.class));
                }
            }
        } else {
            // use jdbcTemplate.execute() then jdbcTemplate.query() to get results
            return null;
        }
        return null;

    }
}
