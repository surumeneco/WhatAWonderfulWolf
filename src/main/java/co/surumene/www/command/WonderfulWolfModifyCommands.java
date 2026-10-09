package co.surumene.www.command;

import co.surumene.www.behavior.WonderfulWolfCommandService;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.Mode;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class WonderfulWolfModifyCommands {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfAbilityRuntime abilities;
    private final WonderfulWolfCommandService commands;

    WonderfulWolfModifyCommands(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            WonderfulWolfCommandService commands) {
        this.loaded = Objects.requireNonNull(loaded);
        this.abilities = Objects.requireNonNull(abilities);
        this.commands = Objects.requireNonNull(commands);
    }

    LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("modify")
                .requires(source -> source.getSender().hasPermission("www.command.modify"))
                .then(Commands.argument("targets", ArgumentTypes.entities())
                        .then(Commands.literal("set")
                                .then(valueArgument("set")))
                        .then(Commands.literal("add")
                                .then(valueArgument("add"))));
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String>
    valueArgument(String operation) {
        return Commands.argument("field", StringArgumentType.word())
                .suggests((context, suggestions) -> {
                    for (String field : FIELDS) suggestions.suggest(field);
                    return suggestions.buildFuture();
                })
                .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(context -> modify(context, operation)));
    }

    private static final List<String> FIELDS = List.of(
            "max-health", "size", "movement-speed", "jump", "step-height",
            "attack-damage", "attack-speed", "defense", "patience", "inventory", "mode");

    private int modify(CommandContext<CommandSourceStack> context, String operation) {
        CommandSender sender = context.getSource().getSender();
        try {
            String field = StringArgumentType.getString(context, "field")
                    .toLowerCase(Locale.ROOT).replace('_', '-');
            String value = StringArgumentType.getString(context, "value").trim();
            EntitySelectorArgumentResolver selector = context.getArgument(
                    "targets", EntitySelectorArgumentResolver.class);
            List<Wolf> targets = selector.resolve(context.getSource()).stream()
                    .filter(Wolf.class::isInstance)
                    .map(Wolf.class::cast)
                    .filter(wolf -> loaded.find(wolf.getUniqueId()).isPresent()
                            || loaded.register(wolf) instanceof RestoreResult.Success)
                    .toList();
            if (targets.isEmpty() || targets.size() > 10) {
                throw new IllegalArgumentException("Select 1..10 Wonderful Wolves");
            }
            if (field.equals("mode")) {
                if (!operation.equals("set")) {
                    throw new IllegalArgumentException("mode supports set only");
                }
                Mode mode = Mode.valueOf(value.toUpperCase(Locale.ROOT));
                UUID issuer = sender instanceof Player player
                        ? player.getUniqueId() : null;
                for (Wolf wolf : targets) {
                    WonderfulWolfIndividual individual =
                            loaded.find(wolf.getUniqueId()).orElseThrow();
                    UUID actor = issuer != null ? issuer
                            : individual.commanderId().orElseGet(
                                    () -> individual.ownerId().orElse(null));
                    if (actor == null && mode != Mode.WANDER) {
                        throw new IllegalArgumentException(
                                "a player or an existing owner/commander is required");
                    }
                }
                for (Wolf wolf : targets) {
                    WonderfulWolfIndividual individual =
                            loaded.find(wolf.getUniqueId()).orElseThrow();
                    UUID actor = issuer != null ? issuer
                            : individual.commanderId().orElseGet(
                                    () -> individual.ownerId().orElse(new UUID(0L, 0L)));
                    commands.changeMode(wolf, mode, actor);
                }
            } else {
                Ability ability = WonderfulWolfCommandMutation.ability(field)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown field: " + field));
                double operand = Double.parseDouble(value);
                // Validate every target before mutating persistent state.
                java.util.LinkedHashMap<Wolf, WonderfulWolfIndividual> updated =
                        new java.util.LinkedHashMap<>();
                for (Wolf wolf : targets) {
                    updated.put(wolf, WonderfulWolfCommandMutation.withAbility(
                            loaded.find(wolf.getUniqueId()).orElseThrow(),
                            ability, operation, operand));
                }
                java.util.LinkedHashMap<Wolf, WonderfulWolfIndividual> previous =
                        new java.util.LinkedHashMap<>();
                for (Wolf wolf : updated.keySet()) {
                    previous.put(wolf, loaded.find(wolf.getUniqueId()).orElseThrow());
                }
                try {
                    for (Map.Entry<Wolf, WonderfulWolfIndividual> entry : updated.entrySet()) {
                        loaded.saveAndRegister(entry.getKey(), entry.getValue());
                        abilities.refresh(entry.getKey());
                        if (entry.getKey().isAdult() && abilities.find(
                                entry.getKey().getUniqueId()).isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Paper cannot apply requested ability: " + ability);
                        }
                    }
                } catch (RuntimeException failure) {
                    // Restore all prior persistent snapshots, including on earlier
                    // targets of a multi-entity selector.
                    for (Map.Entry<Wolf, WonderfulWolfIndividual> entry : previous.entrySet()) {
                        try {
                            loaded.saveAndRegister(entry.getKey(), entry.getValue());
                            abilities.refresh(entry.getKey());
                        } catch (RuntimeException rollbackError) {
                            failure.addSuppressed(rollbackError);
                        }
                    }
                    throw failure;
                }
            }
            sender.sendMessage(Component.text(
                    "Wonderful Wolf " + targets.size() + "個体を変更しました。",
                    NamedTextColor.GREEN));
            return targets.size();
        } catch (Exception error) {
            sender.sendMessage(Component.text(
                    error.getMessage() == null ? error.getClass().getSimpleName()
                            : error.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }
}
