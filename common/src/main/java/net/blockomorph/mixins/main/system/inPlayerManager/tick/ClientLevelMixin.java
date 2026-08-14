package net.blockomorph.mixins.main.system.inPlayerManager.tick;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.core.tick.AnimateTicker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements EntityGetter  {
	@Shadow protected abstract void trySpawnDripParticles(BlockPos pos, BlockState state, ParticleOptions dripParticle, boolean isTopSolid);

	@ModifyReceiver(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;"))
	private BlockPos.MutableBlockPos animTick(BlockPos.MutableBlockPos instance, int x, int y, int z,
			@Local(argsOnly = true) Block marker, @Local(argsOnly = true) RandomSource animateRandom) {
		PlayersStorage storage = PlayersStorage.ofLevel(this);
		try (storage) {
			for (Player player : storage.findMorphedOnPos(null, x + 0.5, y + 0.5, z + 0.5)) {
				BlockInPlayer2 block = MorphMath.getBlockInPlayerOnPos(PlayerAccessor.of(player), x + 0.5, y + 0.5, z + 0.5);
				if (block != null) {
					AnimateTicker.tickBlock(block, marker, animateRandom, this::trySpawnDripParticles);
				}
			}
		}
		return instance;
	}
}
