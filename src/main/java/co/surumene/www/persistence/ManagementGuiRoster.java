package co.surumene.www.persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public final class ManagementGuiRoster {
  private final Map<UUID, UUID> viewers = new HashMap<>();
  public void open(UUID wolf, UUID viewer) { viewers.put(wolf, viewer); }
  public void close(UUID wolf, UUID viewer) { viewers.remove(wolf, viewer); }
  public boolean isOpen(UUID wolf) { return viewers.containsKey(wolf); }
  public void remove(UUID wolf) { viewers.remove(wolf); }
  public void clear() { viewers.clear(); }
}
