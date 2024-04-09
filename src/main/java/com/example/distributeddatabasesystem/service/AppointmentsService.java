package com.example.distributeddatabasesystem.service;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.model.Transaction;

import java.util.List;

public interface AppointmentsService {
//    public Appointments saveAppointment(Appointments appointment);
//    public Appointments getAppointment(Integer id);
//    public List<Appointments> getAllAppointments();
//    public void delete(Integer id);
    public Appointments read(Transaction data);
}
