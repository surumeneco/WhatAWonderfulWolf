package co.surumene.www.behavior;

public record RetreatState(
        boolean active,
        long startedTick) {

    public RetreatState {
        if (active && startedTick < 0L) {
            throw new IllegalArgumentException(
                    "active retreat requires a non-negative startedTick");
        }
    }

    public static RetreatState inactive() {
        return new RetreatState(false, -1L);
    }
}
