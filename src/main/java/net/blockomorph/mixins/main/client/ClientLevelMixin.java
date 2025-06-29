package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements ClientLevelAccessor {
	@Shadow protected abstract LevelEntityGetter<Entity> getEntities();
	@Shadow @Final private Minecraft minecraft;
	@Shadow public abstract void addParticle(ParticleOptions p_104706_, double p_104707_, double p_104708_, double p_104709_, double p_104710_, double p_104711_, double p_104712_);

	@Inject(method = "doAnimateTick", at = @At(value = "TAIL"))
	public void animatePlayers(int p_233613_, int p_233614_, int p_233615_, int p_233616_, RandomSource p_233617_, Block MARKER, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
		AABB blockAABB = new AABB(pos);
		this.getEntities().get(blockAABB, (entity) -> {
			if (entity instanceof PlayerAccessor pl && pl.isFullActive() && (entity != this.minecraft.player || !this.minecraft.options.getCameraType().isFirstPerson())) {
				for (BlockInPlayer2 bl : pl.getBlocksData2().values()) {
					Vec3 real = MorphUtils.getCetneredRealBlockPos(pl, bl.getOffset());
					if (blockAABB.contains(real)) {
						bl.animateTick();
						if (MARKER == bl.getBlockState().getBlock()) {
							this.addParticle(
									new BlockParticleOption(ParticleTypes.BLOCK_MARKER, bl.getBlockState()),
									real.x,
									real.y,
									real.z,
									0.0D,
									0.0D,
									0.0D);
						}
					}
				}
			}
		});
	}

	@ModifyVariable(method = "calculateBlockTint", at = @At("HEAD"))
	public BlockPos getRealPos(BlockPos value) {
		return InPlayerBlockPos.checkOnReal(value);
	}

	private boolean BErendering;

	public boolean blockEntityRendering() {
		return this.BErendering;
	}

	public void setBlockEntityRenderingMode(boolean yes) {
		this.BErendering = yes;
	}
}
