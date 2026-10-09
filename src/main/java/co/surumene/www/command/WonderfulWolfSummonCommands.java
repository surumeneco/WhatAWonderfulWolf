package co.surumene.www.command;

import co.surumene.www.behavior.WonderfulWolfCommandService;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.Mode;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfEntityCreationResult;
import co.surumene.www.ui.PaperItemStackCodec;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

final class WonderfulWolfSummonCommands {
    private static final Pattern RESERVED_UUID =
            Pattern.compile("(?i)(^|[,\\s{])UUID\\s*:");
    private final PaperWonderfulWolfFactory factory;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfAbilityRuntime abilities;
    private final WonderfulWolfCommandService commands;

    WonderfulWolfSummonCommands(PaperWonderfulWolfFactory factory,
                               WonderfulWolfLoadedIndividuals loaded,
                               WonderfulWolfAbilityRuntime abilities,
                               WonderfulWolfCommandService commands) {
        this.factory = Objects.requireNonNull(factory);
        this.loaded = Objects.requireNonNull(loaded);
        this.abilities = Objects.requireNonNull(abilities);
        this.commands = Objects.requireNonNull(commands);
    }

    int execute(CommandContext<CommandSourceStack> context, String raw) {
        CommandSender sender = context.getSource().getSender();
        Wolf wolf = null;
        try {
            Parsed parsed = parse(context.getSource().getLocation(), raw);
            Map<String, Object> options = parsed.customData() == null
                    ? Map.of() : new SnbtLikeParser(parsed.customData()).parseCompound();
            // A bad command must not create an entity.
            validateOptions(options, sender);
            WonderfulWolfEntityCreationResult result = factory.spawnFounder(
                    parsed.location(), FounderOrigin.NATURAL,
                    ThreadLocalRandom.current().nextLong());
            if (result instanceof WonderfulWolfEntityCreationResult.Failure failure) {
                throw new IllegalArgumentException(
                        failure.reason() + ": " + failure.detail());
            }
            wolf = result instanceof WonderfulWolfEntityCreationResult.Success success
                    ? success.wolf()
                    : ((WonderfulWolfEntityCreationResult.AlreadyWonderful) result).wolf();
            applyOptions(wolf, options, sender);
            if (parsed.nbt() != null) {
                if (RESERVED_UUID.matcher(parsed.nbt()).find()) {
                    throw new IllegalArgumentException("UUID override is not allowed");
                }
                if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "minecraft:data merge entity " + wolf.getUniqueId() + " " + parsed.nbt())) {
                    throw new IllegalArgumentException("Vanilla NBT merge failed");
                }
                abilities.refresh(wolf);
            }
            sender.sendMessage(Component.text(
                    "Wonderful Wolfを生成しました: " + wolf.getUniqueId(),
                    NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (Exception error) {
            if (wolf != null) {
                loaded.unregister(wolf);
                wolf.remove();
            }
            sender.sendMessage(Component.text(
                    error.getMessage() == null ? error.getClass().getSimpleName()
                            : error.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private void applyOptions(Wolf wolf, Map<String, Object> options,
                              CommandSender sender) {
        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElseThrow();
        if (options.get("stats") instanceof Map<?, ?> stats) {
            for (Map.Entry<?, ?> entry : stats.entrySet()) {
                Ability ability = WonderfulWolfCommandMutation.ability(
                        String.valueOf(entry.getKey())).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Unknown stat: " + entry.getKey()));
                individual = WonderfulWolfCommandMutation.withAbility(
                        individual, ability, "set", number(entry.getValue()));
            }
        }
        if (options.get("equipment") instanceof Map<?, ?> equipment
                && equipment.containsKey("weapon")) {
            String id = String.valueOf(equipment.get("weapon"));
            Material material = Material.matchMaterial(id);
            if (material == null || material.isAir() || !material.isItem()) {
                throw new IllegalArgumentException("Invalid weapon: " + id);
            }
            individual = individual.withStorage(
                    PaperItemStackCodec.snapshot(ItemStack.of(material)),
                    individual.inventory());
        }
        if (options.containsKey("baby") && Boolean.parseBoolean(
                String.valueOf(options.get("baby")))) {
            wolf.setBaby();
        }
        loaded.saveAndRegister(wolf, individual);
        if (options.get("behavior") instanceof Map<?, ?> behavior
                && behavior.containsKey("mode")) {
            Mode mode = Mode.valueOf(String.valueOf(
                    behavior.get("mode")).toUpperCase(java.util.Locale.ROOT));
            if (mode != Mode.WANDER) {
                if (!(sender instanceof Player player)) {
                    throw new IllegalArgumentException(
                            "Commanded modes require a player as commander");
                }
                if (!wolf.isTamed()) {
                    wolf.setOwner(player);
                }
                loaded.saveAndRegister(wolf, WonderfulWolfCommandMutation.withOwner(
                        loaded.find(wolf.getUniqueId()).orElseThrow(), player.getUniqueId()));
                commands.changeMode(wolf, mode, player.getUniqueId());
            }
        }
        abilities.refresh(wolf);
    }

    private static void validateOptions(Map<String, Object> root, CommandSender sender) {
        for (String key : root.keySet()) {
            if (!List.of("stats", "equipment", "behavior", "baby").contains(key)) {
                throw new IllegalArgumentException("Unknown WWW data key: " + key);
            }
        }
        if (root.containsKey("stats")) {
            if (!(root.get("stats") instanceof Map<?, ?> stats)) {
                throw new IllegalArgumentException("stats must be a compound");
            }
            for (Map.Entry<?, ?> item : stats.entrySet()) {
                Ability ability = WonderfulWolfCommandMutation.ability(
                        String.valueOf(item.getKey())).orElseThrow(
                        () -> new IllegalArgumentException("Unknown stat: " + item.getKey()));
                WonderfulWolfCommandMutation.toNormalized(ability, number(item.getValue()));
            }
        }
        if (root.containsKey("equipment")
                && !(root.get("equipment") instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("equipment must be a compound");
        }
        if (root.get("equipment") instanceof Map<?, ?> equipment) {
            if (equipment.size() > (equipment.containsKey("weapon") ? 1 : 0)) {
                throw new IllegalArgumentException("Only equipment.weapon is supported");
            }
            if (equipment.containsKey("weapon")) {
                Material material = Material.matchMaterial(String.valueOf(equipment.get("weapon")));
                if (material == null || material.isAir() || !material.isItem()) {
                    throw new IllegalArgumentException("Invalid weapon");
                }
            }
        }
        if (root.containsKey("behavior")) {
            if (!(root.get("behavior") instanceof Map<?, ?> behavior)) {
                throw new IllegalArgumentException("behavior must be a compound");
            }
            if (behavior.size() > (behavior.containsKey("mode") ? 1 : 0)) {
                throw new IllegalArgumentException("Only behavior.mode is supported");
            }
            if (behavior.containsKey("mode")) {
                Mode mode = Mode.valueOf(String.valueOf(
                        behavior.get("mode")).toUpperCase(java.util.Locale.ROOT));
                if (mode != Mode.WANDER && !(sender instanceof Player)) {
                    throw new IllegalArgumentException("Commanded mode requires a player");
                }
            }
        }
        if (root.containsKey("baby")
                && !(root.get("baby") instanceof Boolean)) {
            throw new IllegalArgumentException("baby must be boolean");
        }
    }

    private static double number(Object object) {
        double value = object instanceof Number n
                ? n.doubleValue() : Double.parseDouble(String.valueOf(object));
        if (!Double.isFinite(value)) throw new IllegalArgumentException("number must be finite");
        return value;
    }

    static Parsed parse(Location source, String raw) {
        String input = raw == null ? "" : raw.trim();
        int start = CommandText.firstCompoundIndex(input);
        String coordinates = start < 0 ? input : input.substring(0, start).trim();
        String compounds = start < 0 ? "" : input.substring(start);
        Location location = source.clone();
        if (!coordinates.isBlank()) {
            String[] position = coordinates.split("\\s+");
            if (position.length != 3) {
                throw new IllegalArgumentException("Position must contain x y z");
            }
            location = parsePosition(source, position);
        }
        List<String> blocks = CommandText.extractTopLevelCompounds(compounds);
        if (blocks.size() > 2) {
            throw new IllegalArgumentException("At most two compounds: WWW data and NBT");
        }
        String custom = null, nbt = null;
        if (blocks.size() == 1) {
            Map<String, Object> root = new SnbtLikeParser(blocks.getFirst()).parseCompound();
            boolean isCustom = root.keySet().stream().anyMatch(
                    key -> List.of("stats", "equipment", "behavior", "baby").contains(key));
            if (isCustom) custom = blocks.getFirst();
            else nbt = blocks.getFirst();
        } else if (blocks.size() == 2) {
            custom = blocks.get(0);
            nbt = blocks.get(1);
        }
        return new Parsed(location, custom, nbt);
    }

    private static Location parsePosition(Location origin, String[] xyz) {
        boolean local = xyz[0].startsWith("^") || xyz[1].startsWith("^") || xyz[2].startsWith("^");
        if (local) {
            for (String part : xyz) if (!part.startsWith("^")) {
                throw new IllegalArgumentException("Local coordinates must all use ^");
            }
            Vector forward = origin.getDirection().normalize();
            Vector left = new Vector(0, 1, 0).crossProduct(forward).normalize();
            Vector up = forward.clone().crossProduct(left).normalize();
            return origin.clone().add(left.multiply(localValue(xyz[0])))
                    .add(up.multiply(localValue(xyz[1])))
                    .add(forward.multiply(localValue(xyz[2])));
        }
        return new Location(origin.getWorld(),
                coordinate(xyz[0], origin.getX()),
                coordinate(xyz[1], origin.getY()),
                coordinate(xyz[2], origin.getZ()),
                origin.getYaw(), origin.getPitch());
    }

    private static double coordinate(String token, double base) {
        return token.startsWith("~")
                ? base + (token.length() == 1 ? 0 : Double.parseDouble(token.substring(1)))
                : Double.parseDouble(token);
    }

    private static double localValue(String token) {
        return token.length() == 1 ? 0 : Double.parseDouble(token.substring(1));
    }

    record Parsed(Location location, String customData, String nbt) {}
}
