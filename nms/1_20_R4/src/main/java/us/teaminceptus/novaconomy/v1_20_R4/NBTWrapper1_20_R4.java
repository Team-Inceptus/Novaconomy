package us.teaminceptus.novaconomy.v1_20_R4;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

final class NBTWrapper1_20_R4 extends NBTWrapper {
    
    public NBTWrapper1_20_R4(ItemStack item) {
        super(item);
    }

    @Override
    public String getFullTag() {
        return item.getItemMeta().getAsString();
    }

    @Override
    public String getString(String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.STRING);
    }

    @Override
    public void set(String key, String value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    @Override
    public int getInt(String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.INTEGER);
    }

    @Override
    public void set(String key, int value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    @Override
    public UUID getUUID(String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        long[] array = container.get(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.LONG_ARRAY);
        return new UUID(array[0], array[1]);
    }

    @Override
    public void set(String key, UUID id) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.LONG_ARRAY, new long[] {id.getMostSignificantBits(), id.getLeastSignificantBits()});
        item.setItemMeta(meta);
    }

    @Override
    public double getDouble(String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.DOUBLE);
    }

    @Override
    public void set(String key, double value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
    }

    @Override
    public boolean getBoolean(String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.BOOLEAN);
    }

    @Override
    public void set(String key, boolean value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.BOOLEAN, value);
        item.setItemMeta(meta);
    }

    @Override
    public Product getProduct(String key) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(
                    container.get(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.BYTE_ARRAY)
            );
            BukkitObjectInputStream ois = new BukkitObjectInputStream(bis);
            Product p = (Product) ois.readObject();

            ois.close();
            return p;
        } catch (IOException | ClassNotFoundException e) {
            NovaConfig.print(e);
            return null;
        }
    }

    @Override
    public void set(String key, Product product) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos);
            oos.writeObject(product);
            oos.close();

            container.set(new NamespacedKey(NovaConfig.getPlugin(), key), PersistentDataType.BYTE_ARRAY, bos.toByteArray());
        } catch (IOException e) {
            NovaConfig.print(e);
        }

        item.setItemMeta(meta);
    }

}
