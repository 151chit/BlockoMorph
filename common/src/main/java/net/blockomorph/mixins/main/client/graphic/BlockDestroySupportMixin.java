package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class BlockDestroySupportMixin {

	@Shadow @Final private Minecraft minecraft;

	@Unique
	private ClientLevel self() {
		return (ClientLevel)(Object) this;
	}

	@Inject(method = "addDestroyBlockEffect", at = @At(value = "HEAD"), cancellable = true)
	public void destroy(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
			Vec3 vec = InPlayerBlockPos.checkOnReal(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
			this.destroyBlock(pos, state, vec);
			ci.cancel();
		}
	}
	public void destroyBlock(BlockPos keyPos, BlockState state, Vec3 realPos) {
		if (!state.isAir() && ClientPlatformUtils.INSTANCE.isValidDestroyBlock(this.self(), keyPos, state, this.minecraft.particleEngine)) {
			VoxelShape voxelshape = state.getShape(this.self(), keyPos);
			voxelshape.forAllBoxes((p_172273_, p_172274_, p_172275_, p_172276_, p_172277_, p_172278_) -> {
				double d1 = Math.min(1.0D, p_172276_ - p_172273_);
				double d2 = Math.min(1.0D, p_172277_ - p_172274_);
				double d3 = Math.min(1.0D, p_172278_ - p_172275_);
				int i = Math.max(2, Mth.ceil(d1 / 0.25D));
				int j = Math.max(2, Mth.ceil(d2 / 0.25D));
				int k = Math.max(2, Mth.ceil(d3 / 0.25D));

				for (int l = 0; l < i; ++l) {
					for (int i1 = 0; i1 < j; ++i1) {
						for (int j1 = 0; j1 < k; ++j1) {
							double d4 = ((double) l + 0.5D) / (double) i;
							double d5 = ((double) i1 + 0.5D) / (double) j;
							double d6 = ((double) j1 + 0.5D) / (double) k;
							double d7 = d4 * d1 + p_172273_;
							double d8 = d5 * d2 + p_172274_;
							double d9 = d6 * d3 + p_172275_;
							TerrainParticle particle = new TerrainParticle(
									this.self(),
									realPos.x + d7,
									realPos.y + d8,
									realPos.z + d9,
									d4 - 0.5D,
									d5 - 0.5D,
									d6 - 0.5D,
									state,
									BlockPos.containing(realPos)
							);
							ClientPlatformUtils.INSTANCE.addAdditionalData(particle, keyPos, state);
							this.minecraft.particleEngine.add(particle);
						}
					}
				}

			});
		}
	}

	@Unique
	private Vec3 sound;

	@Inject(method = "doAddParticle", at = @At(value = "HEAD"))
	public void redirect(ParticleOptions particleOptions, boolean bl, boolean bl2, double x, double y, double z, double g, double h, double i, CallbackInfo ci) {
		this.sound = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
	}

	@ModifyVariable(method = "doAddParticle", at = @At(value = "HEAD"), ordinal = 0)
	public double getX(double value) {
		return sound.x;
	}

	@ModifyVariable(method = "doAddParticle", at = @At(value = "HEAD"), ordinal = 1)
	public double getY(double value) {
		return sound.y;
	}

	@ModifyVariable(method = "doAddParticle", at = @At(value = "HEAD"), ordinal = 2)
	public double getZ(double value) {
		return sound.z;
	}
}
