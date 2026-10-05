package co.surumene.www.spawn;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfFactoryIntegrationTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(config, engine.geneSequenceCodec());
    private final WonderfulWolfFactory factory =
            new WonderfulWolfFactory(engine, () -> profile);

    @Test
    void naturalFounderBecomesACompleteGenerationZeroIndividual() {
        UUID owner = UUID.fromString("12345678-1234-1234-1234-123456789abc");

        WonderfulWolfCreationResult.Success success = assertInstanceOf(
                WonderfulWolfCreationResult.Success.class,
                factory.createFounder(
                        FounderOrigin.NATURAL,
                        Optional.of(owner),
                        987654L,
                        2026100503L));

        WonderfulWolfIndividual individual = success.individual();
        assertEquals(Optional.of(owner), individual.ownerId());
        assertEquals(987654L, individual.adultBiologicalTime());
        assertEquals(0, individual.generation());
        assertEquals(Mode.WANDER, individual.mode());
        assertEquals(ActionDistance.NORMAL, individual.actionDistance());
        assertTrue(individual.commanderId().isEmpty());
        assertTrue(individual.waitLocation().isEmpty());
        assertTrue(individual.affection().isEmpty());
        assertTrue(individual.inventory().isEmpty());
        assertTrue(individual.weapon().isEmpty());
        assertEquals(0, individual.pedigree().parentA().stream().count());
        individual.phenotypeSnapshot().abilities().values().forEach(value ->
                assertTrue(value >= 0.0 && value <= 0.5));
    }
}
