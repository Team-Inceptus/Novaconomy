package us.teaminceptus.novaconomy.v1_18_R1;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.UUID;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.ROOT;

final class NBTWrapper1_18_R1 extends NBTWrapper {
    
    public NBTWrapper1_18_R1(ItemStack item) {
        super(item);
    }

    @Override
    public String getFullTag() {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        return nmsitem.getOrCreateTag().toString();
    }

    @Override
    public String getString(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        return novaconomy.getString(key);
    }

    @Override
    public void set(String key, String value) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        novaconomy.putString(key, value);
        tag.put(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public int getInt(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        return novaconomy.getInt(key);
    }

    @Override
    public void set(String key, int value) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        novaconomy.putInt(key, value);
        tag.put(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public UUID getUUID(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        return novaconomy.getUUID(key);
    }

    @Override
    public void set(String key, UUID id) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        novaconomy.putUUID(key, id);
        tag.put(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public double getDouble(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        return novaconomy.getDouble(key);
    }

    @Override
    public void set(String key, double value) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        novaconomy.putDouble(key, value);
        tag.put(ROOT, novaconomy);
        nmsitem.setTag(tag);

        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public boolean getBoolean(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        return novaconomy.getBoolean(key);
    }

    @Override
    public void set(String key, boolean value) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        novaconomy.putBoolean(key, value);
        tag.put(ROOT, novaconomy);
        nmsitem.setTag(tag);
        item = CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public Product getProduct(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        CompoundTag productT = novaconomy.getCompound(key);
        if (productT.isEmpty()) return null;
        double amount = productT.getDouble("amount");
        Economy econ = Economy.byId(productT.getUUID("economy"));
        ItemStack item = CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.of(productT.getCompound("item")));

        Product p = new Product(item, econ, amount);

        if (productT.contains("business")) {
            Business b = Business.byId(productT.getUUID("business"));
            return new BusinessProduct(p, b);
        }

        return p;
    }

    @Override
    public void set(String key, Product product) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        CompoundTag productT = new CompoundTag();
        productT.putDouble("amount", product.getAmount());
        productT.putUUID("economy", product.getEconomy().getUniqueId());
        productT.put("item", CraftItemStack.asNMSCopy(product.getItem()).save(CraftItemStack.asNMSCopy(product.getItem()).getOrCreateTag()));

        if (product instanceof BusinessProduct bp)
            productT.putUUID("business", bp.getBusiness().getUniqueId());

item = CraftItemStack.asBukkitCopy(nmsitem);
    }

}
