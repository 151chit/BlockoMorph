package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.DamageHandler;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.LevelAcc;
import net.blockomorph.utils.accessors.ServerLevelAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements ServerLevelAccessor {
	@Unique private Set<ResourceKey<DamageType>> allowedDamages;

	protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
	}

	@Override
	public Set<ResourceKey<DamageType>> formAngGetTntDamages$blockomorph() {
		if (this.allowedDamages == null) {
			Set<ResourceKey<DamageType>> damages = this.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).entrySet().stream().filter(entry -> {
				return entry.getValue().effects() == DamageEffects.BURNING;
			}).map(Map.Entry::getKey).collect(Collectors.toSet());
			damages.addAll(DamageHandler.TNT_DAMAGE);
			this.allowedDamages = Collections.unmodifiableSet(damages);
		}
		return this.allowedDamages;
	}

	@Inject(method = "blockEvent", at = @At(value = "HEAD"), cancellable = true)
	public void blockEvent(BlockPos pos, Block block, int a, int b, CallbackInfo ci) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			ci.cancel();
			pl.prepareSync(new PlayerAccessor.InPlayerBlockEventData(realPos, new BlockEventData(pos, block, a, b)));
		}, ci::cancel, LevelAcc.ofSv(this));
	}

	@Inject(method = "destroyBlockProgress", at = @At(value = "HEAD"), cancellable = true)
	public void sendCracks(int playerId, BlockPos pos, int progress, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
			ci.cancel();
			ServerChunkCache cache = LevelAcc.ofSv(this).getChunkSource();
			Entity pl = LevelAcc.ofSv(this).getEntity(playerId);
			if (pl instanceof ServerPlayer player)
				cache.sendToTrackingPlayers(player, new ClientboundBlockDestructionPacket(playerId, pos, progress));
		}
	}


	@Unique
	private Vec3 sound;

	@Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At(value = "HEAD"))
	public void redirect(Entity entity, double x, double y, double z, Holder<SoundEvent> holder, SoundSource soundSource, float g, float h, long l, CallbackInfo ci) {
		this.sound = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
	}

	@ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At(value = "HEAD"), ordinal = 0)
	public double getX(double value) {
		return sound.x;
	}

	@ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At(value = "HEAD"), ordinal = 1)
	public double getY(double value) {
		return sound.y;
	}

	@ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At(value = "HEAD"), ordinal = 2)
	public double getZ(double value) {
		return sound.z;
	}

	@Unique
	private Vec3 explosion;

	@Inject(method = "explode", at = @At(value = "HEAD"))
	public void redirect(Entity entity, DamageSource damageSource, ExplosionDamageCalculator explosionDamageCalculator, double x, double y, double z, float g, boolean bl, Level.ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions2, WeightedList<ExplosionParticleInfo> weightedList, Holder<SoundEvent> holder, CallbackInfo ci) {
		this.explosion = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
	}

	@ModifyVariable(method = "explode", at = @At(value = "HEAD"), ordinal = 0)
	public double getXp(double value) {
		return explosion.x;
	}

	@ModifyVariable(method = "explode", at = @At(value = "HEAD"), ordinal = 1)
	public double getYp(double value) {
		return explosion.y;
	}

	@ModifyVariable(method = "explode", at = @At(value = "HEAD"), ordinal = 2)
	public double getZp(double value) {
		return explosion.z;
	}
}
