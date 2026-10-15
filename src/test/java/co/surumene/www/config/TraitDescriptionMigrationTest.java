package co.surumene.www.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TraitDescriptionMigrationTest {
    @Test
    void migratesOldBundledDescriptionsWithoutOverwritingCustomOnes() {
        var yaml = new YamlConfiguration();
        yaml.set("trait.mining_support.description", "近くのプレイヤーの採掘速度を上げる。");
        yaml.set("trait.direct_inheritance.description", "繁殖時に親の能力Genome領域を優先して引き継ぐ。");
        yaml.set("trait.scavenger.description", "落ちているアイテムを回収し、負傷時は肉を食べて回復する。");
        yaml.set("trait.snowborn.description", "独自の説明文");
        yaml.set("trait.snowborn.name", "独自名称");
        assertTrue(MessageService.migrateJapaneseTraitDescriptions(yaml));
        assertEquals("近くにいると仕事が早くなる。", yaml.getString("trait.mining_support.description"));
        assertEquals("子供が能力を受け継ぎやすい。", yaml.getString("trait.direct_inheritance.description"));
        assertEquals("落ちているアイテムを拾うが、肉を食べてしまうことがある。", yaml.getString("trait.scavenger.description"));
        assertEquals("独自の説明文", yaml.getString("trait.snowborn.description"));
        assertEquals("独自名称", yaml.getString("trait.snowborn.name"));
        assertFalse(MessageService.migrateJapaneseTraitDescriptions(yaml));
    }
}
