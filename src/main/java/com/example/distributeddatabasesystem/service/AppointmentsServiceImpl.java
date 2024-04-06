package com.example.distributeddatabasesystem.service;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.repository.AppointmentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentsServiceImpl implements AppointmentsService {

    @Autowired
    private AppointmentsRepository appointmentsRepository;

    @Override
    public Appointments saveAppointment(Appointments appointment) {
        return appointmentsRepository.save(appointment);
    }

    @Override
    public Appointments getAppointment(Integer id) {
        return appointmentsRepository.findById(id).get();
    }

    @Override
    public List<Appointments> getAllAppointments() {
        return appointmentsRepository.findAll();
    }

    @Override
    public void delete(Integer id) {
        appointmentsRepository.deleteById(id);
    }
}
