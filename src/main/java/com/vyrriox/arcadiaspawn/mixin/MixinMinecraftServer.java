package com.vyrriox.arcadiaspawn.mixin;

import com.vyrriox.arcadiaspawn.config.SlotBypassConfig;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    /**
     * Intercepts the buildPlayerStatus method to inject a fake max player count
     * into the ServerStatus response.
     */
    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void arcadia$injectFakeMaxSlots(CallbackInfoReturnable<ServerStatus.Players> cir) {
        if (SlotBypassConfig.VALUES.enabled.get() && SlotBypassConfig.VALUES.fakeMaxSlotsEnabled.get()) {
            ServerStatus.Players originalPlayers = cir.getReturnValue();
            if (originalPlayers != null) {
                // Return a new Players object with the fake max slots from the config
                int fakeMaxSlots = SlotBypassConfig.VALUES.maxSlots.get();
                ServerStatus.Players fakePlayers = new ServerStatus.Players(
                        fakeMaxSlots,
                        originalPlayers.online(),
                        originalPlayers.sample()
                );
                cir.setReturnValue(fakePlayers);
            }
        }
    }
}
