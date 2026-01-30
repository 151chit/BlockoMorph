package net.blockomorph.utils.tnt;

import net.blockomorph.utils.MultiBlockLevel;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class TntSpawnLevel extends MultiBlockLevel {
	private PrimedTnt tnt;
	private final BlockState need;

	protected TntSpawnLevel(Level orig, BlockState need) {
		super(orig, false);
		this.need = need;
	}

	public static TntSpawnLevel create(Level orig, BlockState need) {
		return CommonPlatformUtils.INSTANCE.createLevel(orig, need);
	}

	@Override
	public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
		return false;
	}

	public boolean removeBlock(BlockPos p, boolean y) {
		return false;
	}

	@Override
	public boolean hasNeighborSignal(BlockPos blockPos) {
		return true;
	}

	public BlockState getBlockState(BlockPos p) {
		return this.need;
	}

	public FluidState getFluidState(BlockPos p) {
		return realLevel.getFluidState(p);
	}

	public boolean addFreshEntity(Entity tnt) {
		if (tnt instanceof PrimedTnt ent) {
			this.tnt = ent;
			return true;
		}
		return false;
	}

	@Override
	public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double v, double v1, double v2, float v3, boolean b, ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions1, Holder<SoundEvent> holder) {
	}

	@Override
	public RecipeAccess recipeAccess() {
		return realLevel.recipeAccess();
	}

	@Override
	public FuelValues fuelValues() {
		return realLevel.fuelValues();
	}

	public PrimedTnt extractTnt() {
		return this.tnt;
	}
}
