package execution;

import java.util.List;

/** בקשת הרצה: דרגת ההרחבה + קלטים (רשימת מספרים, לפי הסדר) */
public class ExecutionRequestDTO {
    private final int degree;
    private final List<Long> inputs;

    public ExecutionRequestDTO(int degree, List<Long> inputs) {
        this.degree = Math.max(0, degree);
        this.inputs = inputs;
    }
    public int getDegree() { return degree; }
    public List<Long> getInputs() { return inputs; }
}
