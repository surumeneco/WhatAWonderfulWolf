package co.surumene.www.command;

import co.surumene.wgl.api.*;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class OffspringParentSourceCodecTest {
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());

    @Test
    void decodesBase64WglpWithoutGuessingSourceType() {
        DiploidGenome diploid = new DiploidGenome(
                1,
                List.of(new ChromosomePair(
                        BitSequence.fromBits("101"),
                        BitSequence.fromBits("010"))));
        BreedingParentSource source =
                new BreedingParentSource.DiploidParent(diploid);
        String encoded = Base64.getEncoder().encodeToString(
                engine.encodeParentSource(source));

        assertEquals(source, OffspringParentSourceCodec.decode(encoded, engine));
    }

    @Test
    void roundTripsWwcCompatibleParentToken() {
        DiploidGenome genome = new DiploidGenome(1, List.of(
                new ChromosomePair(BitSequence.fromBits("101"), BitSequence.fromBits("010"))));
        BreedingParentSource source = new BreedingParentSource.DiploidParent(genome);
        String token = OffspringParentSourceCodec.encodeToken(source, engine);
        assertTrue(token.startsWith("wglp_"));
        assertEquals(source, OffspringParentSourceCodec.decodeToken(token, engine));
        assertThrows(IllegalArgumentException.class,
                () -> OffspringParentSourceCodec.decodeToken("wglp_***", engine));
    }

    @Test
    void rejectsInvalidBase64OrInvalidWglp() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OffspringParentSourceCodec.decode("***", engine));
        assertThrows(
                RuntimeException.class,
                () -> OffspringParentSourceCodec.decode(
                        Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}),
                        engine));
    }
}
