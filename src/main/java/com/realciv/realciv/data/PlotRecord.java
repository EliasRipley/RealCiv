package com.realciv.realciv.data;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class PlotRecord {
    private final String dimension;
    private final long chunkX;
    private final long chunkZ;
    private LandClass landClass;
    @Nullable
    private UUID ownerId;
    private long nextUpkeepTick;
    private long delinquentSinceTick = -1L;

    public PlotRecord(String dimension, long chunkX, long chunkZ, LandClass landClass, @Nullable UUID ownerId) {
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.landClass = landClass;
        this.ownerId = ownerId;
    }

    public String plotKey() {
        return dimension + "|" + chunkX + "|" + chunkZ;
    }

    public String dimension() {
        return dimension;
    }

    public long chunkX() {
        return chunkX;
    }

    public long chunkZ() {
        return chunkZ;
    }

    public LandClass landClass() {
        return landClass;
    }

    public void setLandClass(LandClass value) {
        this.landClass = value;
    }

    @Nullable
    public UUID ownerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable UUID value) {
        this.ownerId = value;
    }

    public long nextUpkeepTick() {
        return nextUpkeepTick;
    }

    public void setNextUpkeepTick(long value) {
        this.nextUpkeepTick = value;
    }

    public long delinquentSinceTick() {
        return delinquentSinceTick;
    }

    public void setDelinquentSinceTick(long value) {
        this.delinquentSinceTick = value;
    }

    @Nullable
    public UUID renterId() {
        return ownerId;
    }

    public long expiresAtTick() {
        return nextUpkeepTick;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension);
        tag.putLong("chunkX", chunkX);
        tag.putLong("chunkZ", chunkZ);
        tag.putString("landClass", landClass.name());
        if (ownerId != null) {
            tag.putString("ownerId", ownerId.toString());
        }
        tag.putLong("nextUpkeepTick", nextUpkeepTick);
        tag.putLong("delinquentSinceTick", delinquentSinceTick);
        return tag;
    }

    @Nullable
    public static PlotRecord load(CompoundTag tag) {
        if (!tag.contains("dimension")) {
            return null;
        }
        try {
            String dimension = tag.getString("dimension");
            long chunkX = tag.getLong("chunkX");
            long chunkZ = tag.getLong("chunkZ");

            LandClass landClass = LandClass.COMMUNITY;
            if (tag.contains("landClass")) {
                LandClass parsed = LandClass.fromConfig(tag.getString("landClass"));
                if (parsed != null) {
                    landClass = parsed;
                }
            }

            UUID owner = null;
            if (tag.contains("ownerId")) {
                owner = UUID.fromString(tag.getString("ownerId"));
            } else if (tag.contains("renterId")) {
                owner = UUID.fromString(tag.getString("renterId"));
                landClass = LandClass.PRIVATE;
            }

            PlotRecord record = new PlotRecord(dimension, chunkX, chunkZ, landClass, owner);
            if (tag.contains("nextUpkeepTick")) {
                record.nextUpkeepTick = tag.getLong("nextUpkeepTick");
            } else if (tag.contains("expiresAtTick")) {
                record.nextUpkeepTick = tag.getLong("expiresAtTick");
            }
            record.delinquentSinceTick = tag.contains("delinquentSinceTick") ? tag.getLong("delinquentSinceTick") : -1L;
            return record;
        } catch (Exception ignored) {
            return null;
        }
    }
}
