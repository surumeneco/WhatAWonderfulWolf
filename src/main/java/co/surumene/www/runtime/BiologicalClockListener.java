package co.surumene.www.runtime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ClockTimeSkipEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.Objects;

public final class BiologicalClockListener implements Listener {
    private final BiologicalClock clock;

    public BiologicalClockListener(BiologicalClock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        if (!event.getWorld().getName().equals(clock.worldName())) {
            return;
        }

        ClockTimeSkipEvent.SkipReason reason = event.getSkipReason();
        if (reason == ClockTimeSkipEvent.SkipReason.COMMAND
                || reason == ClockTimeSkipEvent.SkipReason.CUSTOM) {
            clock.recordIgnoredSkip(event.getSkipAmount());
        }
    }
}
