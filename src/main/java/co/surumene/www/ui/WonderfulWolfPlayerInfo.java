package co.surumene.www.ui;

import co.surumene.www.ability.AbilityRank;
import co.surumene.www.config.MessageService;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.TraitStrength;
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

    public WonderfulWolfPlayerInfo(MessageService messages) {
        this.messages = java.util.Objects.requireNonNull(messages, "messages");
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
        lore.add(ageLine(wolf, effective));
        effective.ifPresent(values -> {
            for (Ability ability : DISPLAY_ORDER) {
                lore.add(abilityLine(ability, values.get(ability), personality));
            }
        });
        if (effective.isEmpty()) {
            for (Ability ability : DISPLAY_ORDER) {
                AbilityRank rank = AbilityRank.fromNormalized(
                        individual.phenotypeSnapshot().abilities().get(ability));
                lore.add(Component.text(
                        abilityLabel(ability, personality) + ": " + rank.displayName(),
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
        lore.add(ancestorLine("親A", p.parentA().map(ParentSnapshot::ancestor)));
        lore.add(ancestorLine("親B", p.parentB().map(ParentSnapshot::ancestor)));
        lore.add(ancestorLine("祖父母A-A", p.grandparentAA()));
        lore.add(ancestorLine("祖父母A-B", p.grandparentAB()));
        lore.add(ancestorLine("祖父母B-A", p.grandparentBA()));
        lore.add(ancestorLine("祖父母B-B", p.grandparentBB()));
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
                .append(Component.text(wolf.getName(), NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD));
        Personality personality = individual.phenotypeSnapshot().personality();
        message = message.append(Component.newline())
                .append(personalityDetail(personality))
                .append(Component.newline())
                .append(ageLine(wolf, effective));
        if (effective.isPresent()) {
            for (Ability ability : DISPLAY_ORDER) {
                message = message.append(Component.newline())
                        .append(abilityLine(ability, effective.orElseThrow().get(ability), personality));
            }
        } else {
            for (Ability ability : DISPLAY_ORDER) {
                AbilityRank rank = AbilityRank.fromNormalized(
                        individual.phenotypeSnapshot().abilities().get(ability));
                message = message.append(Component.newline()).append(
                        Component.text(
                                abilityLabel(ability, personality) + ": ",
                                NamedTextColor.GRAY)
                                .append(rankComponent(rank)));
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
                .append(Component.text(wolf.getName(), NamedTextColor.YELLOW)
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
        message = appendParent(player.locale(), message, "親A", p.parentA());
        message = appendParent(player.locale(), message, "親B", p.parentB());
        message = appendAncestor(player.locale(), message, "祖父母A-A", p.grandparentAA());
        message = appendAncestor(player.locale(), message, "祖父母A-B", p.grandparentAB());
        message = appendAncestor(player.locale(), message, "祖父母B-A", p.grandparentBA());
        message = appendAncestor(player.locale(), message, "祖父母B-B", p.grandparentBB());
        player.sendMessage(message);
    }

    private static Component abilityLine(
            Ability ability,
            EffectiveAbility value,
            Personality personality) {
        boolean changed = Math.abs(value.effectiveNormalized() - value.baseNormalized()) > 1.0e-9;
        NamedTextColor labelColor = value.injuryActive()
                ? NamedTextColor.RED
                : NamedTextColor.GRAY;
        Component line = Component.text(
                abilityLabel(ability, personality) + ": ",
                labelColor).append(rankComponent(value.rank()));
        if (changed) {
            AbilityRank current = AbilityRank.fromNormalized(value.effectiveNormalized());
            line = line.append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(rankComponent(current));
        }
        return line;
    }

    private static Component personalityDetail(Personality personality) {
        return Component.text(
                messages.text(viewer.locale(), "gui.nature") + ": " + messages.display(viewer.locale(), "personality", personality),
                NamedTextColor.AQUA);
    }

    private static Component ageLine(
            Wolf wolf,
            Optional<EffectiveAbilities> effective) {
        if (!wolf.isAdult()) {
            return Component.text("年齢: 幼体", NamedTextColor.GRAY);
        }
        if (effective.isEmpty()) {
            return Component.text("年齢: 0.0日 / 成長期", NamedTextColor.GRAY);
        }
        EffectiveAbilities values = effective.orElseThrow();
        double age = values.ageGameDays();
        String stage;
        if (age < values.ageCurve().growthEndGameDay()) {
            stage = "成長期";
        } else if (age < values.ageCurve().agingStartGameDay()) {
            stage = "全盛期";
        } else if (age < values.ageCurve().elderGameDay()) {
            stage = "老化期";
        } else {
            stage = "高齢期";
        }
        return Component.text(
                String.format(Locale.ROOT, "年齢: %.1f日 / %s", age, stage),
                NamedTextColor.GRAY);
    }

    private static String abilityLabel(Ability ability, Personality personality) {
        if (PersonalityAbilityModifier.positive(personality).contains(ability)) {
            return ability.displayName() + "↑";
        }
        if (PersonalityAbilityModifier.negative(personality).contains(ability)) {
            return ability.displayName() + "↓";
        }
        return ability.displayName();
    }

    private static Component rankComponent(AbilityRank rank) {
        return Component.text(rank.displayName(), rankColor(rank))
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

    private String traitNames(Locale locale, List<String> ids) {
        if (ids.isEmpty()) return messages.text(locale, "info.none");
        return ids.stream()
                .map(id -> traitName(locale, id))
                .reduce((a, b) -> a + " / " + b)
                .orElse("無し");
    }

    private String traitName(Locale locale, String id) {
        try {
            return messages.text(locale, "trait."
                    + Trait.valueOf(id).name().toLowerCase(Locale.ROOT) + ".name");
        } catch (IllegalArgumentException ignored) {
            return id;
        }
    }

    private static Component ancestorLine(
            String label,
            Optional<AncestorSnapshot> ancestor) {
        return Component.text(label + ": " + ancestorText(ancestor), NamedTextColor.GRAY);
    }

    private static String ancestorText(Optional<AncestorSnapshot> ancestor) {
        return ancestor.map(value ->
                value.displayName()
                        + " / " + value.generation()
                        + "世代 / " + value.lineageId())
                .orElse("無し");
    }

    private Component appendAncestor(
            Locale locale, Component base,
            String label,
            Optional<AncestorSnapshot> ancestor) {
        return base.append(Component.newline())
                .append(ancestorLine(label, ancestor));
    }

    private Component appendParent(
            Locale locale, Component base,
            String label,
            Optional<ParentSnapshot> parent) {
        if (parent.isEmpty()) {
            return base.append(Component.newline())
                    .append(Component.text(label + ": 無し", NamedTextColor.GRAY));
        }
        ParentSnapshot value = parent.orElseThrow();
        String personality;
        try {
            personality = messages.display(locale, "personality", Personality.valueOf(value.personalityId()));
        } catch (IllegalArgumentException ignored) {
            personality = value.personalityId();
        }
        String genetics = personality
                + " / " + traitNames(locale, value.expressedTraitIds())
                + (value.divineLineageExpressed() ? " / " + messages.text(locale, "gui.divine_lineage") : "");
        return base.append(Component.newline())
                .append(Component.text(
                        label + ": " + ancestorText(Optional.of(value.ancestor())),
                        NamedTextColor.GRAY))
                .append(Component.text(" / " + genetics, NamedTextColor.WHITE));
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = ItemStack.of(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}
