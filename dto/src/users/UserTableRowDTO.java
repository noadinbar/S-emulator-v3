package users;

public class UserTableRowDTO {
    private final String name;
    private final int mainPrograms;
    private final int functions;
    private final int creditsCurrent;
    private final int creditsUsed;
    private final int runs;

    public UserTableRowDTO(String name,
                           int mainPrograms,
                           int functions,
                           int creditsCurrent,
                           int creditsUsed,
                           int runs) {
        this.name = name;
        this.mainPrograms = mainPrograms;
        this.functions = functions;
        this.creditsCurrent = creditsCurrent;
        this.creditsUsed = creditsUsed;
        this.runs = runs;
    }

    public String getName() { return name; }
    public int getMainPrograms() { return mainPrograms; }
    public int getFunctions() { return functions; }
    public int getCreditsCurrent() { return creditsCurrent; }
    public int getCreditsUsed() { return creditsUsed; }
    public int getRuns() { return runs; }
}
