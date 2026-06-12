package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.LevelAcc;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements ClientLevelAccessor {

	@Shadow @Final private Minecraft minecraft;

	@Shadow
	protected abstract void trySpawnDripParticles(BlockPos blockPos, BlockState blockState, ParticleOptions particleOptions, boolean bl);

	@Unique
	private final Map<BlockPos, List<BlockInPlayer2>> blocksForTick = new HashMap<>();

	@Inject(method = "animateTick", at = @At("HEAD"))
	public void collectForTick(int x, int y, int z, CallbackInfo ci) {
		int radius = 32;
		this.blocksForTick.clear();
		if (Config.get().blockClientParticles.getValue()) {
			AABB searchBox = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
			for (PlayerAccessor pl : PlayersMultiSectionStorage.fromLevel(LevelAcc.of(this)).findMorphed(null, searchBox)) {
				if (pl != this.minecraft.player || !this.minecraft.options.getCameraType().isFirstPerson()) {
					pl.getBlocksData2InArea(searchBox, (a, block, realPos) -> {
						BlockPos realBlockPosInWorld = BlockPos.containing(realPos);
						this.blocksForTick.computeIfAbsent(realBlockPosInWorld, l -> new ArrayList<>()).add(block);
					});
				}
			}
		}
	}


	@Inject(method = "doAnimateTick", at = @At(value = "TAIL"))
	public void animatePlayers(int x, int y, int z, int radius, RandomSource random, Block MARKER, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
		List<BlockInPlayer2> list = this.blocksForTick.get(pos);
		if (list != null && !list.isEmpty()) {
			BlockInPlayer2 block = list.size() == 1 ? list.get(0) : list.get(LevelAcc.of(this).random.nextInt(list.size()));
			block.animateTick(random, MARKER, (blockState) -> {
				ParticleOptions particleOptions = block.getBlockState().getFluidState().getDripParticle();
				if (particleOptions != null && LevelAcc.of(this).random.nextInt(10) == 0) {
					boolean faceSturdy = block.getBlockState().isFaceSturdy(LevelAcc.of(this), block.getPos(), Direction.DOWN);
					this.trySpawnDripParticles(pos.below(), blockState, particleOptions, faceSturdy);
				}
			});
		}
	}

	@ModifyVariable(method = "calculateBlockTint", at = @At("HEAD"))
	public BlockPos getRealPos(BlockPos value) {
		return InPlayerBlockPos.checkOnReal(value);
	}

	private boolean BErendering;
	private boolean externalGETblockLocked;

	public boolean specialRenderingMode() {
		return this.BErendering;
	}

	public void setSpecialRenderingMode(boolean yes) {
		this.BErendering = yes;
	}

	@Override
	public void lockExternalMorphedBlockGetter(boolean yes) {
		this.externalGETblockLocked = yes;
	}

	@Override
	public boolean isExternalMorphedBlockGetterLocked() {
		return this.externalGETblockLocked;
	}
}
