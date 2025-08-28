package types;

public class LabelDTO {
    private final String name;
    private final boolean isExit;

    public LabelDTO(String name, boolean isExit) {
        this.name = name;
        this.isExit = isExit;
    }

    public String getName() { return name; }
    public boolean isExit() { return isExit; }
}