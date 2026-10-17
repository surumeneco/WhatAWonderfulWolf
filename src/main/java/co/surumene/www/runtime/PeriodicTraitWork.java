package co.surumene.www.runtime;
import co.surumene.www.domain.ExpressedTrait;
import java.util.List;
public final class PeriodicTraitWork {
  public static final int STATUS = 1;
  public static final int WATCH = 2;
  public static final int SCAVENGE = 4;
  private PeriodicTraitWork() {}
  public static int from(List<ExpressedTrait> traits) {
    int flags = 0;
    for (ExpressedTrait t : traits) {
      switch (t.trait()) {
        case MINING_SUPPORT, MUSCLE, GUARDIAN, HEALING, FIRE_BIRD, UNYIELDING, SWIMMER -> flags |= STATUS;
        case WATCHMAN, TARGET -> flags |= WATCH;
        case SCAVENGER -> flags |= SCAVENGE;
        default -> { }
      }
    }
    return flags;
  }
  public static boolean has(int flags, int kind) { return (flags & kind) != 0; }
}
