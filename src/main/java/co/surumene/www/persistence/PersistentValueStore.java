package co.surumene.www.persistence;

public interface PersistentValueStore {
    String getString(String key);
    Integer getInteger(String key);
    byte[] getBytes(String key);

    void putString(String key, String value);
    void putInteger(String key, int value);
    void putBytes(String key, byte[] value);
    void remove(String key);
}
