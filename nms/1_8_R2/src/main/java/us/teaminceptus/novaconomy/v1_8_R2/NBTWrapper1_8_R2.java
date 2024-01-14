package us.teaminceptus.novaconomy.v1_8_R2;

import net.minecraft.server.v1_8_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

import java.nio.ByteBuffer;
import java.util.UUID;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.ROOT;

final class NBTWrapper1_8_R2 extends NBTWrapper {
    
    public NBTWrapper1_8_R2(ItemStack item) {
        super(item);
    }

    @Override
    public String getFullTag() {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        if (!nmsitem.hasTag()) nmsitem.setTag(new NBTTagCompound());

        return nmsitem.getTag().toString();
    }

    @Override
    public String getString(String key) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getString(key);
    }

    @Override
    public void set(String key, String value) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setString(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public int getInt(String key) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getInt(key);
    }

    @Override
    public void set(String key, int value) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setInt(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public UUID getUUID(String key) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        byte[] bytes = novaconomy.getByteArray(key);

        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long first = bb.getLong();
        long second = bb.getLong();
        return new UUID(first, second);
    }

    @Override
    public void set(String key, UUID id) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());

        novaconomy.setByteArray(key, bb.array());
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public double getDouble(String key) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getDouble(key);
    }

    @Override
    public void set(String key, double value) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setDouble(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);

        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public boolean getBoolean(String key) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getBoolean(key);
    }

    @Override
    public void set(String key, boolean value) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setBoolean(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public Product getProduct(String key) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        NBTTagCompound productT = novaconomy.getCompound(key);
        if (productT.isEmpty()) return null;
        double amount = productT.getDouble("amount");

        byte[] eBytes = productT.getByteArray("economy");
        ByteBuffer ebb = ByteBuffer.wrap(eBytes);
        long eF = ebb.getLong();
        long eS = ebb.getLong();

        Economy econ = Economy.byId(new UUID(eF, eS));
        ItemStack item = CraftItemStack.asBukkitCopy(net.minecraft.server.v1_8_R2.ItemStack.createStack(productT.getCompound("item")));

        Product p = new Product(item, econ, amount);

        if (productT.hasKey("business")) {
            byte[] bBytes = productT.getByteArray("business");
            ByteBuffer bbb = ByteBuffer.wrap(bBytes);
            long bF = bbb.getLong();
            long bS = bbb.getLong();

            Business b = Business.byId(new UUID(bF, bS));
            return new BusinessProduct(p, b);
        }

        return p;
    }

    @Override
    public void set(String key, Product product) {
        net.minecraft.server.v1_8_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        NBTTagCompound productT = new NBTTagCompound();
        productT.setDouble("amount", product.getAmount());

        UUID id = product.getEconomy().getUniqueId();
        ByteBuffer ebb = ByteBuffer.wrap(new byte[16]);
        ebb.putLong(id.getMostSignificantBits());
        ebb.putLong(id.getLeastSignificantBits());

        productT.setByteArray("economy", ebb.array());

        net.minecraft.server.v1_8_R2.ItemStack nms = CraftItemStack.asNMSCopy(product.getItem());
        productT.set("item", nms.save(nms.hasTag() ? nms.getTag() : new NBTTagCompound()));

        if (product instanceof BusinessProduct) {
            UUID bId = ((BusinessProduct) product).getBusiness().getUniqueId();

            ByteBuffer bbb = ByteBuffer.wrap(new byte[16]);
            bbb.putLong(bId.getMostSignificantBits());
            bbb.putLong(bId.getLeastSignificantBits());

            productT.setByteArray("business", bbb.array());
        }

        novaconomy.set(key, productT);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

}
