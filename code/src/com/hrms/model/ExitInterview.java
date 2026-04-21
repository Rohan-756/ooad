package com.hrms.model;

import java.time.LocalDate;

/**
 * Model class representing an Exit Interview entity.
 * Linked to an Employee via empId.
 *
 * Schema aligned with com.hrms.db.entities.ExitInterview from hrms-database.jar.
 * Hibernate maps field names to snake_case columns in the `ExitInterview` table:
 *   interview_id   -> interviewId   (String)
 *   emp_id         -> empId         (String, FK to employees.emp_id)
 *   primary_reason -> primaryReason (String)
 *   feedback_text  -> feedbackText  (String)
 *   satisfaction_rating -> satisfactionRating (Integer)
 *   issues_reported -> issuesReported (String)
 *   interviewer_notes -> interviewerNotes (String)
 *   exit_date      -> exitDate      (LocalDate)
 */
public class ExitInterview {

    private String      interviewId;
    private String      empId;           // FK → employees.emp_id
    private String      primaryReason;   // was "exitReason"
    private String      feedbackText;    // was "feedback"
    private Integer     satisfactionRating;
    private String      issuesReported;
    private String      interviewerNotes;
    private String      exitDate;        // stored/returned as ISO-date string (maps to LocalDate in DB)

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    /** Default constructor */
    public ExitInterview() {}

    /** Minimal constructor used by ExitInterviewService. */
    public ExitInterview(String interviewId, String empId,
                         String primaryReason, String feedbackText, String exitDate) {
        this.interviewId   = interviewId;
        this.empId         = empId;
        this.primaryReason = primaryReason;
        this.feedbackText  = feedbackText;
        this.exitDate      = exitDate;
    }

    /** Full constructor with all JAR entity fields. */
    public ExitInterview(String interviewId, String empId, String primaryReason,
                         String feedbackText, Integer satisfactionRating,
                         String issuesReported, String interviewerNotes, String exitDate) {
        this.interviewId       = interviewId;
        this.empId             = empId;
        this.primaryReason     = primaryReason;
        this.feedbackText      = feedbackText;
        this.satisfactionRating = satisfactionRating;
        this.issuesReported    = issuesReported;
        this.interviewerNotes  = interviewerNotes;
        this.exitDate          = exitDate;
    }

    /**
     * Legacy int-id constructor — kept for backward-compatibility with dummy data
     * that uses integer employee IDs.  Converts int ids to String form.
     */
    public ExitInterview(int interviewRowId, int empRowId,
                         String primaryReason, String feedbackText, String exitDate) {
        this("EI_" + interviewRowId, "EMP_" + empRowId, primaryReason, feedbackText, exitDate);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters — canonical (matching JAR entity)
    // ─────────────────────────────────────────────────────────────────────────

    public String  getInterviewId()       { return interviewId; }
    public String  getEmpId()             { return empId; }
    public String  getPrimaryReason()     { return primaryReason; }
    public String  getFeedbackText()      { return feedbackText; }
    public Integer getSatisfactionRating(){ return satisfactionRating; }
    public String  getIssuesReported()    { return issuesReported; }
    public String  getInterviewerNotes()  { return interviewerNotes; }
    public String  getExitDate()          { return exitDate; }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy getters — kept for backward-compatibility
    // ─────────────────────────────────────────────────────────────────────────

    /** @deprecated Use {@link #getInterviewId()} and parse. Returns -1 for non-numeric IDs. */
    @Deprecated
    public int getInterviewIdAsInt() {
        if (interviewId == null) return -1;
        try { return Integer.parseInt(interviewId.replace("EI_", "")); }
        catch (NumberFormatException e) { return -1; }
    }

    /** @deprecated Use {@link #getEmpId()} instead. Returns numeric part of empId or -1. */
    @Deprecated
    public int getEmployeeId() {
        if (empId == null) return -1;
        try { return Integer.parseInt(empId.replace("EMP_", "")); }
        catch (NumberFormatException e) { return empId.hashCode(); }
    }

    /** @deprecated Use {@link #getPrimaryReason()} instead. */
    @Deprecated
    public String getExitReason()    { return primaryReason; }

    /** @deprecated Use {@link #getFeedbackText()} instead. */
    @Deprecated
    public String getFeedback()      { return feedbackText; }

    /** @deprecated Use {@link #getExitDate()} instead. */
    @Deprecated
    public String getInterviewDate() { return exitDate; }

    // ─────────────────────────────────────────────────────────────────────────
    // Setters — canonical
    // ─────────────────────────────────────────────────────────────────────────

    public void setInterviewId(String interviewId)           { this.interviewId = interviewId; }
    public void setEmpId(String empId)                       { this.empId = empId; }
    public void setPrimaryReason(String primaryReason)       { this.primaryReason = primaryReason; }
    public void setFeedbackText(String feedbackText)         { this.feedbackText = feedbackText; }
    public void setSatisfactionRating(Integer r)             { this.satisfactionRating = r; }
    public void setIssuesReported(String issuesReported)     { this.issuesReported = issuesReported; }
    public void setInterviewerNotes(String interviewerNotes) { this.interviewerNotes = interviewerNotes; }
    public void setExitDate(String exitDate)                 { this.exitDate = exitDate; }

    // Legacy setters
    /** @deprecated Use {@link #setEmpId(String)} instead. */
    @Deprecated
    public void setEmployeeId(int id) { this.empId = "EMP_" + id; }

    /** @deprecated Use {@link #setPrimaryReason(String)} instead. */
    @Deprecated
    public void setExitReason(String r) { this.primaryReason = r; }

    /** @deprecated Use {@link #setFeedbackText(String)} instead. */
    @Deprecated
    public void setFeedback(String f) { this.feedbackText = f; }

    /** @deprecated Use {@link #setExitDate(String)} instead. */
    @Deprecated
    public void setInterviewDate(String d) { this.exitDate = d; }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ExitInterview{" +
                "interviewId='" + interviewId + '\'' +
                ", empId='" + empId + '\'' +
                ", primaryReason='" + primaryReason + '\'' +
                ", exitDate='" + exitDate + '\'' +
                '}';
    }
}
