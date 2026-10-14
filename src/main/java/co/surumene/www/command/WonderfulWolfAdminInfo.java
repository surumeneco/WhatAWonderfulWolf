package co.surumene.www.command;

import co.surumene.www.ability.AbilityRank;
import co.surumene.www.config.MessageService;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
    private final MessageService messages;
    private final GenomeEngine engine;
    private final Supplier<WonderfulWolfGenomeProfile> profile;

    public WonderfulWolfAdminInfo(
            Server server,
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profile,
            MessageService messages) {
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
            int total,
            CommandSender sender) {
        Locale locale = sender instanceof Player player ? player.locale() : Locale.JAPANESE;
        Component out = Component.text("◆ ", NamedTextColor.GOLD)
                .append(displayName(wolf).color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(" " + index + "/" + total, NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD));

        out = append(out, section(locale, "basic"));
        out = append(out, localizedLine(locale, "uuid", wolf.getUniqueId().toString()));
        out = append(out, localizedLine(locale, "owner", individual.ownerId()
                .map(this::playerName).orElse(msg(locale, "none"))));
        double maximumHealth = wolf.getAttribute(Attribute.MAX_HEALTH) == null
                ? wolf.getHealth() : wolf.getAttribute(Attribute.MAX_HEALTH).getValue();
        out = append(out, localizedLine(locale, "current_health",
                format(wolf.getHealth()) + " / " + format(maximumHealth)));
        out = append(out, localizedLine(locale, "current_age",
                !wolf.isAdult() ? msg(locale, "immature")
                        : effective.map(v -> format(v.ageGameDays()))
                                .orElse(msg(locale, "immature"))));

        String marker = engine.marker(profile.get().backbone(),
                individual.genome()).formatted();
        out = append(out, section(locale, "pedigree"));
        out = append(out, localizedLine(locale, "bloodline", marker));
        out = append(out, localizedLine(locale, "generation",
                Integer.toString(individual.generation())));
        PedigreeSnapshot pedigree = individual.pedigree();
        out = append(out, parent(locale, "parent_a", pedigree.parentA()));
        out = append(out, parent(locale, "parent_b", pedigree.parentB()));
        out = append(out, ancestor(locale, "grandparent_aa", pedigree.grandparentAA()));
        out = append(out, ancestor(locale, "grandparent_ab", pedigree.grandparentAB()));
        out = append(out, ancestor(locale, "grandparent_ba", pedigree.grandparentBA()));
        out = append(out, ancestor(locale, "grandparent_bb", pedigree.grandparentBB()));

        out = append(out, section(locale, "behavior"));
        String mode = messages.display(locale, "mode", individual.mode())
                + individual.commanderId().map(id -> " (" + msg(locale, "commander")
                        + ": " + playerName(id) + ")").orElse("");
        out = append(out, localizedLine(locale, "mode", mode));
        out = append(out, localizedLine(locale, "action_distance",
                messages.display(locale, "distance", individual.actionDistance())));
        out = append(out, localizedLine(locale, "wait_location",
                individual.waitLocation().map(this::position).orElse(msg(locale, "none"))));
        out = append(out, weaponLine(locale, individual));

        out = append(out, section(locale, "abilities"));
        for (Ability ability : ABILITIES) {
            out = append(out, abilityLine(locale, wolf, ability, individual, effective));
        }

        out = append(out, section(locale, "traits"));
        Personality personality = individual.phenotypeSnapshot().personality();
        out = append(out, localizedLine(locale, "personality",
                messages.display(locale, "personality", personality)));
        out = append(out, localizedLine(locale, "trait",
                traitText(locale, individual.phenotypeSnapshot().expressedTraits())));
        out = append(out, localizedLine(locale, "divine_lineage",
                String.format(Locale.ROOT, "%.3f (%s)",
                        individual.phenotypeSnapshot().divineLineageTotalScore(),
                        msg(locale, individual.phenotypeSnapshot().divineLineageExpressed()
                                ? "expressed" : "not_expressed"))));

        out = append(out, section(locale, "age"));
        out = append(out, ageLine(locale, effective));

        out = append(out, section(locale, "injuries"));
        List<InjuryPhenotype> injuries = individual.phenotypeSnapshot().injuries();
        if (injuries.isEmpty()) {
            out = append(out, localizedLine(locale, "injury", msg(locale, "none")));
        } else {
            for (InjuryPhenotype injury : injuries) {
                double age = effective.map(EffectiveAbilities::ageGameDays).orElse(0.0);
                boolean active = age >= injury.onsetGameDay();
                String value = messages.display(locale, "ability", injury.ability())
                        + " / " + msg(locale, "injury_onset") + " "
                        + format(injury.onsetGameDay())
                        + " / " + msg(locale, "injury_severity") + " "
                        + format(injury.severityRank())
                        + " / " + msg(locale, active ? "injury_active" : "injury_latent");
                out = append(out, localizedLine(locale, "injury", value));
            }
        }

        var relation = individual.phenotypeSnapshot().relationshipPerformance();
        out = append(out, section(locale, "relationship"));
        out = append(out, localizedLine(locale, "relationship",
                msg(locale, "initial_affection") + " " + relation.initialAffection()
                        + " / " + msg(locale, "affection_delta") + " "
                        + relation.affectionDelta()));
        return out;
    }

    private String msg(Locale locale, String key) {
        return messages.text(locale, "info." + key);
    }

    private Component section(Locale locale, String key) {
        return prefix(msg(locale, key));
    }

    private Component localizedLine(Locale locale, String key, String value) {
        return line(msg(locale, key), value);
    }

    private Component abilityLine(
            Locale locale,
            Wolf wolf,
            Ability ability,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective) {
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
                        "  " + messages.display(locale, "ability", ability) + ": ",
                        injury ? NamedTextColor.RED : NamedTextColor.GRAY)
                .append(Component.text(
                        format(base) + " (" + format(current) + ") / ",
                        NamedTextColor.WHITE))
                .append(Component.text(
                        messages.display(locale, "rank", rank),
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

    private Component ageLine(Locale locale, Optional<EffectiveAbilities> effective) {
        if (effective.isEmpty()) {
            return localizedLine(locale, "growth_age", msg(locale, "immature"));
        }
        EffectiveAbilities values = effective.orElseThrow();
        double age = values.ageGameDays();
        var curve = values.ageCurve();
        String stage = age < curve.growthEndGameDay() ? msg(locale, "stage_growing")
                : age < curve.agingStartGameDay() ? msg(locale, "stage_prime")
                : age < curve.elderGameDay() ? msg(locale, "stage_aging")
                : msg(locale, "stage_elder");
        return localizedLine(locale, "growth_age",
                stage
                        + " / " + msg(locale, "growth_end") + " " + format(curve.growthEndGameDay())
                        + " / " + msg(locale, "aging_start") + " " + format(curve.agingStartGameDay())
                        + " / " + msg(locale, "elder_day") + " " + format(curve.elderGameDay())
                        + " / " + msg(locale, "max_reduction") + " "
                        + format(curve.maximumAgingReductionRank()));
    }

    private Component weaponLine(Locale locale, WonderfulWolfIndividual individual) {
        if (individual.weapon().isEmpty()) {
            return localizedLine(locale, "weapon", msg(locale, "none"));
        }
        ItemStack item = PaperItemStackCodec.restore(
                individual.weapon().orElseThrow());
        ItemMeta meta = item.getItemMeta();
        Component name = meta.customName();
        if (name == null) {
            name = Component.translatable(item.getType().translationKey());
        }
        return Component.text("  " + msg(locale, "weapon") + ": ", NamedTextColor.GRAY)
                .append(name);
    }

    private String traitText(Locale locale, List<ExpressedTrait> traits) {
        if (traits.isEmpty()) return msg(locale, "none");
        return traits.stream()
                .map(entry -> traitName(locale, entry.trait())
                        + (entry.strength() == TraitStrength.STRONG
                            && STRONG_VISIBLE.contains(entry.trait()) ? "II" : ""))
                .reduce((a, b) -> a + " / " + b)
                .orElse(msg(locale, "none"));
    }

    private String traitName(Locale locale, Trait trait) {
        return messages.text(locale, "trait." + trait.name().toLowerCase(Locale.ROOT) + ".name");
    }

    private Component parent(Locale locale, String key, Optional<ParentSnapshot> parent) {
        return localizedLine(locale, key, parent.map(value -> {
            String personality;
            try {
                personality = messages.display(locale, "personality",
                        Personality.valueOf(value.personalityId()));
            } catch (IllegalArgumentException error) {
                personality = msg(locale, "unknown");
            }
            String traits = value.expressedTraitIds().isEmpty() ? msg(locale, "none")
                    : value.expressedTraitIds().stream()
                            .map(id -> {
                                try { return traitName(locale, Trait.valueOf(id)); }
                                catch (IllegalArgumentException error) { return msg(locale, "unknown"); }
                            })
                            .reduce((a, b) -> a + "/" + b).orElse(msg(locale, "none"));
            return ancestorText(value.ancestor()) + " " + personality + " " + traits;
        }).orElse(msg(locale, "none")));
    }

    private Component ancestor(Locale locale, String key, Optional<AncestorSnapshot> ancestor) {
        return localizedLine(locale, key, ancestor.map(value ->
                ancestorText(value) + " " + msg(locale, "unknown") + " "
                        + msg(locale, "unknown")).orElse(msg(locale, "none")));
    }

    private static String ancestorText(AncestorSnapshot value) {
        return value.displayName() + "(" + value.generation() + ") " + value.lineageId();
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
        return prefix(section).append(Component.newline()).append(line(label, value));
    }

    private static Component prefix(String section) {
        return Component.text(
                "[" + section + "]",
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
