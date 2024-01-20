package com.wyona.katie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 *
 */
@Configuration
public class DataSourceConfig {

    @Value("${driver.class.name}")
    private String driverClassName;

    @Value("${db.url}")
    private String dbURL;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    /**
     *
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setUrl(dbURL);
        dataSource.setDriverClassName(driverClassName);

        return dataSource;
    }
}
