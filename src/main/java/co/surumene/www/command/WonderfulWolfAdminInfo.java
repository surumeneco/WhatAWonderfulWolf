package co.surumene.www.command;

import co.surumene.www.ability.AbilityRank;
import co.surumene.www.ability.AbilityScale;
import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.ability.EffectiveAbility;
import co.surumene.www.ability.PersonalityAbilityModifier;
import co.surumene.www.config.WonderfulWolfMessages;
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
    private final WonderfulWolfMessages messages;

    public WonderfulWolfAdminInfo(
            Server server,
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profile,
            WonderfulWolfMessages messages) {
        this.server = java.util.Objects.requireNonNull(server, "server");
        this.engine = java.util.Objects.requireNonNull(engine, "engine");
        this.profile = java.util.Objects.requireNonNull(profile, "profile");
        this.messages = java.util.Objects.requireNonNull(messages, "messages");
    }

    public Component render(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective,
            int index,
            int total) {
        return render(wolf, individual, effective, index, total, Locale.JAPANESE);
    }

    public Component render(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective,
            int index,
            int total,
            Locale locale) {
        Component out = Component.text("◆ ", NamedTextColor.GOLD)
                .append(displayName(wolf)
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(
                        "  " + index + "/" + total,
                        NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD));

        out = append(out, section(
                messages.text(locale, "sections.basic"),
                "UUID",
                wolf.getUniqueId().toString()));
        out = append(out, line(
                messages.text(locale, "common.owner"),
                individual.ownerId()
                        .map(this::playerName)
                        .orElse(messages.text(locale, "common.none"))));
        double maximumHealth = wolf.getAttribute(Attribute.MAX_HEALTH) == null
                ? wolf.getHealth()
                : wolf.getAttribute(Attribute.MAX_HEALTH).getValue();
        out = append(out, line(
                messages.text(locale, "common.current_hp"),
                format(wolf.getHealth()) + " / " + format(maximumHealth)));
        out = append(out, line(
                messages.text(locale, "common.current_age"),
                effective.map(v -> format(v.ageGameDays())) .orElse(messages.text(locale, "common.juvenile"))));

        String lineage = engine.marker(
                profile.get().backbone(),
                individual.genome()).formatted();
        out = append(out, section(
                messages.text(locale, "sections.pedigree"),
                messages.text(locale, "common.bloodline"),
                lineage));
        out = append(out, line(messages.text(locale, "common.generation"),
                Integer.toString(individual.generation())));
        PedigreeSnapshot pedigree = individual.pedigree();
        out = append(out, parent(messages.text(locale, "ui.parent_a"), pedigree.parentA(), locale));
        out = append(out, parent(messages.text(locale, "ui.parent_b"), pedigree.parentB(), locale));
        out = append(out, ancestor(messages.text(locale, "ui.grandparent_aa"), pedigree.grandparentAA()));
        out = append(out, ancestor(messages.text(locale, "ui.grandparent_ab"), pedigree.grandparentAB()));
        out = append(out, ancestor(messages.text(locale, "ui.grandparent_ba"), pedigree.grandparentBA()));
        out = append(out, ancestor(messages.text(locale, "ui.grandparent_bb"), pedigree.grandparentBB()));

        String mode = messages.text(locale,
                "ui." + individual.mode().name().toLowerCase(Locale.ROOT))
                + individual.commanderId()
                    .map(id -> " (" + messages.text(locale, "ui.commander") + ": " + playerName(id) + ")")
                    .orElse("");
        out = append(out, section(messages.text(locale, "sections.behavior"),
                messages.text(locale, "common.mode"), mode));
        out = append(out, line(
                messages.text(locale, "common.action_distance"),
                messages.text(locale,
                        "action_distance." + individual.actionDistance().name())));
        out = append(out, line(
                messages.text(locale, "common.waiting_position"),
                individual.waitLocation()
                        .map(this::position)
                        .orElse(messages.text(locale, "common.none"))));
        out = append(out, weaponLine(individual, locale));

        boolean firstAbility = true;
        for (Ability ability : ABILITIES) {
            Component abilityLine = abilityLine(
                    wolf,
                    ability,
                    individual,
                    effective,
                    locale);
            if (firstAbility) {
                abilityLine = prefix(messages.text(locale, "sections.abilities")).append(abilityLine);
                firstAbility = false;
            }
            out = append(out, abilityLine);
        }

        Personality personality = individual.phenotypeSnapshot().personality();
        out = append(out, section(
                messages.text(locale, "sections.traits"),
                messages.text(locale, "common.personality"),
                personalityText(personality, locale)));
        out = append(out, line(
                messages.text(locale, "common.trait"),
                traitText(individual.phenotypeSnapshot().expressedTraits(), locale)));
        out = append(out, line(
                messages.text(locale, "common.divine"),
                String.format(
                        Locale.ROOT,
                        "%.3f (%s)",
                        individual.phenotypeSnapshot().divineLineageTotalScore(),
                        individual.phenotypeSnapshot().divineLineageExpressed()
                                ? messages.text(locale, "common.expressed")
                                : messages.text(locale, "common.latent"))));

        out = append(out, ageLine(effective, locale));

        List<InjuryPhenotype> injuries =
                individual.phenotypeSnapshot().injuries();
        if (injuries.isEmpty()) {
            out = append(out, section(messages.text(locale, "sections.injuries"),
                    messages.text(locale, "common.injury"),
                    messages.text(locale, "common.none")));
        } else {
            for (int i = 0; i < injuries.size(); i++) {
                InjuryPhenotype injury = injuries.get(i);
                double age = effective.map(EffectiveAbilities::ageGameDays)
                        .orElse(0.0);
                boolean active = age >= injury.onsetGameDay();
                String value = messages.text(locale, "ability." + injury.ability().name())
                        + " / " + messages.text(locale, "admin.onset") + " " + format(injury.onsetGameDay())
                        + " / " + messages.text(locale, "admin.severity") + " " + format(injury.severityRank())
                        + " / " + messages.text(locale,
                                active ? "admin.injury_active" : "admin.injury_latent");
                Component injuryLine = Component.text(
                        (i == 0 ? "" : "  ") + messages.text(locale, "common.injury") + ": ",
                        NamedTextColor.GRAY)
                        .append(Component.text(
                                value,
                                active ? NamedTextColor.RED : NamedTextColor.WHITE));
                if (i == 0) injuryLine = prefix(messages.text(locale, "sections.injuries")).append(injuryLine);
                out = append(out, injuryLine);
            }
        }

        var relation = individual.phenotypeSnapshot().relationshipPerformance();
        out = append(out, section(
                messages.text(locale, "sections.relationship"),
                messages.text(locale, "common.affection"),
                messages.text(locale, "admin.initial_affection") + " " + relation.initialAffection()
                        + " / " + messages.text(locale, "admin.affection_delta")
                        + " " + relation.affectionDelta()));
        return out;
    }

    private Component abilityLine(
            Wolf wolf,
            Ability ability,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective,
            Locale locale) {
        double genomeNormalized = individual.phenotypeSnapshot()
                .abilities().get(ability);
        double normalized = individual.adminAbilityOverrides().containsKey(ability)
                ? Math.max(0.0, (individual.adminAbilityOverrides().get(ability)
                        - AbilityScale.toCanonical(ability, 0.0))
                        / (AbilityScale.toCanonical(ability, 1.0)
                                - AbilityScale.toCanonical(ability, 0.0)))
                : genomeNormalized;
        double base = effective
                .map(v -> v.get(ability).baseCanonical())
                .orElseGet(() -> individual.adminAbilityOverrides()
                        .getOrDefault(ability,
                                AbilityScale.toCanonical(ability, genomeNormalized)));
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
                        "  " + messages.text(locale, "ability." + ability.name()) + ": ",
                        injury ? NamedTextColor.RED : NamedTextColor.GRAY)
                .append(Component.text(
                        format(base) + " (" + format(current) + ") / ",
                        NamedTextColor.WHITE))
                .append(Component.text(
                        messages.text(locale, "rank." + rank.key()),
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

    private Component ageLine(Optional<EffectiveAbilities> effective, Locale locale) {
        if (effective.isEmpty()) {
            return section(messages.text(locale, "sections.aging"),
                    messages.text(locale, "sections.aging"),
                    messages.text(locale, "common.juvenile"));
        }
        EffectiveAbilities values = effective.orElseThrow();
        double age = values.ageGameDays();
        var curve = values.ageCurve();
        String stage = age < curve.growthEndGameDay()
                ? messages.text(locale, "common.stage_growth")
                : age < curve.agingStartGameDay()
                    ? messages.text(locale, "common.stage_prime")
                    : age < curve.elderGameDay()
                        ? messages.text(locale, "common.stage_aging")
                        : messages.text(locale, "common.stage_elder");
        return section(
                messages.text(locale, "sections.aging"),
                messages.text(locale, "sections.aging"),
                stage
                        + " / " + messages.text(locale, "admin.age_growth_end")
                        + " " + format(curve.growthEndGameDay())
                        + " / " + messages.text(locale, "admin.age_aging_start")
                        + " " + format(curve.agingStartGameDay())
                        + " / " + messages.text(locale, "admin.age_elder")
                        + " " + format(curve.elderGameDay())
                        + " / " + messages.text(locale, "admin.age_max_reduction")
                        + " " + format(curve.maximumAgingReductionRank()));
    }

    private Component weaponLine(WonderfulWolfIndividual individual, Locale locale) {
        if (individual.weapon().isEmpty()) {
            return line(messages.text(locale, "common.weapon"),
                    messages.text(locale, "common.none"));
        }
        ItemStack item = PaperItemStackCodec.restore(
                individual.weapon().orElseThrow());
        ItemMeta meta = item.getItemMeta();
        Component name = meta.customName();
        if (name == null) {
            name = Component.translatable(item.getType().translationKey());
        }
        return Component.text("  " + messages.text(locale, "common.weapon") + ": ", NamedTextColor.GRAY)
                .append(name);
    }

    private String personalityText(Personality personality, Locale locale) {
        return messages.text(locale, "personality." + personality.name());
    }

    private String traitText(List<ExpressedTrait> traits, Locale locale) {
        if (traits.isEmpty()) return messages.text(locale, "common.none");
        return traits.stream()
                .map(entry -> messages.text(locale, "trait." + entry.trait().name() + ".name")
                        + (entry.strength() == TraitStrength.STRONG
                            && STRONG_VISIBLE.contains(entry.trait())
                            ? "II"
                            : ""))
                .reduce((a, b) -> a + " / " + b)
                .orElse("無し");
    }

    private Component parent(
            String label, Optional<ParentSnapshot> parent, Locale locale) {
        if (parent.isEmpty()) return line(label, messages.text(locale, "common.none"));
        ParentSnapshot snapshot = parent.orElseThrow();
        AncestorSnapshot ancestor = snapshot.ancestor();
        String personality;
        try {
            personality = messages.text(locale,
                    "personality." + Personality.valueOf(snapshot.personalityId()).name());
        } catch (IllegalArgumentException invalidLegacyId) {
            personality = messages.text(locale, "common.unknown");
        }
        String traits = snapshot.expressedTraitIds().isEmpty()
                ? messages.text(locale, "common.none")
                : snapshot.expressedTraitIds().stream().map(id -> {
                    try {
                        return messages.text(locale, "trait." + Trait.valueOf(id).name() + ".name");
                    } catch (IllegalArgumentException invalidLegacyId) {
                        return messages.text(locale, "common.unknown");
                    }
                }).reduce((a, b) -> a + "/" + b).orElse(messages.text(locale, "common.none"));
        return line(label, ancestor.displayName() + "(" + ancestor.generation()
                + ") " + ancestor.lineageId() + " " + personality + " " + traits);
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
