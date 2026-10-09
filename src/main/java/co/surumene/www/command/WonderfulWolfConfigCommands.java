package co.surumene.www.command;

import co.surumene.www.WhatAWonderfulWolfPlugin;
import co.surumene.www.config.WwwConfigLoader;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class WonderfulWolfConfigCommands {
    private final WhatAWonderfulWolfPlugin plugin;
    private final YamlConfiguration defaults;

    WonderfulWolfConfigCommands(WhatAWonderfulWolfPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        try (var stream = Objects.requireNonNull(
                plugin.getResource("config.yml"), "bundled config.yml");
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Unable to load WWW config defaults", ex);
        }
    }

    LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("config")
                .requires(source -> allowed(source.getSender(), "get")
                        || allowed(source.getSender(), "list")
                        || allowed(source.getSender(), "set")
                        || allowed(source.getSender(), "reset"))
                .then(Commands.literal("get")
                        .requires(source -> allowed(source.getSender(), "get"))
                        .then(Commands.argument("path", StringArgumentType.word())
                                .executes(this::get)))
                .then(Commands.literal("set")
                        .requires(source -> allowed(source.getSender(), "set"))
                        .then(Commands.argument("path", StringArgumentType.word())
                                .then(Commands.argument("value",
                                        StringArgumentType.greedyString())
                                        .executes(this::set))))
                .then(Commands.literal("list")
                        .requires(source -> allowed(source.getSender(), "list"))
                        .executes(context -> list(context, ""))
                        .then(Commands.argument("path", StringArgumentType.word())
                                .executes(context -> list(context,
                                        StringArgumentType.getString(context, "path")))))
                .then(Commands.literal("reset")
                        .requires(source -> allowed(source.getSender(), "reset"))
                        .then(Commands.literal("numeric")
                                .executes(context -> resetCategory(context, "numeric", ""))
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .executes(context -> resetCategory(context, "numeric",
                                                StringArgumentType.getString(context, "path")))))
                        .then(Commands.literal("other")
                                .executes(context -> resetCategory(context, "other", ""))
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .executes(context -> resetCategory(context, "other",
                                                StringArgumentType.getString(context, "path")))))
                        .then(Commands.argument("path", StringArgumentType.word())
                                .executes(this::reset)));
    }

    private static boolean allowed(CommandSender sender, String operation) {
        return sender.hasPermission("www.command.config")
                || sender.hasPermission("www.command.config." + operation);
    }

    private int get(CommandContext<CommandSourceStack> context) {
        String path = StringArgumentType.getString(context, "path");
        Object value = plugin.getConfig().get(path);
        if (value == null || value instanceof ConfigurationSection) {
            return error(context, "Unknown configuration leaf: " + path);
        }
        context.getSource().getSender().sendMessage(path + " = " + value);
        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> context, String path) {
        ConfigurationSection config = plugin.getConfig();
        ConfigurationSection section = path.isBlank()
                ? config : config.getConfigurationSection(path);
        if (section == null) {
            return getLeaf(context, path);
        }
        int count = 0;
        for (String key : section.getKeys(true)) {
            String full = path.isBlank() ? key : path + "." + key;
            Object value = config.get(full);
            if (value == null || value instanceof ConfigurationSection) continue;
            context.getSource().getSender().sendMessage(full + " = " + value);
            count++;
        }
        return count > 0 ? Command.SINGLE_SUCCESS : error(context, "No values at " + path);
    }

    private int getLeaf(CommandContext<CommandSourceStack> context, String path) {
        Object value = plugin.getConfig().get(path);
        if (value == null || value instanceof ConfigurationSection) {
            return error(context, "Unknown configuration path: " + path);
        }
        context.getSource().getSender().sendMessage(path + " = " + value);
        return Command.SINGLE_SUCCESS;
    }

    private int set(CommandContext<CommandSourceStack> context) {
        String path = StringArgumentType.getString(context, "path");
        String raw = StringArgumentType.getString(context, "value").trim();
        Object template = defaults.get(path);
        if (template == null || template instanceof ConfigurationSection) {
            return error(context, "Unknown configuration leaf: " + path);
        }
        try {
            Object value = coerce(raw, template);
            commit(Map.of(path, value));
            context.getSource().getSender().sendMessage(
                    Component.text(path + " = " + value, NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            return error(context, exception.getMessage());
        }
    }

    private int reset(CommandContext<CommandSourceStack> context) {
        String path = StringArgumentType.getString(context, "path");
        Object value = defaults.get(path);
        if (value == null || value instanceof ConfigurationSection) {
            return error(context, "Unknown configuration leaf: " + path);
        }
        try {
            commit(Map.of(path, value));
            context.getSource().getSender().sendMessage(
                    Component.text("Reset: " + path, NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            return error(context, exception.getMessage());
        }
    }

    private int resetCategory(CommandContext<CommandSourceStack> context,
                              String kind, String root) {
        ConfigurationSection section = root.isBlank()
                ? defaults : defaults.getConfigurationSection(root);
        if (section == null) {
            return error(context, "Unknown configuration section: " + root);
        }
        Map<String, Object> updates = new LinkedHashMap<>();
        for (String key : section.getKeys(true)) {
            String path = root.isBlank() ? key : root + "." + key;
            Object value = defaults.get(path);
            if (value == null || value instanceof ConfigurationSection) continue;
            boolean numeric = value instanceof Number;
            if (numeric == kind.equals("numeric")) updates.put(path, value);
        }
        if (updates.isEmpty()) return error(context, "No " + kind + " settings at " + root);
        try {
            commit(updates);
            context.getSource().getSender().sendMessage(
                    Component.text("Reset " + updates.size() + " " + kind
                            + " settings", NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            return error(context, exception.getMessage());
        }
    }

    // Validate the entire candidate before saving anything. Restore both the
    // disk file and runtime profile if a subsequent apply fails.
    private void commit(Map<String, Object> updates) {
        FileConfiguration current = plugin.getConfig();
        Map<String, Object> previous = new LinkedHashMap<>();
        for (var entry : updates.entrySet()) {
            previous.put(entry.getKey(), current.get(entry.getKey()));
            current.set(entry.getKey(), entry.getValue());
        }
        try {
            WwwConfigLoader.load(current);
            plugin.saveConfig();
            plugin.reloadWwwConfiguration();
        } catch (RuntimeException error) {
            FileConfiguration rollback = plugin.getConfig();
            for (var entry : previous.entrySet()) {
                rollback.set(entry.getKey(), entry.getValue());
            }
            plugin.saveConfig();
            try {
                plugin.reloadWwwConfiguration();
            } catch (RuntimeException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw error;
        }
    }

    static Object coerce(String text, Object template) {
        Objects.requireNonNull(template, "template");
        if (template instanceof Integer) return Integer.parseInt(text);
        if (template instanceof Long) return Long.parseLong(text);
        if (template instanceof Float) return Float.parseFloat(text);
        if (template instanceof Double) return Double.parseDouble(text);
        if (template instanceof Boolean) {
            if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("boolean must be true or false");
            }
            return Boolean.parseBoolean(text);
        }
        if (template instanceof String) return text;
        throw new IllegalArgumentException("Unsupported configuration value type");
    }

    private static int error(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().getSender().sendMessage(
                Component.text(message == null ? "Invalid configuration" : message,
                        NamedTextColor.RED));
        return 0;
    }
}
