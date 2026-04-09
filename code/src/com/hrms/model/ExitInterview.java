package com.hrms.model;

/**
 * Model class representing an Exit Interview entity.
 * Linked to an Employee via employeeId.
 */
public class ExitInterview {

    private int interviewId;
    private int employeeId;
    private String exitReason;
    private String feedback;
    private String interviewDate;

    // Default constructor
    public ExitInterview() {}

    // Parameterized constructor
    public ExitInterview(int interviewId, int employeeId, String exitReason,
                         String feedback, String interviewDate) {
        this.interviewId = interviewId;
        this.employeeId = employeeId;
        this.exitReason = exitReason;
        this.feedback = feedback;
        this.interviewDate = interviewDate;
    }

    // Getters
    public int getInterviewId() { return interviewId; }
    public int getEmployeeId() { return employeeId; }
    public String getExitReason() { return exitReason; }
    public String getFeedback() { return feedback; }
    public String getInterviewDate() { return interviewDate; }

    // Setters
    public void setInterviewId(int interviewId) { this.interviewId = interviewId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public void setInterviewDate(String interviewDate) { this.interviewDate = interviewDate; }

    @Override
    public String toString() {
        return "ExitInterview{" +
                "id=" + interviewId +
                ", employeeId=" + employeeId +
                ", reason='" + exitReason + '\'' +
                ", date='" + interviewDate + '\'' +
                '}';
    }
}
