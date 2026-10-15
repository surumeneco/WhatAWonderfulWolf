package co.surumene.www.ui;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WolfPedigreeLineTest {
    @Test void parentsAndGrandparentsUseIdenticalCompactInfoRow() {
        assertEquals("無名の狼(2) a1b2 まじめ 直伝/見張り番",
                WolfPedigreeLine.format("無名の狼", 2, "a1b2", "まじめ",
                        List.of("直伝", "見張り番"), "無し"));
        assertEquals("祖父(1) ffff 不明 不明",
                WolfPedigreeLine.format("祖父", 1, "ffff", null,
                        List.of(), "不明"));
    }
}
