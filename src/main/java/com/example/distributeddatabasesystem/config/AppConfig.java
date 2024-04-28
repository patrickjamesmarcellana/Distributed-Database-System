package com.example.distributeddatabasesystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;


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

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource node1DataSource()
    {
        System.out.println("TEST");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class"));
        dataSource.setUrl(environment.getProperty("spring.datasource.url"));
        dataSource.setUsername(environment.getProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getProperty("spring.datasource.password"));
        return dataSource;
    }

    @Bean
    public DataSource node2DataSource()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class"));
        dataSource.setUrl(environment.getProperty("spring.datasource.slave1.url"));
        dataSource.setUsername(environment.getProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getProperty("spring.datasource.password"));
        return dataSource;
    }

    @Bean
    public DataSource node3DataSource()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class"));
        dataSource.setUrl(environment.getProperty("spring.datasource.slave2.url"));
        dataSource.setUsername(environment.getProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getProperty("spring.datasource.password"));
        return dataSource;
    }


}