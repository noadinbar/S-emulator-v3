package types;

public class LabelDTO {
    private final String name;     // למשל: "L34"
    private final boolean isExit;  // true רק עבור EXIT ברשימת התוויות בשימוש

    public LabelDTO(String name, boolean isExit) {
        this.name = name;
        this.isExit = isExit;
    }

    public String getName() { return name; }
    public boolean isExit() { return isExit; }
}