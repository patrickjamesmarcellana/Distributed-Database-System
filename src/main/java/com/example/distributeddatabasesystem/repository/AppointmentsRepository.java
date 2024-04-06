package com.example.distributeddatabasesystem.repository;

import com.example.distributeddatabasesystem.model.Appointments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentsRepository extends JpaRepository<Appointments, Integer> {


}
