package com.hrms.controller;

import com.hrms.exception.HRMSException;
import com.hrms.model.Employee;
import com.hrms.model.ExitInterview;
import com.hrms.service.EmployeeService;
import com.hrms.service.ExitInterviewService;

import java.util.List;

/**
 * EmployeeController — MVC Controller Layer.
 *
 * Acts as the intermediary between the UI (View) and the Service layer (Model).
 * Routes user actions to appropriate services and returns results.
 *
 * SOLID: SRP — Only responsible for request routing and response handling.
 * SOLID: DIP — Depends on service abstractions, not concrete DB calls.
 * GRASP: Controller — Handles system events from the UI layer.
 */
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ExitInterviewService exitInterviewService;

    public EmployeeController() {
        this.employeeService = new EmployeeService();
        this.exitInterviewService = new ExitInterviewService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles the "Add Employee" action from the UI.
     * @return the generated employee ID, or -1 on error.
     */
    public int handleAddEmployee(String name, String department, double attendance,
                                  int yearsOfService, int promotionCount,
                                  Employee.EmploymentStatus status) {
        Employee emp = new Employee();
        emp.setName(name);
        emp.setDepartment(department);
        emp.setAttendancePercentage(attendance);
        emp.setYearsOfService(yearsOfService);
        emp.setPromotionCount(promotionCount);
        emp.setEmploymentStatus(status);

        return employeeService.addEmployee(emp);
    }

    /**
     * Handles "View Employee By ID" action.
     * Throws InvalidEmployeeIdException if not found.
     */
    public Employee handleGetEmployee(int employeeId) {
        return employeeService.getEmployeeById(employeeId);
    }

    /**
     * Returns all employees for table view.
     */
    public List<Employee> handleGetAllEmployees() {
        return employeeService.getAllEmployees();
    }

    /**
     * Handles "Update Employee" action.
     */
    public void handleUpdateEmployee(int employeeId, String name, String department,
                                      double attendance, int yearsOfService,
                                      int promotionCount, Employee.EmploymentStatus status) {
        Employee emp = new Employee();
        emp.setEmployeeId(employeeId);
        emp.setName(name);
        emp.setDepartment(department);
        emp.setAttendancePercentage(attendance);
        emp.setYearsOfService(yearsOfService);
        emp.setPromotionCount(promotionCount);
        emp.setEmploymentStatus(status);

        employeeService.updateEmployee(emp);
    }

    /**
     * Handles "Delete Employee" action.
     */
    public void handleDeleteEmployee(int employeeId) {
        employeeService.deleteEmployee(employeeId);
    }

    /**
     * Returns active employee count for dashboards.
     */
    public int getActiveCount() {
        return employeeService.getActiveEmployeeCount();
    }

    /**
     * Returns total employee count.
     */
    public int getTotalCount() {
        return employeeService.getTotalEmployeeCount();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXIT INTERVIEW ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles "Submit Exit Interview" action.
     * Manages the MissingFeedbackException warning flow.
     *
     * @return true if saved successfully, false if feedback missing (caller shows warning).
     */
    public int handleSaveExitInterview(int employeeId, String exitReason,
                                        String feedback, String date) {
        ExitInterview interview = new ExitInterview();
        interview.setEmployeeId(employeeId);
        interview.setExitReason(exitReason);
        interview.setFeedback(feedback);
        interview.setInterviewDate(date);

        return exitInterviewService.saveExitInterview(interview);
    }

    /**
     * Saves exit interview without feedback — called after user acknowledges warning.
     */
    public int handleSaveExitInterviewNoFeedback(int employeeId, String exitReason, String date) {
        ExitInterview interview = new ExitInterview();
        interview.setEmployeeId(employeeId);
        interview.setExitReason(exitReason);
        interview.setFeedback("");
        interview.setInterviewDate(date);

        return exitInterviewService.saveExitInterviewWithoutFeedback(interview);
    }

    /**
     * Retrieves exit interview for a specific employee ID.
     */
    public ExitInterview handleGetExitInterview(int employeeId) {
        return exitInterviewService.getInterviewByEmployee(employeeId);
    }

    /**
     * Returns all exit interviews.
     */
    public List<ExitInterview> handleGetAllExitInterviews() {
        return exitInterviewService.getAllInterviews();
    }

    /**
     * Returns the count of employees who have exited.
     */
    public int getExitedCount() {
        return exitInterviewService.getExitedEmployeeCount();
    }
}
