package co.surumene.www.founder;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.SynthesisTarget;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WonderfulWolfSynthesisTarget implements SynthesisTarget {
    private final FounderOrigin origin;
    private final Map<GenomeAddress, Double> continuousTargets;
    private final Map<Ability, Double> baseAbilityTargets;
    private final Map<Ability, Double> extraordinaryTargets;

    private WonderfulWolfSynthesisTarget(
            FounderOrigin origin,
            Map<GenomeAddress, Double> continuousTargets,
            Map<Ability, Double> baseAbilityTargets,
            Map<Ability, Double> extraordinaryTargets) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.continuousTargets = Map.copyOf(continuousTargets);
        this.baseAbilityTargets = immutableAbilityMap(baseAbilityTargets);
        this.extraordinaryTargets = immutableAbilityMap(extraordinaryTargets);
    }

    public static WonderfulWolfSynthesisTarget from(
            FounderTarget founder,
            WwwConfig config,
            GenomeRandom random) {
        Objects.requireNonNull(founder, "founder");
        WwwConfig validated = WwwConfigValidator.validate(Objects.requireNonNull(config, "config"));
        Objects.requireNonNull(random, "random");

        Map<GenomeAddress, Double> continuous = new LinkedHashMap<>();
        EnumMap<Ability, Double> base = new EnumMap<>(Ability.class);
        EnumMap<Ability, Double> extraordinary = new EnumMap<>(Ability.class);

        double transferMax = validated.genomeProfile().synthesizer().extraordinary().transferMax();
        for (Ability ability : Ability.values()) {
            double finalTarget = founder.abilities().get(ability);
            double b = finalTarget;
            double e = 0.0;
            if (founder.origin() == FounderOrigin.WOLF_TRAP && finalTarget > 1.0) {
                double maxTransfer = Math.min(transferMax, 1.5 - finalTarget);
                double transfer = maxTransfer <= 0.0 ? 0.0 : maxTransfer * random.nextDouble();
                b = 1.0 - transfer;
                e = finalTarget - b;
            }
            base.put(ability, b);
            extraordinary.put(ability, e);
            continuous.put(new GenomeAddress(0x00, ability.targetId()), b);
        }

        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            continuous.put(
                    new GenomeAddress(0x01, factor.targetId()),
                    founder.developmentFactors().get(factor));
        }
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            continuous.put(
                    new GenomeAddress(0x03, factor.targetId()),
                    founder.personalityFactors().get(factor));
        }

        addTraitTargets(founder, validated, random, continuous);

        continuous.put(new GenomeAddress(0x06, 0x00), founder.relationship().initialAffinityScore());
        continuous.put(new GenomeAddress(0x06, 0x01), founder.relationship().affinityChangeScore());

        return new WonderfulWolfSynthesisTarget(
                founder.origin(), continuous, base, extraordinary);
    }

    public FounderOrigin origin() {
        return origin;
    }

    @Override
    public Map<GenomeAddress, Double> continuousTargets() {
        return continuousTargets;
    }

    public Map<Ability, Double> baseAbilityTargets() {
        return baseAbilityTargets;
    }

    public Map<Ability, Double> extraordinaryTargets() {
        return extraordinaryTargets;
    }

    @Override
    public boolean isSatisfied(DecodedGenome decoded, double tolerance) {
        Objects.requireNonNull(decoded, "decoded");
        if (!Double.isFinite(tolerance) || tolerance < 0.0) {
            throw new IllegalArgumentException("tolerance must be finite and >= 0");
        }
        for (var entry : continuousTargets.entrySet()) {
            GenomeAddress address = entry.getKey();
            AddressAggregate aggregate = decoded.aggregate(address);
            double actual = switch (address.type()) {
                case 0x01, 0x03, 0x06 -> centeredScore(aggregate);
                default -> aggregate.score();
            };
            if (StrictMath.abs(actual - entry.getValue()) > tolerance) {
                return false;
            }
        }
        for (Ability ability : Ability.values()) {
            double expected = extraordinaryTargets.get(ability);
            double actual = 0.5 * decoded.aggregate(
                    new GenomeAddress(0x07, ability.targetId())).score();
            if (StrictMath.abs(actual - expected) > tolerance) {
                return false;
            }
        }
        return true;
    }

    private static void addTraitTargets(
            FounderTarget founder,
            WwwConfig config,
            GenomeRandom random,
            Map<GenomeAddress, Double> continuous) {
        double threshold = config.genomeProfile().decoder().trait().expressionThreshold();
        double gap = config.genomeProfile().decoder().trait().strongGap();
        double inactive = Math.max(0.0, threshold - Math.max(gap, 0.05));
        double weak = Math.min(1.0, threshold + Math.max(0.01, gap * 0.25));
        double strong = Math.min(1.0, weak + gap + Math.max(0.01, gap * 0.25));

        Map<Trait, TraitStrength> expressed = new EnumMap<>(Trait.class);
        for (ExpressedTrait trait : founder.traits()) {
            expressed.put(trait.trait(), trait.strength());
        }
        Trait runnerUp = null;
        if (expressed.size() == 1 && expressed.containsValue(TraitStrength.STRONG)) {
            List<Trait> candidates = java.util.Arrays.stream(Trait.values())
                    .filter(trait -> !expressed.containsKey(trait))
                    .toList();
            runnerUp = candidates.get(random.nextInt(candidates.size()));
        }
        for (Trait trait : Trait.values()) {
            TraitStrength strength = expressed.get(trait);
            double value = strength == null
                    ? (trait == runnerUp ? weak : inactive)
                    : strength == TraitStrength.STRONG ? strong : weak;
            continuous.put(new GenomeAddress(0x04, trait.targetId()), value);
        }
    }

    private static double centeredScore(AddressAggregate aggregate) {
        double negative = 1.0 - aggregate.negativeSurvival();
        return Math.max(0.0, Math.min(1.0,
                0.5 + 0.5 * (aggregate.positiveSaturation() - negative)));
    }

    private static Map<Ability, Double> immutableAbilityMap(Map<Ability, Double> source) {
        EnumMap<Ability, Double> copy = new EnumMap<>(Ability.class);
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }
}
