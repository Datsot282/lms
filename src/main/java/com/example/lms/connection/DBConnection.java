package com.example.lms.connection;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static Connection databaseLink;

    public static Connection getConnection() {
        // Database credentials
        String databaseName = "lms";
        String databaseUser = "postgres"; // It was misspelled as "posgres"
        String databasePassword = "admin";

        // PostgreSQL connection string
        String url = "jdbc:postgresql://localhost:5432/lms?currentSchema=users" + databaseName;

        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish the connection
            databaseLink = DriverManager.getConnection(url, databaseUser, databasePassword);
            System.out.println("Connected to PostgreSQL successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return databaseLink;
    }
}
