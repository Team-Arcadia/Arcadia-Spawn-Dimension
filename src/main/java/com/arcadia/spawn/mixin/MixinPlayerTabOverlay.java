package com.arcadia.spawn.mixin;

import com.arcadia.spawn.tablist.TabListConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the latency/ping signal-bar icon next to each player in the tab list
 * when {@code TabListConfig.hidePingIcons = true}.
 *
 * Client-side only — players who don't have this mod installed still see the icons.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void arcadia$hidePingIcons(GuiGraphics graphics, int width, int x, int y,
                                       PlayerInfo playerInfo, CallbackInfo ci) {
        if (TabListConfig.VALUES.hidePingIcons.get()) {
            ci.cancel();
        }
    }
}
