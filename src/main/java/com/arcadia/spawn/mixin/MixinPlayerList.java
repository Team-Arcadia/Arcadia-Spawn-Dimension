package com.arcadia.spawn.mixin;

import com.arcadia.spawn.config.SlotBypassConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void arcadia$hideJoinLeaveMessages(Component message, boolean bypassHiddenChat, CallbackInfo ci) {
        if (SlotBypassConfig.VALUES.hideJoinLeaveMessages.get()
                && message.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if ("multiplayer.player.joined".equals(key)
                    || "multiplayer.player.joined.renamed".equals(key)
                    || "multiplayer.player.left".equals(key)) {
                ci.cancel();
            }
        }
    }
}
