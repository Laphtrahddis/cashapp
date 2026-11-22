package com.siddharth.cashapp;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CashappApplication {

    public static void main(String[] args) {
        // Run the server in UTC. Two reasons:
        // 1. Servers should store and compare times in one zone, whatever machine they run on.
        // 2. On Windows in India the JVM default is "Asia/Calcutta", an old name the
        //    postgres:17 image no longer accepts, so the database connection fails.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(CashappApplication.class, args);
    }
}
