package com.hrms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection — Singleton Pattern Implementation.
 *
 * Ensures only ONE database connection instance exists throughout the
 * application lifecycle, preventing resource waste and ensuring consistency.
 *
 * Design Pattern: Singleton (Thread-safe using double-checked locking)
 * SOLID Principle: SRP — Only responsible for managing the DB connection.
 */
public class DBConnection {

    private static final String DB_URL = "jdbc:sqlite:hrms_attrition.db";
    private static volatile DBConnection instance = null;
    private Connection connection;

    /**
     * Private constructor — prevents external instantiation.
     * Initializes the SQLite connection and creates tables if they don't exist.
     */
    private DBConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DBConnection] Singleton instance created. Connected to: " + DB_URL);
            initializeSchema();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC Driver not found. Add sqlite-jdbc.jar to classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Thread-safe Singleton accessor using double-checked locking.
     * @return The single shared DBConnection instance.
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the active JDBC Connection object.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DBConnection] Connection was closed. Reconnecting...");
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to re-establish database connection: " + e.getMessage(), e);
        }
        return connection;
    }

    /**
     * Initializes all required database tables on first run.
     * Uses CREATE TABLE IF NOT EXISTS for idempotency.
     */
    private void initializeSchema() {
        String createEmployees = """
                CREATE TABLE IF NOT EXISTS employees (
                    employee_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT NOT NULL,
                    department      TEXT NOT NULL,
                    attendance_pct  REAL NOT NULL DEFAULT 100.0,
                    years_of_service INTEGER NOT NULL DEFAULT 0,
                    promotion_count  INTEGER NOT NULL DEFAULT 0,
                    employment_status TEXT NOT NULL DEFAULT 'ACTIVE'
                );
                """;

        String createExitInterviews = """
                CREATE TABLE IF NOT EXISTS exit_interviews (
                    interview_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_id     INTEGER NOT NULL,
                    exit_reason     TEXT NOT NULL,
                    feedback        TEXT,
                    interview_date  TEXT NOT NULL,
                    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
                );
                """;

        // Stub tables for other modules — allow cross-module queries
        String createAttritionRecords = """
                CREATE TABLE IF NOT EXISTS attrition_records (
                    record_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    time_period     TEXT NOT NULL,
                    total_employees INTEGER NOT NULL,
                    employees_left  INTEGER NOT NULL,
                    attrition_rate  REAL NOT NULL
                );
                """;

        String createRiskAssessments = """
                CREATE TABLE IF NOT EXISTS risk_assessments (
                    assessment_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_id     INTEGER NOT NULL,
                    risk_level      TEXT NOT NULL,
                    reason          TEXT,
                    assessed_date   TEXT NOT NULL,
                    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createEmployees);
            stmt.execute(createExitInterviews);
            stmt.execute(createAttritionRecords);
            stmt.execute(createRiskAssessments);
            System.out.println("[DBConnection] Database schema initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the database connection on application shutdown.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                instance = null;
                System.out.println("[DBConnection] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
        }
    }
}
