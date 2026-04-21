package com.hrms.service.validation;

import com.hrms.model.Employee;
import com.hrms.exception.HRMSException;

public class MetricsValidator extends EmployeeValidator {
    @Override
    public boolean check(Employee employee) throws HRMSException.InvalidInputException {
        if (employee.getAttendanceRate() < 0 || employee.getAttendanceRate() > 100) {
            throw new HRMSException.InvalidInputException(
                    "Attendance rate must be between 0 and 100. Got: " + employee.getAttendanceRate());
        }
        if (employee.getYearsOfService() < 0) {
            throw new HRMSException.InvalidInputException(
                    "Years of service cannot be negative. Got: " + employee.getYearsOfService());
        }
        if (employee.getMonthsSincePromotion() < 0) {
            throw new HRMSException.InvalidInputException(
                    "Months since promotion cannot be negative. Got: " + employee.getMonthsSincePromotion());
        }
        return checkNext(employee);
    }
}
