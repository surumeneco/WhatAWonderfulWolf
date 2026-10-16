package co.surumene.www.persistence;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ManagementGuiRosterTest {
    @Test void onlyOpenViewersBlockScavenging() {
        ManagementGuiRoster roster = new ManagementGuiRoster();
        UUID wolf = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        assertFalse(roster.isOpen(wolf));
        roster.open(wolf, viewer);
        assertTrue(roster.isOpen(wolf));
        roster.close(wolf, viewer);
        assertFalse(roster.isOpen(wolf));
    }

    @Test void oldViewerCannotUnlockReplacement() {
        ManagementGuiRoster roster = new ManagementGuiRoster();
        UUID wolf = UUID.randomUUID();
        UUID first = UUID.randomUUID(), next = UUID.randomUUID();
        roster.open(wolf, first);
        roster.open(wolf, next);
        roster.close(wolf, first);
        assertTrue(roster.isOpen(wolf));
        roster.remove(wolf);
        assertFalse(roster.isOpen(wolf));
    }
}
