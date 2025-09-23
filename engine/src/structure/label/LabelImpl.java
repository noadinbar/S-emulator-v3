package structure.label;

import java.io.Serializable;

public class LabelImpl implements Label, Serializable {

    private final String label;
    private static final long serialVersionUID = 1L;

    public LabelImpl(String l) {
        label = l;
    }

    public String getLabelRepresentation() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabelImpl)) return false;
        LabelImpl other = (LabelImpl) o;
        if (this.label == null && other.label == null) return true;
        if (this.label == null || other.label == null) return false;
        return this.label.equals(other.label); // השוואה רגישה לאותיות
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }


}
