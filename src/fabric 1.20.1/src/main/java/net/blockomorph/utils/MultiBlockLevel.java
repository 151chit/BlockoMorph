package net.blockomorph.utils;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.CrashReportCategory;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.core.particles.ParticleOptions;
import java.util.Collection;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.world.entity.boss.EnderDragonPart;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import net.minecraft.world.level.material.Fluid;
import java.util.HashMap;
import net.minecraft.world.item.crafting.RecipeManager;

public class MultiBlockLevel 
extends Level {
    private final HashMap<BlockPos, BlockState> blocks = new HashMap();
    private final Level realLevel;
    
    public MultiBlockLevel(Level lv) {
        super((WritableLevelData)lv.getLevelData(), lv.dimension(), lv.registryAccess(), lv.dimensionTypeRegistration(), lv.getProfilerSupplier(), false, lv.isDebug(), 0, 5);
        this.realLevel = lv;
    }

    @Override
    public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
    	this.blocks.put(blockPos, blockState);
    	return true;
    }

    public HashMap<BlockPos, BlockState> getBlocks() {
    	return this.blocks;
    }






    //suppliers

    public RecipeManager getRecipeManager() {
    	return realLevel.getRecipeManager();
    }

    public MapItemSavedData getMapData(String s) {
    	return realLevel.getMapData(s);
    }

    public void setMapData(String s, MapItemSavedData d) {}

    public int getFreeMapId() {
    	return realLevel.getFreeMapId();
    }

    public void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4) {}

    public void playSeededSound(@Nullable Player var1, double var2, double var4, double var6, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12) {}

    public void playSeededSound(@Nullable Player var1, Entity var2, Holder<SoundEvent> var3, SoundSource var4, float var5, float var6, long var7) {}

    public String gatherChunkSourceStats() {
    	return realLevel.gatherChunkSourceStats();
    }

    @Nullable
    public Entity getEntity(int var1) {
    	return realLevel.getEntity(var1);
    }

    protected LevelEntityGetter<Entity> getEntities() {
    	return null;
    }

    public void destroyBlockProgress(int var1, BlockPos var2, int var3) {}

    public Scoreboard getScoreboard() {
    	return realLevel.getScoreboard();
    }

    //levelAccessor

    public void gameEvent(GameEvent var1, Vec3 var2, GameEvent.Context var3) {}

    public void playSound(@Nullable Player var1, BlockPos var2, SoundEvent var3, SoundSource var4, float var5, float var6) {}

    public void addParticle(ParticleOptions var1, double var2, double var4, double var6, double var8, double var10, double var12) {}

    public void levelEvent(@Nullable Player var1, int var2, BlockPos var3, int var4) {}

    public ChunkSource getChunkSource() {
    	return realLevel.getChunkSource();
    }

    //entityGetter

    public List<? extends Player> players() {
    	return realLevel.players();
    }

    //levelReader

    public FeatureFlagSet enabledFeatures() {
    	return realLevel.enabledFeatures();
    }

    public int getSeaLevel() {
    	return realLevel.getSeaLevel();
    }

    public Holder<Biome> getUncachedNoiseBiome(int var1, int var2, int var3) {
    	return realLevel.getUncachedNoiseBiome(var1, var2, var3);
    }

    //blockAndTintGetter

    public float getShade(Direction var1, boolean var2) {
    	return realLevel.getShade(var1, var2);
    }

    public LevelLightEngine getLightEngine() {
    	return realLevel.getLightEngine();
    }

    public int getBlockTint(BlockPos var1, ColorResolver var2) {
    	return realLevel.getBlockTint(var1, var2);
    }

    //scheduledTickAcceess

    public LevelTickAccess<Block> getBlockTicks() {
    	return realLevel.getBlockTicks();
    }

    public LevelTickAccess<Fluid> getFluidTicks() {
    	return realLevel.getFluidTicks();
    }

}
