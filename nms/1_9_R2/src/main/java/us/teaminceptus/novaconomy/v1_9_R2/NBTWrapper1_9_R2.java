package us.teaminceptus.novaconomy.v1_9_R2;

import net.minecraft.server.v1_9_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.UUID;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.ROOT;

final class NBTWrapper1_9_R2 extends NBTWrapper {
    
    public NBTWrapper1_9_R2(ItemStack item) {
        super(item);
    }

    @Override
    public String getFullTag() {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        if (!nmsitem.hasTag()) nmsitem.setTag(new NBTTagCompound());

        return nmsitem.getTag().toString();
    }
    @Override
    public String getString(String key) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getString(key);
    }

    @Override
    public void set(String key, String value) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setString(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public int getInt(String key) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getInt(key);
    }

    @Override
    public void set(String key, int value) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setInt(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public UUID getUUID(String key) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.a(key);
    }

    @Override
    public void set(String key, UUID id) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.a(key, id);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public double getDouble(String key) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getDouble(key);
    }

    @Override
    public void set(String key, double value) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setDouble(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);

        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public boolean getBoolean(String key) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getBoolean(key);
    }

    @Override
    public void set(String key, boolean value) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setBoolean(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public Product getProduct(String key) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        NBTTagCompound productT = novaconomy.getCompound(key);
        if (productT.isEmpty()) return null;
        double amount = productT.getDouble("amount");
        Economy econ = Economy.byId(productT.a("economy"));
        ItemStack item = CraftItemStack.asBukkitCopy(net.minecraft.server.v1_9_R2.ItemStack.createStack(productT.getCompound("item")));

        Product p = new Product(item, econ, amount);

        if (productT.hasKey("business")) {
            Business b = Business.byId(productT.a("business"));
            return new BusinessProduct(p, b);
        }

        return p;
    }

    @Override
    public void set(String key, Product product) {
        net.minecraft.server.v1_9_R2.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        NBTTagCompound productT = new NBTTagCompound();
        productT.setDouble("amount", product.getAmount());
        productT.a("economy", product.getEconomy().getUniqueId());

        net.minecraft.server.v1_9_R2.ItemStack nms = CraftItemStack.asNMSCopy(product.getItem());
        productT.set("item", nms.save(nms.hasTag() ? nms.getTag() : new NBTTagCompound()));

        if (product instanceof BusinessProduct)
            productT.a("business", ((BusinessProduct) product).getBusiness().getUniqueId());

        novaconomy.set(key, productT);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

}
