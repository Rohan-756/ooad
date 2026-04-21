package com.hrms.service.validation;

import com.hrms.model.Employee;
import com.hrms.exception.HRMSException;

public class MetricsValidator extends EmployeeValidator {
    @Override
    public boolean check(Employee employee) throws HRMSException.InvalidInputException {
        if (employee.getAttendancePercentage() < 0 || employee.getAttendancePercentage() > 100) {
            throw new HRMSException.InvalidInputException(
                    "Attendance percentage must be between 0 and 100. Got: " + employee.getAttendancePercentage());
        }
        if (employee.getYearsOfService() < 0) {
            throw new HRMSException.InvalidInputException(
                    "Years of service cannot be negative. Got: " + employee.getYearsOfService());
        }
        if (employee.getPromotionCount() < 0) {
            throw new HRMSException.InvalidInputException(
                    "Promotion count cannot be negative. Got: " + employee.getPromotionCount());
        }
        return checkNext(employee);
    }
}
