package execution;

import java.util.List;

public class ExecutionRequestDTO {
    private final int degree;
    private final List<Long> inputs;
    private final String generation;

    public ExecutionRequestDTO(int degree, List<Long> inputs, String generation) {
        this.degree = Math.max(0, degree);
        this.inputs = inputs;
        this.generation = generation;
    }
    public int getDegree() { return degree; }
    public List<Long> getInputs() { return inputs; }
    public String getGeneration() { return generation; }
}
