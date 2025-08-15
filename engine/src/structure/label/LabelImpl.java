package structure.label;

public class LabelImpl implements Label{

    private final String label;

    public LabelImpl(String l) {
        label = l;
    }

    public String getLabelRepresentation() {
        return label;
    }
}
