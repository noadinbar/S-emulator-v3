package users;

import java.util.concurrent.atomic.AtomicInteger;
import users.UserTableRowDTO;

public class UserTableRow {
    private final String name;
    private final AtomicInteger mainPrograms = new AtomicInteger(0);
    private final AtomicInteger functions = new AtomicInteger(0);
    private final AtomicInteger runs = new AtomicInteger(0);
    private final AtomicInteger creditsCurrent = new AtomicInteger(0);
    private final AtomicInteger creditsUsed = new AtomicInteger(0);

    public UserTableRow(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public int getMainPrograms() { return mainPrograms.get(); }
    public int getFunctions() { return functions.get(); }
    public int getRuns() { return runs.get(); }
    public int getCreditsCurrent() { return creditsCurrent.get(); }
    public int getCreditsUsed() { return creditsUsed.get(); }

    /** +1 main program uploaded by this user */
    public void incMainPrograms() { mainPrograms.incrementAndGet(); }

    /** +1 helper function contributed by this user */
    public void incFunctions() { functions.incrementAndGet(); }

    /** Records a single run (run or debug) and deducts creditsSpent */
    public void recordRun(int creditsSpent) {
        runs.incrementAndGet();
        if (creditsSpent > 0) {
            creditsUsed.addAndGet(creditsSpent);
            creditsCurrent.addAndGet(-creditsSpent);
        }
    }

    /** Adjust credits balance (positive = top-up, negative = spend). */
    public void adjustCredits(int delta) {
        creditsCurrent.addAndGet(delta);
        if (delta < 0) {
            creditsUsed.addAndGet(Math.abs(delta));
        }
    }
}
