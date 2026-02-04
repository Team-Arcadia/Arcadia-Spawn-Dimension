package com.vyrriox.arcadiaspawn.registry;

import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;
import com.vyrriox.arcadiaspawn.data.RTPData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, ArcadiaSpawnMod.MOD_ID);

    public static final Supplier<AttachmentType<RTPData>> RTP_DATA = ATTACHMENT_TYPES.register(
            "rtp_data",
            () -> AttachmentType.builder(() -> new RTPData())
                    .serialize(RTPData.CODEC)
                    .copyOnDeath() // Persist across death
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
