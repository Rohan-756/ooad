package com.hrms.exception;

/**
 * Custom exception hierarchy for HRMS Employee & Exit Management subsystem.
 * Implements structured error handling per the OOAD design.
 */
public class HRMSException extends RuntimeException {

    public enum Severity {
        MINOR, WARNING, MAJOR
    }

    public enum ErrorCode {
        INVALID_EMPLOYEE_ID,
        INVALID_INPUT,
        MISSING_FEEDBACK,
        DIVIDE_BY_ZERO,
        INVALID_DATE_RANGE,
        INVALID_FILTER,
        MISSING_DATA,
        INSUFFICIENT_DATA,
        DATA_NOT_AVAILABLE
    }

    private final ErrorCode errorCode;
    private final Severity severity;

    public HRMSException(ErrorCode errorCode, String message, Severity severity) {
        super(message);
        this.errorCode = errorCode;
        this.severity = severity;
    }

    public HRMSException(ErrorCode errorCode, String message, Severity severity, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.severity = severity;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Severity getSeverity() { return severity; }

    @Override
    public String toString() {
        return "[" + severity + "] " + errorCode + ": " + getMessage();
    }

    // ---- Specific exception subclasses ----

    /**
     * MAJOR - Employee ID not found in the system.
     */
    public static class InvalidEmployeeIdException extends HRMSException {
        public InvalidEmployeeIdException(String message) {
            super(ErrorCode.INVALID_EMPLOYEE_ID, message, Severity.MAJOR);
        }
    }

    /**
     * MINOR - Invalid data entered by user.
     */
    public static class InvalidInputException extends HRMSException {
        public InvalidInputException(String message) {
            super(ErrorCode.INVALID_INPUT, message, Severity.MINOR);
        }
    }

    /**
     * WARNING - Feedback text is empty in exit interview.
     */
    public static class MissingFeedbackException extends HRMSException {
        public MissingFeedbackException(String message) {
            super(ErrorCode.MISSING_FEEDBACK, message, Severity.WARNING);
        }
    }

    /**
     * MAJOR - Required employee data is missing for analysis.
     */
    public static class MissingDataException extends HRMSException {
        public MissingDataException(String message) {
            super(ErrorCode.MISSING_DATA, message, Severity.MAJOR);
        }
    }

    /**
     * MAJOR - Division by zero attempted during attrition calculation (zero employees).
     */
    public static class DivideByZeroException extends HRMSException {
        public DivideByZeroException(String message) {
            super(ErrorCode.DIVIDE_BY_ZERO, message, Severity.MAJOR);
        }
    }

    /**
     * WARNING - The supplied date range is invalid (e.g. end before start, or null dates).
     */
    public static class InvalidDateRangeException extends HRMSException {
        public InvalidDateRangeException(String message) {
            super(ErrorCode.INVALID_DATE_RANGE, message, Severity.WARNING);
        }
    }

    /**
     * MINOR - A filter specification supplied by the UI is invalid.
     */
    public static class InvalidFilterException extends HRMSException {
        public InvalidFilterException(String message) {
            super(ErrorCode.INVALID_FILTER, message, Severity.MINOR);
        }
    }

    /**
     * WARNING - Not enough data points to perform the requested analysis.
     */
    public static class InsufficientDataException extends HRMSException {
        public InsufficientDataException(String message) {
            super(ErrorCode.INSUFFICIENT_DATA, message, Severity.WARNING);
        }
    }

    /**
     * WARNING - The requested data is not available in the system.
     */
    public static class DataNotAvailableException extends HRMSException {
        public DataNotAvailableException(String message) {
            super(ErrorCode.DATA_NOT_AVAILABLE, message, Severity.WARNING);
        }
    }
}
