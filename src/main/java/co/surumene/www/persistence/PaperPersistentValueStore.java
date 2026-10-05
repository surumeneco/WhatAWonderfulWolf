package co.surumene.www.persistence;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Objects;

final class PaperPersistentValueStore implements PersistentValueStore {
    private final String namespace;
    private final PersistentDataContainer container;

    PaperPersistentValueStore(Plugin plugin, PersistentDataContainer container) {
        Plugin checkedPlugin = Objects.requireNonNull(plugin, "plugin");
        this.namespace = checkedPlugin.getName().toLowerCase(Locale.ROOT);
        this.container = Objects.requireNonNull(container, "container");
    }

    @Override
    public String getString(String key) {
        return container.get(key(key), PersistentDataType.STRING);
    }

    @Override
    public Integer getInteger(String key) {
        return container.get(key(key), PersistentDataType.INTEGER);
    }

    @Override
    public byte[] getBytes(String key) {
        byte[] bytes = container.get(key(key), PersistentDataType.BYTE_ARRAY);
        return bytes == null ? null : bytes.clone();
    }

    @Override
    public void putString(String key, String value) {
        container.set(key(key), PersistentDataType.STRING, Objects.requireNonNull(value, "value"));
    }

    @Override
    public void putInteger(String key, int value) {
        container.set(key(key), PersistentDataType.INTEGER, value);
    }

    @Override
    public void putBytes(String key, byte[] value) {
        container.set(
                key(key),
                PersistentDataType.BYTE_ARRAY,
                Objects.requireNonNull(value, "value").clone());
    }

    @Override
    public void remove(String key) {
        container.remove(key(key));
    }

    private NamespacedKey key(String key) {
        return new NamespacedKey(namespace, key);
    }
}
