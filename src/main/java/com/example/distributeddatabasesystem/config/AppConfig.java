package com.example.distributeddatabasesystem.config;

import com.example.distributeddatabasesystem.model.Appointments;
import com.example.distributeddatabasesystem.repository.AppointmentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Objects;

@Configuration
@EnableScheduling
@PropertySource(value = { "classpath:application.properties" })
public class AppConfig
{
    @Bean
    @Primary
    public JdbcTemplate node1JdbcTemplate(@Qualifier("node1DataSource") DataSource dataSource)
    {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate node2JdbcTemplate(@Qualifier("node2DataSource") DataSource dataSource)
    {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate node3JdbcTemplate(@Qualifier("node3DataSource") DataSource dataSource)
    {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public DataSource node1DataSource()
    {
        System.out.println("TEST");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://ccscloud.dlsu.edu.ph:20189/mco2");
        dataSource.setUsername("user");
        dataSource.setPassword("sS3pvALPxcWbTCEgXnzq9VYN");
        return dataSource;
    }

    @Bean
    public DataSource node2DataSource()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://ccscloud.dlsu.edu.ph:20190/mco2");
        dataSource.setUsername("user");
        dataSource.setPassword("sS3pvALPxcWbTCEgXnzq9VYN");
        return dataSource;
    }

    @Bean
    public DataSource node3DataSource()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://ccscloud.dlsu.edu.ph:20191/mco2");
        dataSource.setUsername("user");
        dataSource.setPassword("sS3pvALPxcWbTCEgXnzq9VYN");
        return dataSource;
    }


}