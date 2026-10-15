package co.surumene.www.ui;

import java.util.List;
import java.util.Objects;

/** Shared compact ancestry row for GUI lore and chat detail. */
public final class WolfPedigreeLine {
    private WolfPedigreeLine() {}

    public static String format(String name, int generation, String marker,
                                String personality, List<String> traits, String emptyLabel) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(marker, "marker");
        Objects.requireNonNull(traits, "traits");
        Objects.requireNonNull(emptyLabel, "emptyLabel");
        String nature = personality == null ? emptyLabel : personality;
        String names = personality == null || traits.isEmpty()
                ? emptyLabel : String.join("/", traits);
        return name + "(" + generation + ") " + marker + " " + nature + " " + names;
    }
}
