package co.surumene.www.runtime;

import co.surumene.www.domain.Trait;

public final class EffectKeys {
    private EffectKeys() {}

    public static boolean present(Trait value) {
        return value != null;
    }
}
