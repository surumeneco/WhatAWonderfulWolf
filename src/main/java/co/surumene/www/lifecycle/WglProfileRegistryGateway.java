package co.surumene.www.lifecycle;

import co.surumene.wgl.api.GenomeProfile;
import co.surumene.wgl.plugin.WonderfulGenomeLibService;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class WglProfileRegistryGateway implements ProfileRegistryGateway {
    private final WonderfulGenomeLibService service;
    private final Plugin owner;

    public WglProfileRegistryGateway(WonderfulGenomeLibService service, Plugin owner) {
        this.service = Objects.requireNonNull(service, "service");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public void register(GenomeProfile<?> profile) {
        service.registerProfile(owner, profile);
    }

    @Override
    public void replace(GenomeProfile<?> profile) {
        service.replaceProfile(owner, profile);
    }

    @Override
    public void unregisterOwner() {
        service.unregisterOwner(owner);
    }
}
