package com.vyrriox.arcadiaspawn.lobby;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record LobbyLocation(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw,
        float pitch, String description, String item) {

    public static LobbyLocation of(String name, String dimensionId, double x, double y, double z, float yaw,
            float pitch, String description, String item) {
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionId));
        return new LobbyLocation(name, dimKey, x, y, z, yaw, pitch, description != null ? description : "",
                item != null ? item : "minecraft:paper");
    }
}
