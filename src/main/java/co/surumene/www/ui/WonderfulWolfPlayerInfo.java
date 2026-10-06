package co.surumene.www.ui;

import co.surumene.www.ability.AbilityRank;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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

    public ItemStack infoItem(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective) {
        ItemStack item = named(Material.PAPER, "個体情報");
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        Personality personality = individual.phenotypeSnapshot().personality();
        lore.add(Component.text("性格: " + personality.displayName(), NamedTextColor.AQUA));
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
        lore.add(Component.text("クリックで詳細", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack pedigreeItem(
            WonderfulWolfIndividual individual,
            String lineageId) {
        ItemStack item = named(Material.WRITABLE_BOOK, "血統情報");
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        String traits = traitNames(individual.phenotypeSnapshot().expressedTraits().stream()
                .map(entry -> entry.trait().name())
                .toList());
        lore.add(Component.text("特性: " + traits, NamedTextColor.AQUA));
        if (individual.phenotypeSnapshot().divineLineageExpressed()) {
            lore.add(Component.text("神の系譜", NamedTextColor.LIGHT_PURPLE));
        }
        lore.add(Component.text("血統ID: " + lineageId, NamedTextColor.GRAY));
        lore.add(Component.text("世代: " + individual.generation(), NamedTextColor.GRAY));
        PedigreeSnapshot p = individual.pedigree();
        lore.add(ancestorLine("親A", p.parentA().map(ParentSnapshot::ancestor)));
        lore.add(ancestorLine("親B", p.parentB().map(ParentSnapshot::ancestor)));
        lore.add(ancestorLine("祖父母A-A", p.grandparentAA()));
        lore.add(ancestorLine("祖父母A-B", p.grandparentAB()));
        lore.add(ancestorLine("祖父母B-A", p.grandparentBA()));
        lore.add(ancestorLine("祖父母B-B", p.grandparentBB()));
        lore.add(Component.text("クリックで詳細", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void sendInfo(
            org.bukkit.entity.Player player,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Optional<EffectiveAbilities> effective) {
        Component message = Component.text("◆ 個体情報 — ", NamedTextColor.GOLD)
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
        Component message = Component.text("◆ 血統情報 — ", NamedTextColor.GOLD)
                .append(Component.text(wolf.getName(), NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD));
        message = message.append(Component.newline())
                .append(Component.text(
                        "特性: " + traitNames(individual.phenotypeSnapshot().expressedTraits().stream()
                                .map(entry -> entry.trait().name())
                                .toList()),
                        NamedTextColor.AQUA));
        if (individual.phenotypeSnapshot().divineLineageExpressed()) {
            message = message.append(Component.newline())
                    .append(Component.text("神の系譜: 発現", NamedTextColor.LIGHT_PURPLE));
        }
        message = message.append(Component.newline())
                .append(Component.text("血統ID: " + lineageId, NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("世代: " + individual.generation(), NamedTextColor.GRAY));

        PedigreeSnapshot p = individual.pedigree();
        message = appendParent(message, "親A", p.parentA());
        message = appendParent(message, "親B", p.parentB());
        message = appendAncestor(message, "祖父母A-A", p.grandparentAA());
        message = appendAncestor(message, "祖父母A-B", p.grandparentAB());
        message = appendAncestor(message, "祖父母B-A", p.grandparentBA());
        message = appendAncestor(message, "祖父母B-B", p.grandparentBB());
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
        Set<Ability> positive = PersonalityAbilityModifier.positive(personality);
        Set<Ability> negative = PersonalityAbilityModifier.negative(personality);
        String plus = names(positive);
        String minus = names(negative);
        String suffix;
        if (positive.isEmpty() && negative.isEmpty()) {
            suffix = " (補正なし)";
        } else {
            suffix = " (" + (plus.isEmpty() ? "なし" : plus + "↑")
                    + " / " + (minus.isEmpty() ? "なし" : minus + "↓") + ")";
        }
        return Component.text(
                "性格: " + personality.displayName() + suffix,
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

    private static String names(Set<Ability> abilities) {
        return abilities.stream().map(Ability::displayName)
                .sorted().reduce((a, b) -> a + "・" + b).orElse("");
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

    private static String traitNames(List<String> ids) {
        if (ids.isEmpty()) return "無し";
        return ids.stream()
                .map(WonderfulWolfPlayerInfo::traitName)
                .reduce((a, b) -> a + " / " + b)
                .orElse("無し");
    }

    private static String traitName(String id) {
        try {
            return Trait.valueOf(id).displayName();
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

    private static Component appendAncestor(
            Component base,
            String label,
            Optional<AncestorSnapshot> ancestor) {
        return base.append(Component.newline())
                .append(ancestorLine(label, ancestor));
    }

    private static Component appendParent(
            Component base,
            String label,
            Optional<ParentSnapshot> parent) {
        if (parent.isEmpty()) {
            return base.append(Component.newline())
                    .append(Component.text(label + ": 無し", NamedTextColor.GRAY));
        }
        ParentSnapshot value = parent.orElseThrow();
        String personality;
        try {
            personality = Personality.valueOf(value.personalityId()).displayName();
        } catch (IllegalArgumentException ignored) {
            personality = value.personalityId();
        }
        String genetics = personality
                + " / " + traitNames(value.expressedTraitIds())
                + (value.divineLineageExpressed() ? " / 神の系譜" : "");
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
