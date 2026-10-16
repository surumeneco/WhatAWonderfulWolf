package co.surumene.www.config;
import co.surumene.www.domain.Ability;
import java.util.Set;

public final class WwwStatValidator {
    private WwwStatValidator() {}

    public static void validate(WwwConfig.Stats stats) {
        for (Ability ability : Ability.values()) {
            WwwConfig.Stat stat = stats.get(ability);
            String key = "stats." + DefaultStats.key(ability);
            if (!Double.isFinite(stat.min()) || !Double.isFinite(stat.max())
                    || stat.min() < 0 || stat.min() >= stat.max()) {
                throw new IllegalArgumentException(key + ": require 0 <= min < max");
            }
            if ((ability == Ability.HEALTH || ability == Ability.SIZE) && stat.min() <= 0) {
                throw new IllegalArgumentException(key + ": min must be positive");
            }
            if (!Set.of("none", "rank", "value", "both").contains(stat.display())) {
                throw new IllegalArgumentException(key + ".display: invalid display mode");
            }
        }
    }
}
