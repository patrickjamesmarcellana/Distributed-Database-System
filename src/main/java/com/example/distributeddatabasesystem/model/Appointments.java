package com.example.distributeddatabasesystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Appointments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String status;
    private LocalDateTime timequeued;
    private LocalDateTime queuedDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String type;
    private String virtual;

    private int px_age;
    private String px_gender;

    private String clinic_hospitalname;
    private String clinic_ishospital;
    private String clinic_city;
    private String clinic_province;
    private String clinic_regionname;

    private String doctor_mainspecialty;
    private int doctor_age;

    public Appointments() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimequeued() {
        return timequeued;
    }

    public void setTimequeued(LocalDateTime timequeued) {
        this.timequeued = timequeued;
    }

    public LocalDateTime getQueuedDate() {
        return queuedDate;
    }

    public void setQueuedDate(LocalDateTime queuedDate) {
        this.queuedDate = queuedDate;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVirtual() {
        return virtual;
    }

    public void setVirtual(String virtual) {
        this.virtual = virtual;
    }

    public int getPx_age() {
        return px_age;
    }

    public void setPx_age(int px_age) {
        this.px_age = px_age;
    }

    public String getPx_gender() {
        return px_gender;
    }

    public void setPx_gender(String px_gender) {
        this.px_gender = px_gender;
    }

    public String getClinic_hospitalname() {
        return clinic_hospitalname;
    }

    public void setClinic_hospitalname(String clinic_hospitalname) {
        this.clinic_hospitalname = clinic_hospitalname;
    }

    public String getClinic_ishospital() {
        return clinic_ishospital;
    }

    public void setClinic_ishospital(String clinic_ishospital) {
        this.clinic_ishospital = clinic_ishospital;
    }

    public String getClinic_city() {
        return clinic_city;
    }

    public void setClinic_city(String clinic_city) {
        this.clinic_city = clinic_city;
    }

    public String getClinic_province() {
        return clinic_province;
    }

    public void setClinic_province(String clinic_province) {
        this.clinic_province = clinic_province;
    }

    public String getClinic_regionname() {
        return clinic_regionname;
    }

    public void setClinic_regionname(String clinic_regionname) {
        this.clinic_regionname = clinic_regionname;
    }

    public String getDoctor_mainspecialty() {
        return doctor_mainspecialty;
    }

    public void setDoctor_mainspecialty(String doctor_mainspecialty) {
        this.doctor_mainspecialty = doctor_mainspecialty;
    }

    public int getDoctor_age() {
        return doctor_age;
    }

    public void setDoctor_age(int doctor_age) {
        this.doctor_age = doctor_age;
    }
}
