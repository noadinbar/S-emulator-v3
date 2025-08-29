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
}
