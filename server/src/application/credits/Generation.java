package application.credits;

/** Map-like enum for base credits per generation (int). */
public enum Generation {
    I(5),
    II(100),
    III(500),
    IV(1000);

    private final int credits;

    Generation(int credits) {
        this.credits = credits;
    }

    public int getCredits() {
        return credits;
    }
}