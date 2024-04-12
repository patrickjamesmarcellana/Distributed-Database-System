package com.example.distributeddatabasesystem.service;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.model.Transaction;

import java.sql.SQLException;
import java.util.List;

public interface AppointmentsService {
//    public Appointments saveAppointment(Appointments appointment);
//    public Appointments getAppointment(Integer id);
//    public List<Appointments> getAllAppointments();
//    public void delete(Integer id);
    public Appointments read(Transaction data) throws SQLException, InterruptedException;
    public Appointments update(Transaction data) throws SQLException, InterruptedException;
    public void delete (Transaction data) throws SQLException, InterruptedException;
    public List<Appointments> findAllAppointments(String node, String transaction, String operation) throws SQLException;
    public void resetSlaves();
}
