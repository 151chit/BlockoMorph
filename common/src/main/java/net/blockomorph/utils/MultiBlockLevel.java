package net.blockomorph.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public abstract class MultiBlockLevel
		extends Level {
	private final HashMap<BlockPos, BlockState> blocks = new HashMap<>();
	protected final Level realLevel;

	public MultiBlockLevel(Level lv, boolean client) {
		super((WritableLevelData) lv.getLevelData(), lv.dimension(), lv.registryAccess(), lv.dimensionTypeRegistration(), lv.getProfilerSupplier(), client, lv.isDebug(), 0, 5);
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

	public List<Entity> getEntities(@Nullable Entity e, AABB ab, Predicate<? super Entity> p) {
		return realLevel.getEntities(e, ab, p);
	}

	public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> test, AABB ab, Predicate<? super T> p, List<? super T> l, int i) {
		realLevel.getEntities(test, ab, p, l, i);
	}

	public RecipeManager getRecipeManager() {
		return realLevel.getRecipeManager();
	}

	public void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4) {
	}

	public void playSeededSound(@Nullable Player var1, double var2, double var4, double var6, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12) {
	}

	public void playSeededSound(@Nullable Player var1, Entity var2, Holder<SoundEvent> var3, SoundSource var4, float var5, float var6, long var7) {
	}

	public String gatherChunkSourceStats() {
		return realLevel.gatherChunkSourceStats();
	}

	@Nullable
	public Entity getEntity(int var1) {
		return realLevel.getEntity(var1);
	}

	@Nullable
	protected LevelEntityGetter<Entity> getEntities() {
		return null;
	}

	public void destroyBlockProgress(int var1, BlockPos var2, int var3) {
	}

	public Scoreboard getScoreboard() {
		return realLevel.getScoreboard();
	}

	//levelAccessor

	public void gameEvent(Holder<GameEvent> var1, Vec3 var2, GameEvent.Context var3) {
	}

	public void playSound(@Nullable Player var1, BlockPos var2, SoundEvent var3, SoundSource var4, float var5, float var6) {
	}

	public void addParticle(ParticleOptions var1, double var2, double var4, double var6, double var8, double var10, double var12) {
	}

	public void levelEvent(@Nullable Player var1, int var2, BlockPos var3, int var4) {
	}

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

	public Level getRealLevel() {
		return this.realLevel;
	}
}
