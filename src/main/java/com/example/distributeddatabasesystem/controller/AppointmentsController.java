package com.example.distributeddatabasesystem.controller;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.service.AppointmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/appointments")
public class AppointmentsController {
    @Autowired
    private AppointmentsService appointmentsService;

    @PostMapping("/add")
    public String add(@RequestBody Appointments appointment) {
        appointmentsService.saveAppointment(appointment);
        return "New appointment was added.";
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Appointments>> getAll() {
        try {
            List<Appointments> appointments = appointmentsService.getAllAppointments();
            return new ResponseEntity<>(appointments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointments> get(@PathVariable Integer id) {
        try {
            Appointments appointment = appointmentsService.getAppointment(id);
            return new ResponseEntity<>(appointment, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        appointmentsService.delete(id);
        return "Appointment successfully deleted.";
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Appointments> update(@RequestBody Appointments appointment, @PathVariable Integer id) {
        try {
            Appointments existingAppointment = appointmentsService.getAppointment(id);
            appointmentsService.saveAppointment(appointment);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


}
