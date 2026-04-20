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

    private static final String DB_URL = "jdbc:sqlite:hrms.db";
    private static volatile DBConnection instance = null;
    private Connection connection;
    private boolean dummyMode = false;

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
            // Fall back to dummy in-memory mode when JDBC driver is not available.
            this.connection = null;
            this.dummyMode = true;
            System.err.println("[DBConnection] Warning: SQLite JDBC Driver not found. Running in dummy mode (no DB). Add sqlite-jdbc.jar to classpath for real DB access.");
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
            if (dummyMode) return null;
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
     * Returns true when the connection could not be established and DB access is disabled.
     */
    public boolean isDummyMode() {
        return dummyMode;
    }

    /**
     * Initializes attrition-subsystem tables on first run.
     *
     * NOTE: The `employees` and `exit_interviews` tables are owned and managed
     * by the DB team (hrms-database.jar / hrms.db). We must NOT recreate them.
     * We only create the two tables that belong to this subsystem.
     */
    private void initializeSchema() {
        // Attrition rate history — owned by Attrition Analysis subsystem
        String createAttritionRecords = """
                CREATE TABLE IF NOT EXISTS attrition_records (
                    record_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    time_period     TEXT NOT NULL,
                    total_employees INTEGER NOT NULL,
                    employees_left  INTEGER NOT NULL,
                    attrition_rate  REAL NOT NULL
                );
                """;

        // Risk evaluations — owned by Attrition Analysis subsystem
        String createRiskAssessments = """
                CREATE TABLE IF NOT EXISTS risk_assessments (
                    assessment_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_id     INTEGER NOT NULL,
                    risk_level      TEXT NOT NULL,
                    reason          TEXT,
                    assessed_date   TEXT NOT NULL
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAttritionRecords);
            stmt.execute(createRiskAssessments);
            System.out.println("[DBConnection] Attrition-subsystem tables initialized (attrition_records, risk_assessments).");
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
