package net.blockomorph.core.tick;

import net.blockomorph.core.BlockInPlayer2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class AnimateTicker {

	public static void tickBlock(BlockInPlayer2 block, Block marker, RandomSource specialRandom, DripParticleSpawner dripSpawner) {
		BlockState state = block.getBlockState();
		Level level = block.getOwner().level();
		BlockPos id = block.getPos();
		RandomSource randomSource = level.getRandom();
		state.getBlock().animateTick(state, level, id, specialRandom);
		if (block.shouldDoFluidAction()) {
			FluidState fluidState = state.getFluidState();
			if (!fluidState.isEmpty()) {
				fluidState.animateTick(level, id, specialRandom);
				ParticleOptions dripParticle = fluidState.getDripParticle();
				if (dripParticle != null && randomSource.nextInt(10) == 0) {
					boolean hasWatertightBottom = state.isFaceSturdy(level, id, Direction.DOWN);
					BlockPos below = id.below();
					dripSpawner.trySpawnDripParticles(below, level.getBlockState(below), dripParticle, hasWatertightBottom);
				}
			}
		}
		if (state.getBlock() == marker) {
			level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state), id.getX() + 0.5, id.getY() + 0.5, id.getZ() + 0.5, 0, 0, 0);
		}
		if (!state.isCollisionShapeFullBlock(level, id)) {
			var holder = level.getBiome(id).value().getAmbientParticle();
			if (holder.isPresent()) {
				if (holder.get().canSpawn(randomSource)) {
					level.addParticle(holder.get().getOptions(),
							id.getX() + randomSource.nextDouble(),
							id.getY() + randomSource.nextDouble(),
							id.getZ() + randomSource.nextDouble(), 0.0, 0.0, 0.0);
				}
			}
		}
	}

	@FunctionalInterface
	public interface DripParticleSpawner {
		void trySpawnDripParticles(BlockPos below, BlockState state, ParticleOptions particleTpe, boolean hasWatertightBottom);
	}
}
