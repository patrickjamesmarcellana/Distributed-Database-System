package com.example.distributeddatabasesystem.service;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.repository.AppointmentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppointmentsServiceImpl implements AppointmentsService {

    @Autowired
    private AppointmentsRepository appointmentsRepository;

    @Override
    public Appointments saveAppointment(Appointments appointment) {
        return appointmentsRepository.save(appointment);
    }
}
