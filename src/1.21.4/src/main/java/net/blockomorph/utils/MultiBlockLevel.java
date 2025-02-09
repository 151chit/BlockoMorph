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
import net.minecraft.world.level.saveddata.maps.MapId;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.TickRateManager;
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
import net.neoforged.neoforge.entity.PartEntity;

public class MultiBlockLevel 
extends Level {
    private final HashMap<BlockPos, BlockState> blocks = new HashMap();
    private final Level realLevel;
    
    public MultiBlockLevel(Level lv) {
        super((WritableLevelData)lv.getLevelData(), lv.dimension(), lv.registryAccess(), lv.dimensionTypeRegistration(), false, lv.isDebug(), 0, 5);
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

    @Override
    public void setDayTimePerTick(float t) {}

    public float getDayTimePerTick() {
    	return realLevel.getDayTimePerTick();
    }

    public float getDayTimeFraction() {
    	return realLevel.getDayTimeFraction();
    }

    public void setDayTimeFraction(float t) {}

    public void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4) {}

    public void playSeededSound(@Nullable Player var1, double var2, double var4, double var6, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12) {}

    public void playSeededSound(@Nullable Player var1, Entity var2, Holder<SoundEvent> var3, SoundSource var4, float var5, float var6, long var7) {}

    public void explode(@Nullable Entity var1, @Nullable DamageSource var2, @Nullable ExplosionDamageCalculator var3, double var4, double var6, double var8, float var10, boolean var11, ExplosionInteraction var12, ParticleOptions var13, ParticleOptions var14, Holder<SoundEvent> var15) {}

    public String gatherChunkSourceStats() {
    	return realLevel.gatherChunkSourceStats();
    }

    @Nullable
    public MapItemSavedData getMapData(MapId var1) {
    	return realLevel.getMapData(var1);
    }

    public void setMapData(MapId var1, MapItemSavedData var2) {}

    @Override
    public MapId getFreeMapId() {
        return realLevel.getFreeMapId();
    }

    public RecipeAccess recipeAccess() {
    	return realLevel.recipeAccess();
    }

    public TickRateManager tickRateManager() {
    	return realLevel.tickRateManager();
    }

    @Nullable
    public Entity getEntity(int var1) {
    	return realLevel.getEntity(var1);
    }

    public Collection<PartEntity<?>> dragonParts() {
    	return realLevel.dragonParts();//realLevel.dra
    }

    public PotionBrewing potionBrewing() {
    	return realLevel.potionBrewing();
    }

    public FuelValues fuelValues() {
    	return realLevel.fuelValues();
    }

    protected LevelEntityGetter<Entity> getEntities() {
    	return null;
    }

    public void destroyBlockProgress(int var1, BlockPos var2, int var3) {}

    public Scoreboard getScoreboard() {
    	return realLevel.getScoreboard();
    }

    //levelAccessor

    public void gameEvent(Holder<GameEvent> var1, Vec3 var2, GameEvent.Context var3) {}

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

    public <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3, TickPriority var4) {
    	return realLevel.createTick(var1, var2, var3, var4);
    }

    public <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3) {
    	return realLevel.createTick(var1, var2, var3);
    }

    public LevelTickAccess<Block> getBlockTicks() {
    	return realLevel.getBlockTicks();
    }

    public LevelTickAccess<Fluid> getFluidTicks() {
    	return realLevel.getFluidTicks();
    }

}
