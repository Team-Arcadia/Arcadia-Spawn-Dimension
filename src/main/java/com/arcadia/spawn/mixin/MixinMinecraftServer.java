package com.arcadia.spawn.mixin;

import com.arcadia.spawn.config.SlotBypassConfig;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void arcadia$injectFakeMaxSlots(CallbackInfoReturnable<ServerStatus.Players> cir) {
        if (SlotBypassConfig.VALUES.enabled.get() && SlotBypassConfig.VALUES.fakeMaxSlotsEnabled.get()) {
            ServerStatus.Players original = cir.getReturnValue();
            if (original != null) {
                cir.setReturnValue(new ServerStatus.Players(
                        SlotBypassConfig.VALUES.maxSlots.get(),
                        original.online(),
                        original.sample()));
            }
        }
    }
}
