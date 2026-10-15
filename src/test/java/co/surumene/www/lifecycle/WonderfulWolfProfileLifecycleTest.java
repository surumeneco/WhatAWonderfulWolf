package co.surumene.www.lifecycle;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.wgl.api.GenomeProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WonderfulWolfProfileLifecycleTest {
    @Test
    void registersInitialProfileAndAtomicallyReplacesOnlyValidReloads() {
        RecordingGateway gateway = new RecordingGateway();
        WonderfulWolfProfileLifecycle lifecycle = new WonderfulWolfProfileLifecycle(gateway);
        WwwConfig initial = WwwConfigLoader.loadDefaults();

        lifecycle.start(initial);

        assertEquals(1, gateway.registerCount);
        assertSame(lifecycle.currentProfile(), gateway.registered);
        assertSame(initial, lifecycle.currentConfig());

        WwwConfig reloaded = withPersonalityModifier(initial, 0.20);
        GenomeProfile<?> previousProfile = lifecycle.currentProfile();

        lifecycle.reload(reloaded);

        assertEquals(1, gateway.replaceCount);
        assertSame(reloaded, lifecycle.currentConfig());
        assertSame(lifecycle.currentProfile(), gateway.replaced);
        assertNotSame(previousProfile, lifecycle.currentProfile());

        WwwConfig invalid = withDescendingActionDistance(reloaded);
        assertThrows(IllegalArgumentException.class, () -> lifecycle.reload(invalid));

        assertEquals(1, gateway.replaceCount);
        assertSame(reloaded, lifecycle.currentConfig());
    }

    @Test
    void failedRegistryReplaceLeavesPreviousRuntimeStateUntouched() {
        RecordingGateway gateway = new RecordingGateway();
        WonderfulWolfProfileLifecycle lifecycle = new WonderfulWolfProfileLifecycle(gateway);
        WwwConfig initial = WwwConfigLoader.loadDefaults();
        lifecycle.start(initial);

        GenomeProfile<?> previousProfile = lifecycle.currentProfile();
        gateway.failReplace = true;

        assertThrows(IllegalStateException.class,
                () -> lifecycle.reload(withPersonalityModifier(initial, 0.20)));

        assertSame(initial, lifecycle.currentConfig());
        assertSame(previousProfile, lifecycle.currentProfile());
    }

    @Test
    void closingLifecycleUnregistersOwnerProfiles() {
        RecordingGateway gateway = new RecordingGateway();
        WonderfulWolfProfileLifecycle lifecycle = new WonderfulWolfProfileLifecycle(gateway);
        lifecycle.start(WwwConfigLoader.loadDefaults());

        lifecycle.close();

        assertEquals(1, gateway.unregisterCount);
    }

    private static WwwConfig withPersonalityModifier(WwwConfig base, double modifierRate) {
        WwwConfig.Runtime runtime = base.runtime();
        return new WwwConfig(
                base.configVersion(),
                base.founderTarget(),
                base.genomeProfile(),
                new WwwConfig.Runtime(
                        runtime.combat(),
                        runtime.actionDistance(),
                        runtime.wanWand(),
                        runtime.age(),
                        runtime.relationship(),
                        new WwwConfig.PersonalityRuntime(modifierRate),
                        runtime.traits(),
                        runtime.spawn()));
    }

    private static WwwConfig withDescendingActionDistance(WwwConfig base) {
        WwwConfig.Runtime runtime = base.runtime();
        return new WwwConfig(
                base.configVersion(),
                base.founderTarget(),
                base.genomeProfile(),
                new WwwConfig.Runtime(
                        runtime.combat(),
                        new WwwConfig.ActionDistance(5.0, 4.0, 20.0, 30.0),
                        runtime.wanWand(),
                        runtime.age(),
                        runtime.relationship(),
                        runtime.personality(),
                        runtime.traits(),
                        runtime.spawn()));
    }

    private static final class RecordingGateway implements ProfileRegistryGateway {
        int registerCount;
        int replaceCount;
        int unregisterCount;
        boolean failReplace;
        GenomeProfile<?> registered;
        GenomeProfile<?> replaced;

        @Override
        public void register(GenomeProfile<?> profile) {
            registerCount++;
            registered = profile;
        }

        @Override
        public void replace(GenomeProfile<?> profile) {
            if (failReplace) {
                throw new IllegalStateException("replace failed");
            }
            replaceCount++;
            replaced = profile;
        }

        @Override
        public void unregisterOwner() {
            unregisterCount++;
        }
    }
}
