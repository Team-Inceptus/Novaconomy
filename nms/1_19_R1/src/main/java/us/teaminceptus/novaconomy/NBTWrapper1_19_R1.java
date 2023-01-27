package us.teaminceptus.novaconomy;

import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.minecraft.nbt.CompoundTag;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.ROOT;

import java.util.UUID;

public class NBTWrapper1_19_R1 extends NBTWrapper {
    
    public NBTWrapper1_19_R1(ItemStack item) {
        super(item);
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
    public ItemStack getItem(String key) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        CompoundTag item = novaconomy.getCompound(key);
        return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.of(item));
    }

    @Override
    public void set(String key, ItemStack item) {
        net.minecraft.world.item.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        CompoundTag tag = nmsitem.getOrCreateTag();
        CompoundTag novaconomy = tag.getCompound(ROOT);

        novaconomy.put(key, CraftItemStack.asNMSCopy(item).getOrCreateTag());
        tag.put(ROOT, novaconomy);
        nmsitem.setTag(tag);
    }

}
