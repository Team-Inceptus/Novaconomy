package us.teaminceptus.novaconomy.api.business;

import com.google.common.collect.ImmutableMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents the Business Copyright registry.
 */
@SuppressWarnings("unchecked")
public final class BusinessCopyright {

    private static final Map<ItemStack, UUID> registry = new HashMap<>();

    private BusinessCopyright() {}

    static {
        read();
    }

    /**
     * Gets an immutable copy of the entire copyright registry.
     * @return An immutable copy of the registry.
     */
    @NotNull
    public static Map<ItemStack, Business> getAllItems() {
        return ImmutableMap.copyOf(registry.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), Business.byId(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Returns the business that owns the specified item.
     * @param item The item to check.
     * @return The business that owns the item, or null if the item is not owned by any business.
     */
    @Nullable
    public static Business getOwner(@Nullable ItemStack item) {
        if (item == null) return null;

        ItemStack item0 = item.clone();
        item0.setAmount(1);
        return Business.byId(registry.get(item0));
    }

    /**
     * Checks if the specified item is registered in the copyright registry.
     * @param item The item to check.
     * @return true if the item is registered, false otherwise
     */
    public static boolean isRegistered(@Nullable ItemStack item) {
        if (item == null) return false;

        ItemStack item0 = item.clone();
        item0.setAmount(1);

        return registry.containsKey(item0);
    }

    /**
     * Sets the owner of the specified item.
     * @param item The item to set the owner of.
     * @param business The business to set as the owner.
     * @throws IllegalArgumentException if item or business is null
     * @throws IllegalStateException if the item is already claimed
     */
    public static void setOwner(@NotNull ItemStack item, @NotNull Business business) throws IllegalArgumentException, IllegalStateException {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        if (business == null) throw new IllegalArgumentException("business cannot be null");
        if (isRegistered(item)) throw new IllegalStateException("item is already owned");

        ItemStack item0 = item.clone();
        item0.setAmount(1);

        registry.put(item0, business.getUniqueId());
        write();
    }

    /**
     * Removes the owner of the specified item.
     * @param item The item to remove the owner of
     * @throws IllegalArgumentException if item is null
     */
    public static void removeOwner(@NotNull ItemStack item) throws IllegalArgumentException {
        if (item == null) throw new IllegalArgumentException("item cannot be null");

        ItemStack item0 = item.clone();
        item0.setAmount(1);

        registry.remove(item0);
        write();
    }

    // I/O

    private static void checkTable() throws SQLException  {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS business_copyright (" +
                "item MEDIUMBLOB NOT NULL, " +
                "business CHAR(36) NOT NULL, " +
                "PRIMARY KEY (item))");
    }

    private static void read() {
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                readDB();
            } else
                readFile();
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private static void readDB() throws SQLException, IOException, ClassNotFoundException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();
        ResultSet rs = db.createStatement().executeQuery("SELECT * FROM business_copyright");

        while (rs.next()) {
            ByteArrayInputStream bais = new ByteArrayInputStream(rs.getBytes("item"));
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);

            ItemStack item = (ItemStack) bois.readObject();
            UUID business = UUID.fromString(rs.getString("business"));

            registry.put(item, business);
            bois.close();
        }

        rs.close();
    }

    private static void readFile() throws IOException, ClassNotFoundException {
        File registryFile = new File(NovaConfig.getBusinessesFolder(), "copyright.dat");
        if (!registryFile.exists()) return;

        FileInputStream fis = new FileInputStream(registryFile);
        BukkitObjectInputStream bois = new BukkitObjectInputStream(fis);

        Map<ItemStack, UUID> map = (Map<ItemStack, UUID>) bois.readObject();
        registry.putAll(map);
    }

    private static void write() {
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                writeDB();
            } else
                writeFile();
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private static void writeDB() throws SQLException, IOException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        for (Map.Entry<ItemStack, UUID> entry : registry.entrySet()) {
            PreparedStatement ps = db.prepareStatement("INSERT INTO business_copyright VALUES (?, ?)");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeObject(entry.getKey());
            boos.close();

            ps.setBytes(1, baos.toByteArray());
            ps.setString(2, entry.getValue().toString());
            ps.executeUpdate();
            ps.close();
        }
    }

    private static void writeFile() throws IOException {
        File registryFile = new File(NovaConfig.getBusinessesFolder(), "copyright.dat");
        if (!registryFile.exists()) registryFile.createNewFile();

        FileOutputStream fos = new FileOutputStream(registryFile);
        BukkitObjectOutputStream boos = new BukkitObjectOutputStream(fos);
        boos.writeObject(registry);
        boos.close();
    }

}
