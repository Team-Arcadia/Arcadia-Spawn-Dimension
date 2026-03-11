package com.vyrriox.arcadiaspawn.mixin;

import com.vyrriox.arcadiaspawn.config.SlotBypassConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    /**
     * Intercepts system messages broadcasted to all players to hide join/leave messages.
     */
    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void arcadia$hideJoinLeaveMessages(Component message, boolean bypassHiddenChat, CallbackInfo ci) {
        if (SlotBypassConfig.VALUES.hideJoinLeaveMessages.get() && message.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if ("multiplayer.player.joined".equals(key) ||
                "multiplayer.player.joined.renamed".equals(key) ||
                "multiplayer.player.left".equals(key)) {
                
                // Cancel the broadcast of this message
                ci.cancel();
            }
        }
    }
}
