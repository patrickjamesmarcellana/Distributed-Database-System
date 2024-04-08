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
    private LocalDateTime queuedate;
    private LocalDateTime starttime;
    private LocalDateTime endtime;
    private String appttype;
    private String isvirtual;

    private int px_age;
    private String px_gender;

    private String clinic_hospitalname;
    private String clinic_ishospital;
    private String clinic_city;
    private String clinic_province;
    private String clinic_regionname;

    private String doctor_mainspecialty;
    private int doctor_age;

    private String island;

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

    public LocalDateTime getQueuedate() {
        return queuedate;
    }

    public void setQueuedate(LocalDateTime queuedate) {
        this.queuedate = queuedate;
    }

    public LocalDateTime getStarttime() {
        return starttime;
    }

    public void setStarttime(LocalDateTime starttime) {
        this.starttime = starttime;
    }

    public LocalDateTime getEndtime() {
        return endtime;
    }

    public void setEndtime(LocalDateTime endtime) {
        this.endtime = endtime;
    }

    public String getAppttype() {
        return appttype;
    }

    public void setAppttype(String appttype) {
        this.appttype = appttype;
    }

    public String getIsvirtual() {
        return isvirtual;
    }

    public void setIsvirtual(String isvirtual) {
        this.isvirtual = isvirtual;
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

    public String getIsland() {
        return island;
    }

    public void setIsland(String island) {
        this.island = island;
    }

}
