package structure.variable;

import java.io.Serializable;

public class VariableImpl implements Variable, Serializable {

    private final VariableType type;
    private final int number;
    private static final long serialVersionUID = 1L;

    public VariableImpl(VariableType type, int number) {
        this.type = type;
        this.number = number;
    }

    @Override
    public VariableType getType() {
        return type;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public String getRepresentation() {
        return type.getVariableRepresentation(number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl)) return false;
        VariableImpl other = (VariableImpl) o;
        return this.number == other.number && this.type == other.type;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + Integer.hashCode(number);
        return result;
    }

}
