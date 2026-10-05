package co.surumene.www.runtime;

public interface BiologicalClockStateStore {
    BiologicalClockState load();
    void save(BiologicalClockState state);
}
