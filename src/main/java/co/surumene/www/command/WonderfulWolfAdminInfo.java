package co.surumene.www.command;

import co.surumene.www.ability.AbilityRank;
import co.surumene.www.ability.AbilityScale;
import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.ability.EffectiveAbility;
import co.surumene.www.ability.PersonalityAbilityModifier;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.InjuryPhenotype;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.AncestorSnapshot;
import co.surumene.www.individual.ParentSnapshot;
import co.surumene.www.individual.PedigreeSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.ui.PaperItemStackCodec;
import co.surumene.www.runtime.PaperAbilityProjector;
import co.surumene.wgl.api.GenomeEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class WonderfulWolfAdminInfo {
    private static final List<Ability> ABILITIES = List.of(
            Ability.HEALTH,
            Ability.SIZE,
            Ability.MOVEMENT_SPEED,
            Ability.JUMP,
            Ability.STEP_HEIGHT,
            Ability.ATTACK_DAMAGE,
            Ability.ATTACK_SPEED,
            Ability.DEFENSE,
            Ability.PATIENCE,
            Ability.INVENTORY);
    private static final Set<Trait> STRONG_VISIBLE = EnumSet.of(
            Trait.MINING_SUPPORT,
            Trait.MUSCLE,
            Trait.GUARDIAN,
            Trait.HEALING,
            Trait.DIRECT_INHERITANCE,
            Trait.WILD,
            Trait.HOLY_POISON,
            Trait.UNYIELDING,
            Trait.LIFE_DRAIN,
            Trait.INTIMIDATION);

    private final Server server;
    private final GenomeEngine engine;
    private final Supplier<WonderfulWolfGenomeProfile> profile;

    public WonderfulWolfAdminInfo(
            Server server,
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profile) {
        this.server = java.util.Objects.requireNonNull(server, "server");
        this.engine = java.util.Objects.requireNonNull(engine, "engine");
        this.profile = java.util.Objects.requireNonNull(profile, "profile");
    }

    public Component render(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective,
            int index,
            int total) {
        Component out = Component.text("◆ ", NamedTextColor.GOLD)
                .append(displayName(wolf)
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(
                        "  " + index + "/" + total,
                        NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD));

        out = append(out, section(
                "基本",
                "UUID",
                wolf.getUniqueId().toString()));
        out = append(out, line(
                "Owner",
                individual.ownerId()
                        .map(this::playerName)
                        .orElse("無し")));
        double maximumHealth = wolf.getAttribute(Attribute.MAX_HEALTH) == null
                ? wolf.getHealth()
                : wolf.getAttribute(Attribute.MAX_HEALTH).getValue();
        out = append(out, line(
                "現在HP",
                format(wolf.getHealth()) + " / " + format(maximumHealth)));
        out = append(out, line(
                "現在日齢",
                effective.map(v -> format(v.ageGameDays())).orElse("幼体")));

        String lineage = engine.marker(
                profile.get().backbone(),
                individual.genome()).formatted();
        out = append(out, section(
                "血統",
                "血統ID",
                lineage));
        out = append(out, line("世代", Integer.toString(individual.generation())));
        PedigreeSnapshot pedigree = individual.pedigree();
        out = append(out, ancestor("親A", pedigree.parentA().map(ParentSnapshot::ancestor)));
        out = append(out, ancestor("親B", pedigree.parentB().map(ParentSnapshot::ancestor)));
        out = append(out, ancestor("祖父母A-A", pedigree.grandparentAA()));
        out = append(out, ancestor("祖父母A-B", pedigree.grandparentAB()));
        out = append(out, ancestor("祖父母B-A", pedigree.grandparentBA()));
        out = append(out, ancestor("祖父母B-B", pedigree.grandparentBB()));

        String mode = individual.mode().displayName()
                + individual.commanderId()
                    .map(id -> " (指揮者: " + playerName(id) + ")")
                    .orElse("");
        out = append(out, section("行動", "モード", mode));
        out = append(out, line(
                "行動距離",
                individual.actionDistance().displayName()));
        out = append(out, line(
                "待機地点",
                individual.waitLocation()
                        .map(this::position)
                        .orElse("無し")));
        out = append(out, weaponLine(individual));

        boolean firstAbility = true;
        for (Ability ability : ABILITIES) {
            Component abilityLine = abilityLine(
                    ability,
                    individual,
                    effective);
            if (firstAbility) {
                abilityLine = prefix("能力").append(abilityLine);
                firstAbility = false;
            }
            out = append(out, abilityLine);
        }

        Personality personality = individual.phenotypeSnapshot().personality();
        out = append(out, section(
                "性格・特性",
                "性格",
                personalityText(personality)));
        out = append(out, line(
                "特性",
                traitText(individual.phenotypeSnapshot().expressedTraits())));
        out = append(out, line(
                "神の系譜",
                String.format(
                        Locale.ROOT,
                        "%.3f (%s)",
                        individual.phenotypeSnapshot().divineLineageTotalScore(),
                        individual.phenotypeSnapshot().divineLineageExpressed()
                                ? "発現"
                                : "未発現")));

        out = append(out, ageLine(effective));

        List<InjuryPhenotype> injuries =
                individual.phenotypeSnapshot().injuries();
        if (injuries.isEmpty()) {
            out = append(out, section("怪我", "怪我", "無し"));
        } else {
            for (int i = 0; i < injuries.size(); i++) {
                InjuryPhenotype injury = injuries.get(i);
                double age = effective.map(EffectiveAbilities::ageGameDays)
                        .orElse(0.0);
                boolean active = age >= injury.onsetGameDay();
                String value = injury.ability().displayName()
                        + " / 発症 " + format(injury.onsetGameDay()) + "日"
                        + " / 重症度 " + format(injury.severityRank()) + "ランク"
                        + " / " + (active ? "発症中" : "未発症");
                Component injuryLine = Component.text(
                        (i == 0 ? "" : "  ") + "怪我: ",
                        NamedTextColor.GRAY)
                        .append(Component.text(
                                value,
                                active ? NamedTextColor.RED : NamedTextColor.WHITE));
                if (i == 0) injuryLine = prefix("怪我").append(injuryLine);
                out = append(out, injuryLine);
            }
        }

        var relation = individual.phenotypeSnapshot().relationshipPerformance();
        out = append(out, section(
                "関係性能",
                "関係性能",
                "初期好感度 " + relation.initialAffection()
                        + " / 好感度増減量 " + relation.affectionDelta()));
        return out;
    }

    private Component abilityLine(
            Ability ability,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective) {
        double normalized = individual.phenotypeSnapshot()
                .abilities()
                .get(ability);
        double base = effective
                .map(v -> v.get(ability).baseCanonical())
                .orElseGet(() -> AbilityScale.toCanonical(ability, normalized));
        double current = effective
                .map(v -> observedCanonical(
                        wolf,
                        ability,
                        v.get(ability).effectiveCanonical()))
                .orElse(base);
        AbilityRank rank = AbilityRank.fromNormalized(normalized);
        boolean injury = effective
                .map(v -> v.get(ability).injuryActive())
                .orElse(false);
        return Component.text(
                        "  " + ability.displayName() + ": ",
                        injury ? NamedTextColor.RED : NamedTextColor.GRAY)
                .append(Component.text(
                        format(base) + " (" + format(current) + ") / ",
                        NamedTextColor.WHITE))
                .append(Component.text(
                        rank.displayName(),
                        rankColor(rank))
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(
                        " (" + String.format(Locale.ROOT, "%.3f", normalized) + ")",
                        NamedTextColor.DARK_GRAY));
    }

    private static double observedCanonical(
            Wolf wolf,
            Ability ability,
            double fallback) {
        Attribute attribute = switch (ability) {
            case HEALTH -> Attribute.MAX_HEALTH;
            case SIZE -> Attribute.SCALE;
            case MOVEMENT_SPEED -> Attribute.MOVEMENT_SPEED;
            case JUMP -> Attribute.JUMP_STRENGTH;
            case STEP_HEIGHT -> Attribute.STEP_HEIGHT;
            case ATTACK_DAMAGE -> Attribute.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attribute.ATTACK_SPEED;
            case DEFENSE -> Attribute.ARMOR;
            case PATIENCE, INVENTORY -> null;
        };
        if (attribute == null || wolf.getAttribute(attribute) == null) {
            return fallback;
        }
        double value = wolf.getAttribute(attribute).getValue();
        return switch (ability) {
            case MOVEMENT_SPEED ->
                    PaperAbilityProjector.blocksPerSecondForMovementAttribute(value);
            case JUMP ->
                    PaperAbilityProjector.heightForJumpStrength(value);
            default -> value;
        };
    }

    private Component ageLine(Optional<EffectiveAbilities> effective) {
        if (effective.isEmpty()) {
            return section("成長・老化", "成長・老化", "幼体");
        }
        EffectiveAbilities values = effective.orElseThrow();
        double age = values.ageGameDays();
        var curve = values.ageCurve();
        String stage = age < curve.growthEndGameDay()
                ? "成長期"
                : age < curve.agingStartGameDay()
                    ? "全盛期"
                    : age < curve.elderGameDay()
                        ? "老化期"
                        : "高齢期";
        return section(
                "成長・老化",
                "成長・老化",
                stage
                        + " / 成長終了 " + format(curve.growthEndGameDay()) + "日"
                        + " / 老化開始 " + format(curve.agingStartGameDay()) + "日"
                        + " / 高齢期 " + format(curve.elderGameDay()) + "日"
                        + " / 最大低下 " + format(curve.maximumAgingReductionRank()) + "ランク");
    }

    private Component weaponLine(WonderfulWolfIndividual individual) {
        if (individual.weapon().isEmpty()) {
            return line("武器", "無し");
        }
        ItemStack item = PaperItemStackCodec.restore(
                individual.weapon().orElseThrow());
        ItemMeta meta = item.getItemMeta();
        Component name = meta.customName();
        if (name == null) {
            name = Component.translatable(item.getType().translationKey());
        }
        return Component.text("  武器: ", NamedTextColor.GRAY)
                .append(name);
    }

    private String personalityText(Personality personality) {
        Set<Ability> positive = PersonalityAbilityModifier.positive(personality);
        Set<Ability> negative = PersonalityAbilityModifier.negative(personality);
        if (positive.isEmpty() && negative.isEmpty()) {
            return personality.displayName() + " (補正なし)";
        }
        return personality.displayName()
                + " (" + abilityNames(positive, "↑")
                + " / " + abilityNames(negative, "↓") + ")";
    }

    private static String abilityNames(Set<Ability> abilities, String suffix) {
        if (abilities.isEmpty()) return "なし";
        return abilities.stream()
                .map(Ability::displayName)
                .sorted()
                .reduce((a, b) -> a + "・" + b)
                .orElse("なし")
                + suffix;
    }

    private static String traitText(List<ExpressedTrait> traits) {
        if (traits.isEmpty()) return "無し";
        return traits.stream()
                .map(entry -> entry.trait().displayName()
                        + (entry.strength() == TraitStrength.STRONG
                            && STRONG_VISIBLE.contains(entry.trait())
                            ? "II"
                            : ""))
                .reduce((a, b) -> a + " / " + b)
                .orElse("無し");
    }

    private Component ancestor(
            String label,
            Optional<AncestorSnapshot> ancestor) {
        return line(
                label,
                ancestor.map(value ->
                        value.displayName()
                                + " / " + value.generation()
                                + "世代 / " + value.lineageId())
                        .orElse("無し"));
    }

    private String playerName(UUID id) {
        String name = server.getOfflinePlayer(id).getName();
        return name == null || name.isBlank() ? id.toString() : name;
    }

    private String position(WorldPosition position) {
        String world = Optional.ofNullable(server.getWorld(position.worldId()))
                .map(org.bukkit.World::getName)
                .orElse(position.worldId().toString());
        return world + " "
                + format(position.x()) + " "
                + format(position.y()) + " "
                + format(position.z());
    }

    private static Component displayName(Wolf wolf) {
        Component custom = wolf.customName();
        return custom == null
                ? Component.translatable(wolf.getType().translationKey())
                : custom;
    }

    private static Component append(Component base, Component line) {
        return base.append(Component.newline()).append(line);
    }

    private static Component section(
            String section,
            String label,
            String value) {
        return prefix(section).append(line(label, value));
    }

    private static Component prefix(String section) {
        return Component.text(
                "[" + section + "] ",
                NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD);
    }

    private static Component line(String label, String value) {
        return Component.text(
                        "  " + label + ": ",
                        NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private static String format(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static NamedTextColor rankColor(AbilityRank rank) {
        return switch (rank) {
            case MISERABLE -> NamedTextColor.DARK_RED;
            case VERY_LOW -> NamedTextColor.RED;
            case LOW -> NamedTextColor.GOLD;
            case SLIGHTLY_LOW -> NamedTextColor.YELLOW;
            case COMMON -> NamedTextColor.WHITE;
            case SLIGHTLY_HIGH -> NamedTextColor.GREEN;
            case HIGH -> NamedTextColor.AQUA;
            case VERY_HIGH -> NamedTextColor.BLUE;
            case LEGENDARY -> NamedTextColor.LIGHT_PURPLE;
            case MYTHICAL -> NamedTextColor.DARK_PURPLE;
            case IMPOSSIBLE -> NamedTextColor.GOLD;
        };
    }
}
