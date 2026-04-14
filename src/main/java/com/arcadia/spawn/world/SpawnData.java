package com.arcadia.spawn.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class SpawnData extends SavedData {
    private static final String DATA_NAME = "arcadia_spawn_data";

    private double x, y, z;
    private float yaw, pitch;
    private boolean set = false;
    private String dimensionId = "arcadia:spawn";

    public static SpawnData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(SpawnData::new, SpawnData::load, null), DATA_NAME);
    }

    public static SpawnData get() {
        ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().overworld();
        return get(overworld);
    }

    public SpawnData() {}

    public static SpawnData load(CompoundTag tag, HolderLookup.Provider provider) {
        SpawnData data = new SpawnData();
        data.x = tag.getDouble("spawnX");
        data.y = tag.getDouble("spawnY");
        data.z = tag.getDouble("spawnZ");
        data.yaw = tag.getFloat("spawnYaw");
        data.pitch = tag.getFloat("spawnPitch");
        data.set = tag.getBoolean("isSet");
        if (tag.contains("dimensionId")) {
            data.dimensionId = tag.getString("dimensionId");
        }
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
        tag.putString("dimensionId", dimensionId);
        return tag;
    }

    public void setSpawn(double x, double y, double z, float yaw, float pitch, String dimensionId) {
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.dimensionId = dimensionId;
        this.set = true;
        this.setDirty();
    }

    public ResourceKey<Level> getDimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionId));
    }

    public String getDimensionId() { return dimensionId; }
    public boolean isSet() { return set; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    @Override
    public String toString() {
        return String.format("SpawnData{set=%s, dim=%s, x=%.2f, y=%.2f, z=%.2f, yaw=%.1f, pitch=%.1f}",
                set, dimensionId, x, y, z, yaw, pitch);
    }
}
