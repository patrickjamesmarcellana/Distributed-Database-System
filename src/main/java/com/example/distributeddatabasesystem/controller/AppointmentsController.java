package com.example.distributeddatabasesystem.controller;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.service.AppointmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/appointments")
public class AppointmentsController {
    @Autowired
    private AppointmentsService appointmentsService;

    @PostMapping("/add")
    public String add(@RequestBody Appointments appointment) {
        appointmentsService.saveAppointments(appointment);
        return "New appointment was added.";
    }
}
