package com.hrms.controller;

import com.hrms.exception.HRMSException;
import com.hrms.model.AttritionRecord;
import com.hrms.model.PeriodType;
import com.hrms.service.AttritionService;
import com.hrms.service.IAttritionRate;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Attrition features — translates UI input into service calls.
 */
public class AttritionController {

    private final IAttritionRate service;

    public AttritionController() {
        this.service = new AttritionService();
    }

    public AttritionRecord calculate(String periodTypeStr, LocalDate startDate, LocalDate endDate) throws HRMSException {
        PeriodType periodType = parsePeriodType(periodTypeStr);
        return service.calculateAttritionRate(periodType, startDate, endDate);
    }

    public List<AttritionRecord> trend(String periodTypeStr, LocalDate startDate, LocalDate endDate) throws HRMSException {
        PeriodType periodType = parsePeriodType(periodTypeStr);
        return service.generateTrendData(periodType, startDate, endDate);
    }

    private PeriodType parsePeriodType(String s) throws HRMSException {
        if (s == null) throw new HRMSException.InvalidInputException("periodType cannot be null");
        switch (s.trim().toLowerCase()) {
            case "monthly":
                return PeriodType.MONTHLY;
            case "quarterly":
                return PeriodType.QUARTERLY;
            case "annual":
            case "yearly":
                return PeriodType.ANNUAL;
            default:
                throw new HRMSException.InvalidInputException("Unsupported period type: " + s);
        }
    }
}
