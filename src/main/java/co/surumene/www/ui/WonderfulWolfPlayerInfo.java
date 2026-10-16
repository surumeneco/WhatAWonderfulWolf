package co.surumene.www.ui;

import co.surumene.www.ability.AbilityRank;
import co.surumene.www.config.MessageService;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.util.WolfDisplayName;
import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.ability.EffectiveAbility;
import co.surumene.www.ability.PersonalityAbilityModifier;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.Trait;
import co.surumene.www.individual.AncestorSnapshot;
import co.surumene.www.individual.ParentSnapshot;
import co.surumene.www.individual.PedigreeSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class WonderfulWolfPlayerInfo {
    private static final List<Ability> DISPLAY_ORDER = List.of(
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
            Trait.MINING_SUPPORT, Trait.MUSCLE, Trait.GUARDIAN, Trait.HEALING,
            Trait.DIRECT_INHERITANCE, Trait.WILD, Trait.HOLY_POISON,
            Trait.UNYIELDING, Trait.LIFE_DRAIN, Trait.INTIMIDATION);

    private final MessageService messages;
    private java.util.function.Supplier<co.surumene.www.config.WwwConfig.Stats> stats;

    public WonderfulWolfPlayerInfo(MessageService messages) {
        this.messages = java.util.Objects.requireNonNull(messages, "messages");
        this.stats = co.surumene.www.config.DefaultStats::create;
    }

    public ItemStack infoItem(
            Player viewer,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective) {
        ItemStack item = named(Material.PAPER, messages.text(viewer.locale(), "gui.info"));
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        Personality personality = individual.phenotypeSnapshot().personality();
        lore.add(Component.text(messages.text(viewer.locale(), "gui.nature") + ": " + messages.display(viewer.locale(), "personality", personality), NamedTextColor.AQUA));
        lore.add(ageLine(viewer.locale(), wolf, effective));
        effective.ifPresent(values -> {
            for (Ability ability : DISPLAY_ORDER) {
                lore.add(abilityLine(viewer.locale(), ability, values.get(ability), personality));
            }
        });
        if (effective.isEmpty()) {
            for (Ability ability : DISPLAY_ORDER) {
                AbilityRank rank = AbilityRank.fromNormalized(
                        individual.phenotypeSnapshot().abilities().get(ability));
                lore.add(Component.text(
                        abilityLabel(viewer.locale(), ability, personality) + ": "
                                + messages.display(viewer.locale(), "rank", rank),
                        rankColor(rank)));
            }
        }
        lore.add(Component.text(messages.text(viewer.locale(), "gui.click_detail"), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack pedigreeItem(
            Player viewer,
            WonderfulWolfIndividual individual,
            String lineageId) {
        ItemStack item = named(Material.WRITABLE_BOOK, messages.text(viewer.locale(), "gui.pedigree"));
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        String traits = displayedTraits(viewer.locale(),
                individual.phenotypeSnapshot().expressedTraits());
        lore.add(Component.text(messages.text(viewer.locale(), "gui.trait")
                + ": " + traits, NamedTextColor.AQUA));
        lore.addAll(traitDescriptionLines(viewer.locale(),
                individual.phenotypeSnapshot().expressedTraits()));
        if (individual.phenotypeSnapshot().divineLineageExpressed()) {
            lore.add(Component.text(messages.text(viewer.locale(), "gui.divine_lineage"), NamedTextColor.LIGHT_PURPLE));
        }
        lore.add(Component.text(messages.text(viewer.locale(), "gui.bloodline") + ": " + lineageId, NamedTextColor.GRAY));
        lore.add(Component.text(messages.text(viewer.locale(), "gui.generation") + ": " + individual.generation(), NamedTextColor.GRAY));
        PedigreeSnapshot p = individual.pedigree();
        lore.add(parentLine(viewer.locale(), "parent_a", p.parentA()));
        lore.add(parentLine(viewer.locale(), "parent_b", p.parentB()));
        lore.add(ancestorLine(viewer.locale(), "grandparent_aa", p.grandparentAA()));
        lore.add(ancestorLine(viewer.locale(), "grandparent_ab", p.grandparentAB()));
        lore.add(ancestorLine(viewer.locale(), "grandparent_ba", p.grandparentBA()));
        lore.add(ancestorLine(viewer.locale(), "grandparent_bb", p.grandparentBB()));
        lore.add(Component.text(messages.text(viewer.locale(), "gui.click_detail"), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void sendInfo(
            org.bukkit.entity.Player player,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective) {
        Component message = Component.text("◆ " + messages.text(player.locale(), "gui.info")
                + " — ", NamedTextColor.GOLD)
                .append(WolfDisplayName.fromWolf(wolf,
                        Component.text(messages.text(player.locale(), "info.unnamed"), NamedTextColor.YELLOW))
                        .decorate(TextDecoration.BOLD));
        Personality personality = individual.phenotypeSnapshot().personality();
        message = message.append(Component.newline())
                .append(personalityDetail(player.locale(), personality))
                .append(Component.newline())
                .append(ageLine(player.locale(), wolf, effective));
        if (effective.isPresent()) {
            for (Ability ability : DISPLAY_ORDER) {
                message = message.append(Component.newline())
                        .append(abilityLine(player.locale(), ability, effective.orElseThrow().get(ability), personality));
            }
        } else {
            for (Ability ability : DISPLAY_ORDER) {
                AbilityRank rank = AbilityRank.fromNormalized(
                        individual.phenotypeSnapshot().abilities().get(ability));
                message = message.append(Component.newline()).append(
                        Component.text(
                                abilityLabel(player.locale(), ability, personality) + ": ",
                                NamedTextColor.GRAY)
                                .append(rankComponent(player.locale(), rank)));
            }
        }
        player.sendMessage(message);
    }

    public void sendPedigree(
            org.bukkit.entity.Player player,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            String lineageId) {
        Component message = Component.text("◆ " + messages.text(player.locale(), "gui.pedigree")
                + " — ", NamedTextColor.GOLD)
                .append(WolfDisplayName.fromWolf(wolf,
                        Component.text(messages.text(player.locale(), "info.unnamed"), NamedTextColor.YELLOW))
                        .decorate(TextDecoration.BOLD));
        message = message.append(Component.newline())
                .append(Component.text(messages.text(player.locale(), "gui.trait") + ": "
                        + displayedTraits(player.locale(),
                                individual.phenotypeSnapshot().expressedTraits()),
                        NamedTextColor.AQUA));
        for (Component detail : traitDescriptionLines(player.locale(),
                individual.phenotypeSnapshot().expressedTraits())) {
            message = message.append(Component.newline()).append(detail);
        }
        if (individual.phenotypeSnapshot().divineLineageExpressed()) {
            message = message.append(Component.newline())
                    .append(Component.text(messages.text(player.locale(), "gui.divine_lineage") + ": " + messages.text(player.locale(), "info.expressed"), NamedTextColor.LIGHT_PURPLE));
        }
        message = message.append(Component.newline())
                .append(Component.text(messages.text(player.locale(), "gui.bloodline") + ": " + lineageId, NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text(messages.text(player.locale(), "gui.generation") + ": " + individual.generation(), NamedTextColor.GRAY));

        PedigreeSnapshot p = individual.pedigree();
        message = appendParent(player.locale(), message, "parent_a", p.parentA());
        message = appendParent(player.locale(), message, "parent_b", p.parentB());
        message = appendAncestor(player.locale(), message, "grandparent_aa", p.grandparentAA());
        message = appendAncestor(player.locale(), message, "grandparent_ab", p.grandparentAB());
        message = appendAncestor(player.locale(), message, "grandparent_ba", p.grandparentBA());
        message = appendAncestor(player.locale(), message, "grandparent_bb", p.grandparentBB());
        player.sendMessage(message);
    }

    private Component abilityLine(
            Locale locale,
            Ability ability,
            EffectiveAbility value,
            Personality personality) {
        boolean changed = Math.abs(value.effectiveNormalized() - value.baseNormalized()) > 1.0e-9;
        NamedTextColor labelColor = value.injuryActive()
                ? NamedTextColor.RED : NamedTextColor.GRAY;
        Component line = Component.text(abilityLabel(locale, ability, personality) + ": ",
                labelColor).append(rankComponent(locale, value.rank()));
        if (changed) {
            AbilityRank updated = AbilityRank.fromNormalized(value.effectiveNormalized());
            line = line.append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(rankComponent(locale, updated));
        }
        return line;
    }

    private Component personalityDetail(Locale locale, Personality personality) {
        return Component.text(messages.text(locale, "gui.nature") + ": "
                + messages.display(locale, "personality", personality), NamedTextColor.AQUA);
    }

    private Component ageLine(Locale locale, Wolf wolf, Optional<EffectiveAbilities> effective) {
        String label = messages.text(locale, "gui.age");
        if (!wolf.isAdult()) {
            return Component.text(label + ": " + messages.text(locale, "info.immature"),
                    NamedTextColor.GRAY);
        }
        if (effective.isEmpty()) {
            return Component.text(label + ": 0.0 / "
                    + messages.text(locale, "info.stage_growing"), NamedTextColor.GRAY);
        }
        EffectiveAbilities values = effective.orElseThrow();
        double age = values.ageGameDays();
        String stage = age < values.ageCurve().growthEndGameDay() ? "stage_growing"
                : age < values.ageCurve().agingStartGameDay() ? "stage_prime"
                : age < values.ageCurve().elderGameDay() ? "stage_aging" : "stage_elder";
        return Component.text(String.format(Locale.ROOT, "%s: %.1f / %s", label,
                age, messages.text(locale, "info." + stage)), NamedTextColor.GRAY);
    }

    private String abilityLabel(Locale locale, Ability ability, Personality personality) {
        String label = messages.display(locale, "ability", ability);
        if (PersonalityAbilityModifier.positive(personality).contains(ability)) return label + "↑";
        if (PersonalityAbilityModifier.negative(personality).contains(ability)) return label + "↓";
        return label;
    }

    private Component rankComponent(Locale locale, AbilityRank rank) {
        return Component.text(messages.display(locale, "rank", rank), rankColor(rank))
                .decorate(TextDecoration.BOLD);
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

    private String displayedTraits(Locale locale, List<ExpressedTrait> traits) {
        if (traits.isEmpty()) return messages.text(locale, "info.none");
        return traits.stream().map(entry -> {
            String key = "trait." + entry.trait().name().toLowerCase(Locale.ROOT);
            String name = messages.text(locale, key + ".name");
            return name + (entry.strength() == TraitStrength.STRONG
                    && STRONG_VISIBLE.contains(entry.trait()) ? "II" : "");
        }).reduce((a, b) -> a + " / " + b).orElse(messages.text(locale, "info.none"));
    }

    private List<Component> traitDescriptionLines(Locale locale, List<ExpressedTrait> traits) {
        List<Component> result = new ArrayList<>();
        for (ExpressedTrait entry : traits) {
            String key = "trait." + entry.trait().name().toLowerCase(Locale.ROOT);
            String name = messages.text(locale, key + ".name")
                    + (entry.strength() == TraitStrength.STRONG
                        && STRONG_VISIBLE.contains(entry.trait()) ? "II" : "");
            result.add(Component.text("  " + name + ": "
                    + messages.text(locale, key + ".description"), NamedTextColor.GRAY));
        }
        return result;
    }

    private List<String> traitNames(Locale locale, List<String> ids) {
        return ids.stream().map(id -> traitName(locale, id)).toList();
    }

    private String traitName(Locale locale, String id) {
        try {
            return messages.text(locale, "trait."
                    + Trait.valueOf(id).name().toLowerCase(Locale.ROOT) + ".name");
        } catch (IllegalArgumentException ignored) {
            return messages.text(locale, "info.unknown");
        }
    }

    private String personalityName(Locale locale, String id) {
        if (id == null) return null;
        try {
            return messages.display(locale, "personality", Personality.valueOf(id));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Component ancestorLine(
            Locale locale, String key, Optional<AncestorSnapshot> ancestor) {
        String value = ancestor.map(snapshot -> formatAncestor(locale, snapshot))
                .orElse(messages.text(locale, "info.none"));
        return Component.text(messages.text(locale, "info." + key) + ": " + value,
                NamedTextColor.GRAY);
    }

    private Component parentLine(
            Locale locale, String key, Optional<ParentSnapshot> parent) {
        String value = parent.map(snapshot ->
                        WolfPedigreeLine.format(
                                snapshot.ancestor().displayName(),
                                snapshot.ancestor().generation(),
                                snapshot.ancestor().lineageId(),
                                personalityName(locale, snapshot.personalityId()),
                                traitNames(locale, snapshot.expressedTraitIds()),
                                messages.text(locale,
                                        personalityName(locale, snapshot.personalityId()) == null
                                                ? "info.unknown" : "info.none")))
                .orElse(messages.text(locale, "info.none"));
        return Component.text(messages.text(locale, "info." + key) + ": " + value,
                NamedTextColor.GRAY);
    }

    private String formatAncestor(Locale locale, AncestorSnapshot snapshot) {
        String personality = snapshot.personalityId()
                .map(id -> personalityName(locale, id)).orElse(null);
        return WolfPedigreeLine.format(
                snapshot.displayName(), snapshot.generation(), snapshot.lineageId(),
                personality,
                traitNames(locale, snapshot.expressedTraitIds()),
                messages.text(locale, personality == null ? "info.unknown" : "info.none"));
    }

    private Component appendAncestor(
            Locale locale, Component base,
            String key, Optional<AncestorSnapshot> ancestor) {
        return base.append(Component.newline()).append(ancestorLine(locale, key, ancestor));
    }

    private Component appendParent(
            Locale locale, Component base,
            String key, Optional<ParentSnapshot> parent) {
        return base.append(Component.newline()).append(parentLine(locale, key, parent));
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = ItemStack.of(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}
