package co.surumene.www.command;

import co.surumene.www.WhatAWonderfulWolfPlugin;
import co.surumene.www.breeding.WonderfulWolfOffspringService;
import co.surumene.www.behavior.WonderfulWolfCommandService;
import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfEntityCreationResult;
import co.surumene.wgl.api.BreedingParentSource;
import co.surumene.wgl.api.DiploidGenome;
import co.surumene.wgl.api.GenomeEngine;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class WonderfulWolfAdminCommands {
    private static final int MAX_TARGETS = 10;

    private final WhatAWonderfulWolfPlugin plugin;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfAbilityRuntime abilities;
    private final GenomeEngine engine;
    private final PaperWonderfulWolfFactory factory;
    private final WonderfulWolfOffspringService offspringService;
    private final WonderfulWolfAdminInfo info;
    private final WonderfulWolfConfigCommands configCommands;
    private final WonderfulWolfModifyCommands modifyCommands;
    private final WonderfulWolfSummonCommands summonCommands;

    public WonderfulWolfAdminCommands(
            WhatAWonderfulWolfPlugin plugin,
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            GenomeEngine engine,
            PaperWonderfulWolfFactory factory,
            WonderfulWolfOffspringService offspringService,
            WonderfulWolfAdminInfo info,
            WonderfulWolfCommandService commandService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.offspringService =
                Objects.requireNonNull(offspringService, "offspringService");
        this.info = Objects.requireNonNull(info, "info");
        this.configCommands = new WonderfulWolfConfigCommands(plugin);
        this.modifyCommands = new WonderfulWolfModifyCommands(loaded, abilities, commandService);
        this.summonCommands = new WonderfulWolfSummonCommands(factory, loaded, abilities, commandService);
    }

    public com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal("www");
        root.then(infoNode());
        root.then(genomeNode());
        root.then(summonNode());
        root.then(modifyCommands.build());
        root.then(configCommands.build());
        root.then(Commands.literal("reload")
                .requires(source -> source.getSender()
                        .hasPermission("www.command.reload"))
                .executes(this::reload));
        return root.build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> infoNode() {
        return Commands.literal("info")
                .requires(source -> source.getSender()
                        .hasPermission("www.command.info"))
                .executes(this::infoNearest)
                .then(Commands.argument(
                                "targets",
                                ArgumentTypes.entities())
                        .executes(this::infoSelected));
    }

    private LiteralArgumentBuilder<CommandSourceStack> genomeNode() {
        return Commands.literal("genome")
                .requires(source -> source.getSender()
                        .hasPermission("www.command.genome"))
                .then(Commands.literal("get")
                        .then(Commands.argument(
                                        "targets",
                                        ArgumentTypes.entities())
                                .executes(this::genomeGet)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> summonNode() {
        return Commands.literal("summon")
                .requires(source -> source.getSender()
                        .hasPermission("www.command.summon"))
                .executes(context -> summonCommands.execute(context, ""))
                .then(Commands.literal("genome")
                        .then(Commands.argument(
                                        "format",
                                        StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("text");
                                    builder.suggest("bits");
                                    builder.suggest("hex");
                                    builder.suggest("dna");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument(
                                                "haplotypes",
                                                StringArgumentType.greedyString())
                                        .executes(this::summonGenome))))
                .then(Commands.literal("offspring")
                        .then(Commands.literal("parent")
                                .then(Commands.argument(
                                                "sourceA",
                                                StringArgumentType.word())
                                        .then(Commands.literal("parent")
                                                .then(Commands.argument(
                                                                "sourceB",
                                                                StringArgumentType.word())
                                                        .executes(
                                                                this::summonOffspring))))))
                .then(Commands.argument("arguments", StringArgumentType.greedyString())
                        .executes(context -> summonCommands.execute(context,
                                StringArgumentType.getString(context, "arguments"))));
    }

    private int infoNearest(
            CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Location origin = context.getSource().getLocation();
        Wolf nearest = origin.getWorld()
                .getEntitiesByClass(Wolf.class)
                .stream()
                .filter(Wolf::isValid)
                .filter(this::ensureWonderful)
                .min(Comparator.comparingDouble(
                        wolf -> wolf.getLocation()
                                .distanceSquared(origin)))
                .orElse(null);
        if (nearest == null) {
            error(sender, "有効なWonderful Wolfが見つかりません。");
            return 0;
        }
        sendInfo(sender, List.of(nearest));
        return Command.SINGLE_SUCCESS;
    }

    private int infoSelected(
            CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            List<Wolf> targets = resolveTargets(context);
            if (!validateTargetCount(sender, targets)) return 0;
            sendInfo(sender, targets);
            return targets.size();
        } catch (Exception error) {
            error(sender, safeMessage(error));
            return 0;
        }
    }

    private int genomeGet(
            CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            List<Wolf> targets = resolveTargets(context);
            if (!validateTargetCount(sender, targets)) return 0;

            for (int i = 0; i < targets.size(); i++) {
                Wolf wolf = targets.get(i);
                var individual = loaded.find(wolf.getUniqueId())
                        .orElseThrow();
                GenomeAdminCodec.RawBitsView raw =
                        GenomeAdminCodec.rawBits(individual.genome());
                sender.sendMessage(Component.text(
                        "◆ Genome " + (i + 1) + "/" + targets.size()
                                + " — " + wolf.getUniqueId(),
                        NamedTextColor.GOLD));
                sender.sendMessage(Component.text(
                        "Format Version: "
                                + individual.genome().genomeFormatVersion(),
                        NamedTextColor.GRAY));
                sender.sendMessage(Component.text(
                        "ParentSource(Base64 WGLP): "
                                + OffspringParentSourceCodec.encode(
                                        new BreedingParentSource.DiploidParent(
                                                individual.genome()),
                                        engine),
                        NamedTextColor.GRAY));
                sender.sendMessage(Component.text(
                        "Lengths A/B: " + raw.chromosomeLengths(),
                        NamedTextColor.GRAY));
                sender.sendMessage(Component.text(
                        "A(bits): " + raw.haplotypeA(),
                        NamedTextColor.WHITE));
                sender.sendMessage(Component.text(
                        "B(bits): " + raw.haplotypeB(),
                        NamedTextColor.WHITE));
            }
            return targets.size();
        } catch (Exception error) {
            error(sender, safeMessage(error));
            return 0;
        }
    }

    private int summonGenome(
            CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            GenomeInputParser.Format format =
                    GenomeInputParser.Format.parse(
                            StringArgumentType.getString(
                                    context,
                                    "format"));
            List<String> haplotypes =
                    GenomeCommandArguments.parse(
                            StringArgumentType.getString(
                                    context,
                                    "haplotypes"));
            DiploidGenome genome = GenomeAdminCodec.parseGenome(
                    format,
                    haplotypes.get(0),
                    haplotypes.size() == 2
                            ? haplotypes.get(1)
                            : null,
                    engine.sequenceCodec());

            WonderfulWolfEntityCreationResult result =
                    factory.spawnGenome(
                            context.getSource().getLocation(),
                            genome);
            if (result instanceof WonderfulWolfEntityCreationResult.Failure failure) {
                error(
                        sender,
                        failure.reason() + ": " + failure.detail());
                return 0;
            }

            Wolf wolf = result instanceof WonderfulWolfEntityCreationResult.Success success
                    ? success.wolf()
                    : ((WonderfulWolfEntityCreationResult.AlreadyWonderful) result)
                        .wolf();
            sender.sendMessage(Component.text(
                    "Wonderful Wolfを任意Genomeから生成しました: "
                            + wolf.getUniqueId(),
                    NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (Exception error) {
            error(sender, safeMessage(error));
            return 0;
        }
    }

    private int summonOffspring(
            CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            BreedingParentSource sourceA =
                    OffspringParentSourceCodec.decode(
                            StringArgumentType.getString(
                                    context,
                                    "sourceA"),
                            engine);
            BreedingParentSource sourceB =
                    OffspringParentSourceCodec.decode(
                            StringArgumentType.getString(
                                    context,
                                    "sourceB"),
                            engine);

            WonderfulWolfOffspringService.Result bred =
                    offspringService.breed(
                            sourceA,
                            sourceB,
                            java.util.concurrent.ThreadLocalRandom.current()
                                    .nextLong());
            if (bred instanceof WonderfulWolfOffspringService.Result.Failure failure) {
                error(
                        sender,
                        failure.reason()
                                + (failure.detail().isBlank()
                                    ? ""
                                    : ": " + failure.detail()));
                return 0;
            }

            WonderfulWolfEntityCreationResult created =
                    factory.spawnGenome(
                            context.getSource().getLocation(),
                            ((WonderfulWolfOffspringService.Result.Success) bred)
                                    .genome());
            if (created instanceof WonderfulWolfEntityCreationResult.Failure failure) {
                error(
                        sender,
                        failure.reason() + ": " + failure.detail());
                return 0;
            }

            Wolf wolf = created instanceof WonderfulWolfEntityCreationResult.Success success
                    ? success.wolf()
                    : ((WonderfulWolfEntityCreationResult.AlreadyWonderful) created)
                        .wolf();
            sender.sendMessage(Component.text(
                    "Wonderful Wolfを親Genomeから生成しました: "
                            + wolf.getUniqueId(),
                    NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (Exception error) {
            error(sender, safeMessage(error));
            return 0;
        }
    }

    private int reload(
            CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        try {
            plugin.reloadWwwConfiguration();
            sender.sendMessage(Component.text(
                    "What a Wonderful Wolfの設定を再読込しました。",
                    NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (Exception error) {
            error(sender, "設定再読込に失敗しました: " + safeMessage(error));
            return 0;
        }
    }

    private List<Wolf> resolveTargets(
            CommandContext<CommandSourceStack> context)
            throws Exception {
        EntitySelectorArgumentResolver resolver =
                context.getArgument(
                        "targets",
                        EntitySelectorArgumentResolver.class);
        Collection<Entity> resolved =
                resolver.resolve(context.getSource());
        return resolved.stream()
                .filter(Wolf.class::isInstance)
                .map(Wolf.class::cast)
                .filter(this::ensureWonderful)
                .toList();
    }

    private boolean ensureWonderful(Wolf wolf) {
        if (loaded.find(wolf.getUniqueId()).isPresent()) {
            return true;
        }
        return loaded.register(wolf)
                instanceof RestoreResult.Success;
    }

    private boolean validateTargetCount(
            CommandSender sender,
            List<Wolf> targets) {
        if (targets.isEmpty()) {
            error(sender, "対象にWonderful Wolfが含まれていません。");
            return false;
        }
        if (targets.size() > MAX_TARGETS) {
            error(
                    sender,
                    "対象が多すぎます。最大"
                            + MAX_TARGETS
                            + "個体です。");
            return false;
        }
        return true;
    }

    private void sendInfo(
            CommandSender sender,
            List<Wolf> targets) {
        for (int i = 0; i < targets.size(); i++) {
            Wolf wolf = targets.get(i);
            abilities.refresh(wolf);
            var individual = loaded.find(wolf.getUniqueId())
                    .orElseThrow();
            if (i > 0) {
                sender.sendMessage(Component.text(
                        "────────────────────────",
                        NamedTextColor.DARK_GRAY));
            }
            sender.sendMessage(info.render(
                    wolf,
                    individual,
                    abilities.find(wolf.getUniqueId()),
                    i + 1,
                    targets.size()));
        }
    }

    private static void error(
            CommandSender sender,
            String message) {
        sender.sendMessage(Component.text(
                message == null || message.isBlank()
                        ? "不明なエラーが発生しました。"
                        : message,
                NamedTextColor.RED));
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName()
                        + ": "
                        + message;
    }
}
