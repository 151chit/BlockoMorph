package net.blockomorph.mixins.main.client.graphic;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.MorphedPlayerRenderer;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin implements LevelRendererAccessor {
	@Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Shadow
	protected abstract void checkPoseStack(PoseStack p_109589_);

	@Shadow @Final private RenderBuffers renderBuffers;
	@Shadow @Nullable private ClientLevel level;
	@Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

	@Unique
	private Vec3 sound;

	@Inject(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At(value = "HEAD"))
	public void redirect(ParticleOptions p_109805_, boolean p_109806_, boolean p_109807_, double x, double y, double z, double p_109811_, double p_109812_, double p_109813_, CallbackInfoReturnable<Particle> cir) {
		this.sound = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
	}

	@ModifyVariable(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At(value = "HEAD"), ordinal = 0)
	public double getX(double value) {
		return sound.x;
	}

	@ModifyVariable(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At(value = "HEAD"), ordinal = 1)
	public double getY(double value) {
		return sound.y;
	}

	@ModifyVariable(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At(value = "HEAD"), ordinal = 2)
	public double getZ(double value) {
		return sound.z;
	}

	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks() {
		return this.destructionProgress;
	}

	@Unique
	private final MorphedPlayerRenderer CUSTOM_RENDERER = new MorphedPlayerRenderer();
	@Unique
	private final ArrayList<AbstractClientPlayer> playersToRender = new ArrayList<>();

	@Override
	public void prepareTranslucentPlayersForRender$blockomorph(PoseStack stack, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller) {
		if (this.level != null) {
			profilerFiller.popPush("morphedPlayerTranslucentBlocks");
			for (AbstractClientPlayer player : this.playersToRender) {
				this.renderMorphedPlayer(player, camera, stack, deltaTracker);
			}
		}
		this.playersToRender.clear();
	}

	@Inject(method = "renderEntity", at = @At("HEAD"))
	private void renderPlayer(Entity entity, double p_109519_, double p_109520_, double p_109521_, float p_109522_, PoseStack p_109523_, MultiBufferSource p_109524_, CallbackInfo ci) {
		if (entity instanceof AbstractClientPlayer player) {
			this.playersToRender.add(player);
		}
	}

	@Unique//TEMP FIX TRANSLUCENT
	private void renderMorphedPlayer(AbstractClientPlayer player, Camera camera, PoseStack posestack, DeltaTracker deltaTracker) {
		if (player instanceof PlayerAccessor pl && pl.isFullActive()) {
			Vec3 camPos = camera.getPosition();
			MultiBufferSource.BufferSource buffer = this.renderBuffers.bufferSource();
			float tick = deltaTracker.getGameTimeDeltaPartialTick(!this.level.tickRateManager().isEntityFrozen(player));
			double d0 = Mth.lerp(tick, player.xOld, player.getX());
			double d1 = Mth.lerp(tick, player.yOld, player.getY());
			double d2 = Mth.lerp(tick, player.zOld, player.getZ());
			PlayerRenderer renderer = (PlayerRenderer) this.entityRenderDispatcher.getRenderer(player);
			Vec3 offset = renderer.getRenderOffset(renderer.createRenderState(player, tick));
			posestack.pushPose();
			posestack.translate(d0 - camPos.x + offset.x, d1 - camPos.y + offset.y, d2 - camPos.z + offset.z);
			CUSTOM_RENDERER.renderTranslucentBlocks(pl, posestack, buffer);
			posestack.popPose();
			this.checkPoseStack(posestack);
		}
	}

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
	public void rejectChunkUpdateOnPlayer(int chunkX, int sectionY, int chunkZ, boolean yes, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphPlayerChunk(new ChunkPos(chunkX, chunkZ)))
			ci.cancel();
	}
}
