package com.dbconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DbConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbConnectorApplication.class, args);
    }
}
