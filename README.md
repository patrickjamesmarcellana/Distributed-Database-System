# Distributed Database System

This project provides a simulation interface for a distributed database system using written SQL transactions and customizable isolation levels. Using De La Salle University's CCS Cloud Platform, the distributed database is hosted across three virtual machines with MySQL. The structure of the distributed database follows a **master-slave architecture**, which has exactly three nodes:
1. **Central Node (Master)**
     - Contains all appointment records
2. **Luzon Node (Slave 1)**
     - Contains appointment records only from Luzon
3. **Visayas/Mindanao Node (Slave 2)**
     - Contains appointment records only from Visayas and Mindanao

## Setting Up Instructions
The project is hosted on Render using a Docker image from a Docker Hub repository, which can be accessed through this [link](https://distributed-database-system-latest.onrender.com/). However, it can also be launched locally by following these instructions:
1. Clone the repository on your local machine.
2. Create an `application.properties` file in the `src/main/resources` directory with the following environmental variables:
     - `spring.application.name`
     - `spring.autoconfigure.exclude`
     - `spring.jpa.hibernate.ddl-auto`
     - `spring.datasource.url`
     - `spring.datasource.slave1.url`
     - `spring.datasource.slave2.url`
     - `spring.datasource.username`
     - `spring.datasource.password`
     - `spring.datasource.driver-class`
     - `server.port`
3. Build the application using the `gradle build` command in the command prompt.
4. Run the application using the `gradle bootRun` command in the command prompt.
5. Using the specified port number, launch the application in your browser via `localhost:<port number>`

## Dependencies
The dependencies of the project are located in the `build.gradle` file of the repository under the `src` directory.
1. **Spring Data JPA**
     - Used to create JPA-based (Java Persistence API) repositories
2. **Spring Web**
     - Used to build the RESTful web application using Spring MVC (Model-View-Controller)
3. **Spring Boot DevTools**
     - Used for faster application relaunches and configurations for enhanced development experience
4. **MySQL JDBC Driver**
     - Used to connect to the MySQL databases in the virtual machines

## Discussion of the Distributed Database System's Design, Concurrency Control and Consistency, and Global Failure and Recovery Strategy
The significant details of the distributed database design, concurrency control and consistency, and global failure and recovery strategy are documented in this [paper](https://drive.google.com/file/d/1oqLWK2b2EjM7uYK4zSQ4-7E-jXVc60-Q). This database application was designed, tested, evaluated, undergoing a series of edge test cases, including central node failures, replication failures, and the like.

## Database Information
The data stored in the distributed database system is a fraction of the SeriousMD appointments dataset, consisting of exactly more or less appointment records out of the original nine million records. Unlike the original dataset, this distributed database system is denormalized into one table for faster access of data between nodes.

## Sample Transactions
Listed below are the sample transactions that you can use in running the application for the different operation types. However, you can also try testing the different test case scenarios stated in this [paper](https://drive.google.com/file/d/1oqLWK2b2EjM7uYK4zSQ4-7E-jXVc60-Q) for better learning outcomes. The `id` field can be any number between 1 to 1000 on any node since the node redirection of the distributed database system allows reading from any node of any data despite the region difference.
1. **Read**
     - `SELECT * FROM appointments WHERE id = ?;`
2. **Write (Update)**
     - `UPDATE appointments SET <field_1> = <value_1>, ..., <field_n> = <value_n> WHERE id = ?;`
3. **Write (Delete)**
     - `DELETE FROM appointments WHERE id = ?;`
4. **Find All**
     - Only the node, transaction, and operation type are required in this operation type
     - `SELECT * FROM appointments;`
