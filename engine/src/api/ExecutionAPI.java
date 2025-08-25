package api;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

public interface ExecutionAPI {
    /** דרגת ההרחבה המקסימלית עבור התוכנית הטעונה כרגע (מ-ProgramImpl#getMaxDegree) */
    int getMaxDegree();

    /** מבצע הרחבה והרצה לפי הבקשה ומחזיר פלט סופי כ-DTO */
    ExecutionDTO execute(ExecutionRequestDTO request);
}
