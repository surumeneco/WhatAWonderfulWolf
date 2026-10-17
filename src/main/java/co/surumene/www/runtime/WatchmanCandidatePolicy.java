package co.surumene.www.runtime;

/** Cheap world/entity checks before owner-chain engagement resolution. */
final class WatchmanCandidatePolicy {
    private WatchmanCandidatePolicy() {}

    static boolean shouldInspect(boolean living, boolean player, boolean dead,
                                 boolean valid, double distanceSquared, double radiusSquared) {
        return living && !player && !dead && valid && distanceSquared <= radiusSquared;
    }
}
