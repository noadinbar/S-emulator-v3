package structure.label;

import java.io.Serializable;

public enum FixedLabel implements Label, Serializable {

    EXIT {
        @Override
        public String getLabelRepresentation() {
            return "EXIT";
        }
    },
    EMPTY {
        @Override
        public String getLabelRepresentation() {
            return "";
        }
    };

    @Override
    public abstract String getLabelRepresentation();
}
