package co.surumene.www.persistence;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class CommanderWolfIndexTest {
    @Test void onlyMatchingOwnersAreReturned() {
        CommanderWolfIndex index = new CommanderWolfIndex();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        index.update(one, alice);
        index.update(two, bob);
        assertEquals(Set.of(one), index.ids(alice));
        assertEquals(Set.of(two), index.ids(bob));
        assertTrue(index.ids(UUID.randomUUID()).isEmpty());
    }

    @Test void reassignmentAndUnloadNeverLeakStaleIds() {
        CommanderWolfIndex index = new CommanderWolfIndex();
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        UUID wolf = UUID.randomUUID();
        index.update(wolf, alice);
        Set<UUID> earlier = index.ids(alice);
        index.update(wolf, bob);
        assertEquals(Set.of(wolf), earlier);
        assertTrue(index.ids(alice).isEmpty());
        assertEquals(Set.of(wolf), index.ids(bob));
        index.remove(wolf);
        assertTrue(index.ids(bob).isEmpty());
    }

    @Test void unownedWolfIsNotIndexedAndDuplicateUpdatesAreSafe() {
        CommanderWolfIndex index = new CommanderWolfIndex();
        UUID owner = UUID.randomUUID(), wolf = UUID.randomUUID();
        index.update(wolf, owner);
        index.update(wolf, owner);
        assertEquals(1, index.ids(owner).size());
        index.update(wolf, null);
        assertTrue(index.ids(owner).isEmpty());
    }
}
