package com.vyrriox.spawndimension.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class SpawnData extends SavedData {
    private static final String DATA_NAME = "lobby_spawn_data";

    private double x, y, z;
    private float yaw, pitch;
    private boolean set = false;

    public static SpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(SpawnData::new, SpawnData::load, null), DATA_NAME);
    }

    // Fallback if we just want to get it from the overworld (common storage place)
    public static SpawnData get() {
        ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().overworld();
        return get(overworld);
    }

    public SpawnData() {
    }

    public static SpawnData load(CompoundTag tag, HolderLookup.Provider provider) {
        SpawnData data = new SpawnData();
        data.x = tag.getDouble("spawnX");
        data.y = tag.getDouble("spawnY");
        data.z = tag.getDouble("spawnZ");
        data.yaw = tag.getFloat("spawnYaw");
        data.pitch = tag.getFloat("spawnPitch");
        data.set = tag.getBoolean("isSet");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putDouble("spawnX", x);
        tag.putDouble("spawnY", y);
        tag.putDouble("spawnZ", z);
        tag.putFloat("spawnYaw", yaw);
        tag.putFloat("spawnPitch", pitch);
        tag.putBoolean("isSet", set);
        return tag;
    }

    public void setSpawn(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.set = true;
        this.setDirty();
    }

    public boolean isSet() {
        return set;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
