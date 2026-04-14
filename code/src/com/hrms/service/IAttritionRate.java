package com.hrms.service;

import com.hrms.exception.HRMSException;
import com.hrms.model.AttritionRecord;
import com.hrms.model.PeriodType;
import java.time.LocalDate;
import java.util.List;

public interface IAttritionRate {
    AttritionRecord calculateAttritionRate(PeriodType periodType, LocalDate startDate, LocalDate endDate)
            throws HRMSException;

    List<AttritionRecord> generateTrendData(PeriodType periodType, LocalDate startDate, LocalDate endDate)
            throws HRMSException;
}
