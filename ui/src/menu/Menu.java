package menu;

public enum Menu {
    LOAD_XML(1, "Load XML"),
    DISPLAY_PROGRAM(2, "Display Program"),
    EXPAND(3, "Expand"),
    RUN(4, "Execute"),
    HISTORY(5, "History"),
    EXIT(6, "Exit");

    public final int num;
    public final String title;

    Menu(int num, String title) { this.num = num; this.title = title; }

    public static Menu byChoice(int n) {
        for (Menu m : values()) if (m.num == n) return m;
        return null;
    }
}
