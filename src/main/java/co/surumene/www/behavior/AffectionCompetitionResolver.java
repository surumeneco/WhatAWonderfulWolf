package co.surumene.www.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AffectionCompetitionResolver {
    private AffectionCompetitionResolver() {}

    public static UUID choose(
            Map<UUID, Long> candidates,
            double draw) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "candidates must not be empty");
        }
        if (!Double.isFinite(draw) || draw < 0.0 || draw >= 1.0) {
            throw new IllegalArgumentException(
                    "draw must be finite and within [0,1)");
        }

        List<Map.Entry<UUID, Long>> entries =
                new ArrayList<>(candidates.entrySet());
        for (Map.Entry<UUID, Long> entry : entries) {
            Objects.requireNonNull(entry.getKey(), "candidate player");
            Objects.requireNonNull(entry.getValue(), "candidate affection");
        }
        if (entries.size() == 1) {
            return entries.getFirst().getKey();
        }

        long min = entries.stream()
                .mapToLong(Map.Entry::getValue)
                .min()
                .orElseThrow();

        double[] weights = new double[entries.size()];
        double total = 0.0;
        for (int index = 0; index < entries.size(); index++) {
            long value = entries.get(index).getValue();
            double weight = min < 0
                    ? (double) value - (double) min
                    : (double) value;
            weights[index] = Math.max(0.0, weight);
            total += weights[index];
        }

        if (!(total > 0.0)) {
            int index = Math.min(
                    entries.size() - 1,
                    (int) Math.floor(draw * entries.size()));
            return entries.get(index).getKey();
        }

        double threshold = draw * total;
        double cumulative = 0.0;
        for (int index = 0; index < entries.size(); index++) {
            cumulative += weights[index];
            if (threshold < cumulative) {
                return entries.get(index).getKey();
            }
        }
        return entries.getLast().getKey();
    }
}
