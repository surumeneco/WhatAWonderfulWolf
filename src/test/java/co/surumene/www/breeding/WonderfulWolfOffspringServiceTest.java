package co.surumene.www.breeding;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfOffspringServiceTest {
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(
                    WwwConfigLoader.loadDefaults(),
                    engine.geneSequenceCodec());
    private final WonderfulWolfOffspringService service =
            new WonderfulWolfOffspringService(engine, () -> profile);

    @Test
    void acceptsDiploidGameteAndGameteGameteInputs() {
        DiploidGenome diploid = backboneDiploid();
        HaploidGenome gamete = backboneGamete();

        assertInstanceOf(
                WonderfulWolfOffspringService.Result.Success.class,
                service.breed(
                        new BreedingParentSource.DiploidParent(diploid),
                        new BreedingParentSource.Gamete(gamete),
                        2026100703L));
        assertInstanceOf(
                WonderfulWolfOffspringService.Result.Success.class,
                service.breed(
                        new BreedingParentSource.Gamete(gamete),
                        new BreedingParentSource.Gamete(gamete),
                        2026100704L));
    }

    @Test
    void rejectsSourceThatIsNotCompatibleWithWonderfulWolfBackbone() {
        HaploidGenome incompatible = invertedBackboneGamete();

        WonderfulWolfOffspringService.Result.Failure failure = assertInstanceOf(
                WonderfulWolfOffspringService.Result.Failure.class,
                service.breed(
                        new BreedingParentSource.Gamete(incompatible),
                        new BreedingParentSource.Gamete(backboneGamete()),
                        2026100705L));

        assertEquals("BACKBONE_INCOMPATIBLE_A", failure.reason());
    }

    private DiploidGenome backboneDiploid() {
        List<ChromosomePair> pairs = new ArrayList<>();
        for (var chromosome : profile.backbone().chromosomes()) {
            pairs.add(new ChromosomePair(
                    chromosome.templateBits(),
                    chromosome.templateBits()));
        }
        return new DiploidGenome(1, pairs);
    }

    private HaploidGenome backboneGamete() {
        return new HaploidGenome(
                1,
                profile.backbone().chromosomes().stream()
                        .map(chromosome -> chromosome.templateBits())
                        .toList());
    }

    private HaploidGenome invertedBackboneGamete() {
        return new HaploidGenome(
                1,
                profile.backbone().chromosomes().stream()
                        .map(chromosome -> invert(chromosome.templateBits()))
                        .toList());
    }

    private static BitSequence invert(BitSequence source) {
        String bits = source.toBitString();
        StringBuilder out = new StringBuilder(bits.length());
        for (int i = 0; i < bits.length(); i++) {
            out.append(bits.charAt(i) == '0' ? '1' : '0');
        }
        return BitSequence.fromBits(out.toString());
    }
}
