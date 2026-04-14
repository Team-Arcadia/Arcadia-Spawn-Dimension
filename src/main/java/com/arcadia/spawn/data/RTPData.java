package com.arcadia.spawn.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public class RTPData {
    private int usages;
    private BlockPos lastRtpPos;

    public static final Codec<RTPData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("usages").orElse(0).forGetter(RTPData::getUsages),
            BlockPos.CODEC.optionalFieldOf("last_rtp_pos", BlockPos.ZERO).forGetter(RTPData::getLastRtpPos))
            .apply(instance, RTPData::new));

    public RTPData() {
        this(0, BlockPos.ZERO);
    }

    public RTPData(int usages, BlockPos lastRtpPos) {
        this.usages = usages;
        this.lastRtpPos = lastRtpPos;
    }

    public int getUsages() { return usages; }
    public void incrementUsages() { this.usages++; }
    public BlockPos getLastRtpPos() { return lastRtpPos; }
    public void setLastRtpPos(BlockPos lastRtpPos) { this.lastRtpPos = lastRtpPos; }
    public void reset() { this.usages = 0; this.lastRtpPos = BlockPos.ZERO; }
}
