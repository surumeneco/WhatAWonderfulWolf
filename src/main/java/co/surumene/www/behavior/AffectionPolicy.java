package co.surumene.www.behavior;

import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AffectionPolicy {
    private AffectionPolicy() {}

    public static long current(
            WonderfulWolfIndividual individual,
            UUID playerId) {
        Objects.requireNonNull(individual, "individual");
        Objects.requireNonNull(playerId, "playerId");
        return individual.affection().getOrDefault(
                playerId,
                (long) individual.phenotypeSnapshot()
                        .relationshipPerformance()
                        .initialAffection());
    }

    public static WonderfulWolfIndividual reward(
            WonderfulWolfIndividual individual,
            UUID playerId) {
        return adjust(
                individual,
                playerId,
                individual.phenotypeSnapshot()
                        .relationshipPerformance()
                        .affectionDelta());
    }

    public static WonderfulWolfIndividual penalize(
            WonderfulWolfIndividual individual,
            UUID playerId) {
        return adjust(
                individual,
                playerId,
                -((long) individual.phenotypeSnapshot()
                        .relationshipPerformance()
                        .affectionDelta()));
    }

    public static WonderfulWolfIndividual adjust(
            WonderfulWolfIndividual individual,
            UUID playerId,
            long delta) {
        Objects.requireNonNull(individual, "individual");
        Objects.requireNonNull(playerId, "playerId");

        long current = current(individual, playerId);
        long next;
        try {
            next = Math.addExact(current, delta);
        } catch (ArithmeticException overflow) {
            next = delta >= 0
                    ? WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION
                    : -WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION;
        }
        next = Math.max(
                -WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION,
                Math.min(
                        WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION,
                        next));

        Map<UUID, Long> affection =
                new HashMap<>(individual.affection());
        affection.put(playerId, next);
        return individual.withAffection(affection);
    }
}
