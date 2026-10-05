package co.surumene.www.lifecycle;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.GenomeProfile;

import java.util.Objects;

public final class WonderfulWolfProfileLifecycle implements AutoCloseable {
    private final ProfileRegistryGateway registry;
    private volatile State state;

    public WonderfulWolfProfileLifecycle(ProfileRegistryGateway registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public synchronized void start(WwwConfig candidate) {
        if (state != null) {
            throw new IllegalStateException("Wonderful Wolf profile lifecycle is already started");
        }

        WwwConfig validated = WwwConfigValidator.validate(candidate);
        WonderfulWolfGenomeProfile profile = new WonderfulWolfGenomeProfile(validated);
        registry.register(profile);
        state = new State(validated, profile);
    }

    public synchronized void reload(WwwConfig candidate) {
        requireState();

        WwwConfig validated = WwwConfigValidator.validate(candidate);
        WonderfulWolfGenomeProfile profile = new WonderfulWolfGenomeProfile(validated);

        registry.replace(profile);
        state = new State(validated, profile);
    }

    public WwwConfig currentConfig() {
        return requireState().config();
    }

    public GenomeProfile<?> currentProfile() {
        return requireState().profile();
    }

    @Override
    public synchronized void close() {
        if (state == null) {
            return;
        }
        try {
            registry.unregisterOwner();
        } finally {
            state = null;
        }
    }

    private State requireState() {
        State current = state;
        if (current == null) {
            throw new IllegalStateException("Wonderful Wolf profile lifecycle is not started");
        }
        return current;
    }

    private record State(WwwConfig config, WonderfulWolfGenomeProfile profile) {}
}
