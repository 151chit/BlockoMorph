package net.blockomorph.utils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.TntBlock;

public class TntSpawnLevel extends MultiBlockLevel {
	private PrimedTnt tnt;
	private final BlockState need;

	public TntSpawnLevel(Level orig, BlockState need) {
		super(orig, orig.isClientSide);
		this.need = need;
	}

	public TntSpawnLevel(Level orig, boolean c, BlockState need) {
		super(orig, c);
		this.need = need;
	}

	@Override
	public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
		return false;
	}

	public boolean removeBlock(BlockPos p, boolean y) {
		return false;
	}

	public void playSound(@Nullable Player pl, BlockPos bp, SoundEvent se, SoundSource ss, float sp1, float sp2) {
		this.realLevel.playSound(pl, bp, se, ss, sp1, sp2);
	}

	public BlockState getBlockState(BlockPos p) {
		return this.need;
	}

	public FluidState getFluidState(BlockPos p) {
		return realLevel.getFluidState(p);
	}

	public void playSeededSound(
		@Nullable Player a1, 
		double a2, 
		double a3, 
		double a4, 
		Holder<SoundEvent> a5, 
		SoundSource a6, 
		float a7, 
		float a8, 
		long a9
	) {
		realLevel.playSeededSound(a1, a2, a3, a4, a5, a6, a7, a8, a9);
	}

	public boolean addFreshEntity(Entity tnt) {
		if (tnt instanceof PrimedTnt ent) {
			this.tnt = ent;
			return true;
		}
		return false;
	}

	public Explosion explode(
		@Nullable Entity a1, 
		@Nullable DamageSource a2, 
		@Nullable ExplosionDamageCalculator a3, 
		double a4, double a5, double a6, float a7, boolean a8, 
		Level.ExplosionInteraction a9, 
		boolean a10
	) {
		return null;
	}

    @Override
	public Explosion explode(
		@Nullable Entity a1, 
		@Nullable DamageSource a2, 
		@Nullable ExplosionDamageCalculator a3, 
		double a4, double a5, double a6, float a7, boolean a8, 
		Level.ExplosionInteraction a9
	) {
		return null;
	}

	public PrimedTnt extractTnt() {
		return this.tnt;
	}
}
