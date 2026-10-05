package co.surumene.www.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class WwwConfigLoader {
    private WwwConfigLoader() {}

    public static WwwConfig loadDefaults() {
        InputStream stream = WwwConfigLoader.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("bundled config.yml is missing");
        }
        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return load(YamlConfiguration.loadConfiguration(reader));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to read bundled config.yml", e);
        }
    }

    public static WwwConfig load(ConfigurationSection root) {
        ConfigurationSection founder = section(root, "founder-target");
        ConfigurationSection genome = section(root, "genome-profile");
        ConfigurationSection runtime = section(root, "runtime");

        WwwConfig.FounderTarget founderTarget = new WwwConfig.FounderTarget(
                new WwwConfig.FounderKind(distribution(founder, "natural.abilities")),
                new WwwConfig.FounderKind(distribution(founder, "wolf-trap.abilities")),
                distribution(founder, "development"),
                distribution(founder, "relationship"));

        ConfigurationSection decoder = section(genome, "decoder");
        WwwConfig.Decoder decoderConfig = new WwwConfig.Decoder(
                new WwwConfig.PersonalityDecoder(
                        number(decoder, "personality.mean"),
                        number(decoder, "personality.sigma"),
                        number(decoder, "personality.dominant-gap-sigma"),
                        number(decoder, "personality.neutral-factor-sigma"),
                        number(decoder, "personality.neutral-spread-sigma")),
                new WwwConfig.TraitDecoder(
                        number(decoder, "trait.expression-threshold"),
                        number(decoder, "trait.strong-gap")),
                new WwwConfig.InjuryDecoder(
                        number(decoder, "injury.expression-threshold"),
                        number(decoder, "injury.onset-max-game-days"),
                        number(decoder, "injury.severity-rank-min"),
                        number(decoder, "injury.severity-rank-max")),
                new WwwConfig.DivineDecoder(
                        number(decoder, "divine.min-haplotype-score"),
                        number(decoder, "divine.total-score")));

        ConfigurationSection synth = section(genome, "synthesizer");
        WwwConfig.Synthesizer synthesizer = new WwwConfig.Synthesizer(
                new WwwConfig.InjurySynthesizer(
                        number(synth, "injury.blocks-lambda"),
                        integer(synth, "injury.blocks-max"),
                        integer(synth, "injury.genes-per-block"),
                        number(synth, "injury.load-mean"),
                        number(synth, "injury.load-standard-deviation"),
                        number(synth, "injury.load-min"),
                        number(synth, "injury.load-max"),
                        number(synth, "injury.bilateral-probability"),
                        number(synth, "injury.onset-mean"),
                        number(synth, "injury.onset-standard-deviation"),
                        number(synth, "injury.severity-mean"),
                        number(synth, "injury.severity-standard-deviation")),
                new WwwConfig.DivineSynthesizer(
                        number(synth, "divine.supply-probability"),
                        integer(synth, "divine.genes-min"),
                        integer(synth, "divine.genes-center"),
                        integer(synth, "divine.genes-max"),
                        number(synth, "divine.major-haplotype-probability"),
                        number(synth, "divine.gene-d-min"),
                        number(synth, "divine.gene-d-max")),
                new WwwConfig.ExtraordinarySynthesizer(
                        integer(synth, "extraordinary.genes-per-target-min"),
                        integer(synth, "extraordinary.genes-per-target-max"),
                        integer(synth, "extraordinary.blocks-per-target-min"),
                        integer(synth, "extraordinary.blocks-per-target-max"),
                        number(synth, "extraordinary.transfer-max")),
                number(synth, "cancellation-min"),
                number(synth, "cancellation-max"),
                number(synth, "high-target-headroom"),
                new WwwConfig.GenesPerTarget(
                        range(synth, "genes-per-target", "ability"),
                        range(synth, "genes-per-target", "personality"),
                        range(synth, "genes-per-target", "development"),
                        range(synth, "genes-per-target", "relationship"),
                        range(synth, "genes-per-target", "trait")),
                number(synth, "chromosome-length-standard-deviation-ratio"),
                number(synth, "chromosome-length-min-ratio"),
                number(synth, "chromosome-length-max-ratio"),
                integer(synth, "direct-genes-soft-min"),
                integer(synth, "direct-genes-soft-max"),
                integer(synth, "direct-genes-hard-max"),
                integer(synth, "regulation-genes-min"),
                integer(synth, "regulation-genes-center"),
                integer(synth, "regulation-genes-max"),
                integer(synth, "regulation-genes-hard-max"),
                integer(synth, "recognizable-genes-hard-max"),
                number(synth, "recognizable-region-max-ratio"),
                number(synth, "noncoding-region-min-ratio"),
                new WwwConfig.Relay(
                        number(synth, "relay.attachment-min-ratio"),
                        number(synth, "relay.attachment-max-ratio"),
                        number(synth, "relay.normal-secondary-min-ratio"),
                        number(synth, "relay.normal-secondary-max-ratio"),
                        number(synth, "relay.strong-secondary-min-ratio"),
                        number(synth, "relay.strong-secondary-max-ratio"),
                        number(synth, "relay.positive-ratio")));

        ConfigurationSection breeding = section(genome, "breeding-policy");
        WwwConfig.BreedingPolicy breedingPolicy = new WwwConfig.BreedingPolicy(
                new WwwConfig.DirectInheritance(
                        number(breeding, "direct-inheritance.weak-prefer-probability"),
                        number(breeding, "direct-inheritance.crossover-weight-inside-block")),
                new WwwConfig.WildTrait(
                        number(breeding, "wild-trait.weak-parent-multiplier"),
                        number(breeding, "wild-trait.strong-parent-multiplier")));

        WwwConfig.Runtime runtimeConfig = new WwwConfig.Runtime(
                new WwwConfig.Combat(integer(runtime, "combat.retreat-min-ticks")),
                new WwwConfig.ActionDistance(
                        number(runtime, "action-distance.narrow-blocks"),
                        number(runtime, "action-distance.normal-blocks"),
                        number(runtime, "action-distance.wide-blocks"),
                        number(runtime, "action-distance.very-wide-blocks")),
                new WwwConfig.WanWand(number(runtime, "wan-wand.target-max-distance-blocks")),
                new WwwConfig.Age(
                        text(runtime, "age.clock-world"),
                        number(runtime, "age.base-growth-game-days"),
                        number(runtime, "age.base-peak-duration-game-days"),
                        number(runtime, "age.base-aging-duration-game-days"),
                        new WwwConfig.AgeSensitivity(
                                number(runtime, "age.sensitivity.health"),
                                number(runtime, "age.sensitivity.defense"),
                                number(runtime, "age.sensitivity.patience"),
                                number(runtime, "age.sensitivity.size"),
                                number(runtime, "age.sensitivity.inventory"),
                                number(runtime, "age.sensitivity.movement-speed"),
                                number(runtime, "age.sensitivity.jump"),
                                number(runtime, "age.sensitivity.step-height"),
                                number(runtime, "age.sensitivity.attack-damage"),
                                number(runtime, "age.sensitivity.attack-speed"))),
                new WwwConfig.Relationship(
                        integer(runtime, "relationship.passive-increase-interval-ticks"),
                        number(runtime, "relationship.passive-increase-probability")),
                new WwwConfig.PersonalityRuntime(number(runtime, "personality.modifier-rate")),
                new WwwConfig.Traits(
                        number(runtime, "traits.aura-radius-blocks"),
                        number(runtime, "traits.watchman-distance-multiplier"),
                        integer(runtime, "traits.watchman-warning-min-ticks"),
                        integer(runtime, "traits.watchman-warning-max-ticks")),
                new WwwConfig.Spawn(
                        number(runtime, "spawn.natural-conversion-probability"),
                        number(runtime, "spawn.wolf-trap-lightning-probability"),
                        number(runtime, "spawn.wolf-trap-activation-radius-blocks")));

        return WwwConfigValidator.validate(new WwwConfig(
                integer(root, "config-version"),
                founderTarget,
                new WwwConfig.GenomeProfile(decoderConfig, synthesizer, breedingPolicy),
                runtimeConfig));
    }

    private static WwwConfig.Distribution distribution(ConfigurationSection root, String path) {
        return new WwwConfig.Distribution(
                number(root, path + ".mean"),
                number(root, path + ".standard-deviation"));
    }

    private static WwwConfig.Range range(ConfigurationSection root, String prefix, String name) {
        String path = prefix + "." + name;
        return new WwwConfig.Range(
                integer(root, path + "-min"),
                integer(root, path + "-center"),
                integer(root, path + "-max"));
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        ConfigurationSection value = root.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("missing configuration section: " + path);
        }
        return value;
    }

    private static double number(ConfigurationSection root, String path) {
        Object value = root.get(path);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("configuration value must be numeric: " + path);
        }
        return number.doubleValue();
    }

    private static int integer(ConfigurationSection root, String path) {
        Object value = root.get(path);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("configuration value must be an integer: " + path);
        }
        double raw = number.doubleValue();
        int parsed = number.intValue();
        if (!Double.isFinite(raw) || raw != parsed) {
            throw new IllegalArgumentException("configuration value must be an integer: " + path);
        }
        return parsed;
    }

    private static String text(ConfigurationSection root, String path) {
        Object value = root.get(path);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("configuration value must be non-blank text: " + path);
        }
        return text;
    }
}
