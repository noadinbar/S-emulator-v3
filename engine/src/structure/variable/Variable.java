package structure.variable;

public interface Variable  {
    VariableType getType();
    int getNumber();
    String getRepresentation();

    Variable RESULT = new VariableImpl(VariableType.RESULT, 0);
}
