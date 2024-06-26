package com.example.distributeddatabasesystem.controller;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.model.Transaction;
import com.example.distributeddatabasesystem.service.AppointmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/appointments")
public class AppointmentsController {
    @Autowired
    private AppointmentsService appointmentsService;

//    @PostMapping("/add")
//    public ResponseEntity<Void> add(@RequestBody Appointments appointment) {
//        try {
//            appointmentsService.saveAppointment(appointment);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @GetMapping("/getAll")
//    public ResponseEntity<List<Appointments>> getAll() {
//        try {
//            List<Appointments> appointments = appointmentsService.getAllAppointments();
//            return new ResponseEntity<>(appointments, HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Appointments> get(@PathVariable Integer id) {
//        try {
//            Appointments appointment = appointmentsService.getAppointment(id);
//            return new ResponseEntity<>(appointment, HttpStatus.OK);
//        } catch (NoSuchElementException e) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @DeleteMapping("/delete/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Integer id) {
//        try {
//            appointmentsService.delete(id);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @PutMapping("/update/{id}")
//    public ResponseEntity<Appointments> update(@RequestBody Appointments appointment, @PathVariable Integer id) {
//        try {
//            Appointments existingAppointment = appointmentsService.getAppointment(id);
//            appointmentsService.saveAppointment(appointment);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (NoSuchElementException e) {
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @GetMapping("/read")
    public ResponseEntity<Appointments> read(@RequestParam String node, @RequestParam String isolationLevel, @RequestParam String transaction, @RequestParam String operation, @RequestParam String id, @RequestParam String sleepOrNot, @RequestParam String commitOrRollback) throws SQLException {
        try {
            Transaction newTransaction = new Transaction(node, isolationLevel, transaction, operation, Integer.parseInt(id), sleepOrNot, commitOrRollback);
            Appointments result = appointmentsService.read(newTransaction);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PatchMapping("/update")
    public ResponseEntity<Appointments> update(@RequestBody Transaction transaction) {
        try {
            Appointments result = appointmentsService.update(transaction);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody Transaction transaction) {
        try {
            appointmentsService.delete(transaction);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<Appointments>> findAll(@RequestParam String node, @RequestParam String transaction, @RequestParam String operation) {
        try {
            List<Appointments> appointments = appointmentsService.findAllAppointments(node, transaction, operation);
            return new ResponseEntity<>(appointments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/reset")
    public ResponseEntity<List<Appointments>> reset() {
        try {
            appointmentsService.resetSlaves();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
