import com.hrms.controller.AttritionController;
import com.hrms.exception.HRMSException;
import com.hrms.model.AttritionRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * Small runner to demonstrate Attrition calculations.
 */
public class MainApp {
    public static void main(String[] args) {
        AttritionController controller = new AttritionController();
        LocalDate start = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        try {
            List<AttritionRecord> trend = controller.trend("monthly", start, end);
            System.out.println("Attrition trend from " + start + " to " + end + ":");
            for (AttritionRecord r : trend) System.out.println(r);
        } catch (HRMSException e) {
            System.err.println("Error computing attrition: " + e.getMessage());
        }
    }
}
