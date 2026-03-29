package net.blockomorph.utils;

import net.blockomorph.utils.accessors.ServerLevelAccessor;
import net.blockomorph.utils.accessors.ServerPlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DamageHandler {
	public static final ResourceKey<DamageType> PLAYER_DESTROYED = ResourceKey.create(Registries.DAMAGE_TYPE, MorphUtils.res("player_destroyed"));
	public static final ResourceKey<DamageType> PLAYER_DESTROYED_NULL = ResourceKey.create(Registries.DAMAGE_TYPE, MorphUtils.res("player_destroyed_null"));
	public static final Set<ResourceKey<DamageType>> TNT_DAMAGE = Set.of(DamageTypes.PLAYER_EXPLOSION, DamageTypes.EXPLOSION);

	public static boolean needSuppressDamage(DamageSource damage, float hp, Entity attacked) {
		if (attacked instanceof PlayerAccessor pl) {
			if (checkDamageConfig(damage)) return false;
			if (pl.isActive()) {
				if (tntNotHandled(pl, damage, hp)) {
					Holder<DamageType> holder = damage.typeHolder();
					Optional<ResourceKey<DamageType>> key = holder instanceof Holder.Reference<DamageType> reference && reference.isBound() ? 
							reference.unwrapKey() : pl.player().level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getResourceKey(damage.type());
					if (key.isEmpty()) return true;
					if (allowDamage(damage, hp, Set.of(key.get()))) {
						destroyAnimation(pl);
						DamageHandler.destroy(pl, damage.getEntity());
					}
					return !(damage.is(PLAYER_DESTROYED) || damage.is(PLAYER_DESTROYED_NULL));
				}
				return true;
			}
		}
		return false;
	}

	private static boolean tntNotHandled(PlayerAccessor pl, DamageSource damage, float hp) {
		boolean noActiveTnt = pl.getTnt() == null;
		boolean hasTntBlockInCenter = pl.getBlockState(InPlayerBlockPos.ZERO).getBlock() instanceof TntBlock;
		boolean allowDamage = allowDamage(damage, hp, pl.player().level() instanceof ServerLevelAccessor lv ? lv.formAngGetTntDamages$blockomorph() : TNT_DAMAGE);
		if (allowDamage) {
			if (!hasTntBlockInCenter) return true;
			if (noActiveTnt) {
				pl.setTnt();
				PrimedTnt tnt = pl.getTnt();
				if (tnt != null) for (ResourceKey<DamageType> key : TNT_DAMAGE) {
					if (damage.typeHolder().is(key)) {
						int i = tnt.getFuse();
						tnt.setFuse(pl.player().level().random.nextInt(i / 4) + i / 8);
						break;
					}
				}
			}
			return false;
		}
		return true;
	}
	
	private static boolean allowDamage(DamageSource damage, float hp, Set<ResourceKey<DamageType>> need) {
		Map<ResourceLocation, Integer> allowed = Config.get().allowedDamages.getValue();
		for (ResourceKey<DamageType> key : need) {
			if (damage.is(key) && allowed.containsKey(key.location()) && hp >= allowed.get(key.location())) return true;
		}
		return false;
	}

	private static boolean checkDamageConfig(DamageSource damage) {
		if (damage.getDirectEntity() instanceof Player) {
			return Config.get().hitReaction.getValue().hand;
		} else if (damage.getDirectEntity() instanceof Projectile) {
			return Config.get().hitReaction.getValue().projectile;
		}
		return false;
	}

	private static void destroyAnimation(PlayerAccessor pl) {
		pl.getBlocksData2().values().forEach(block -> {
			pl.player().level().levelEvent(2001, block.getPos(), Block.getId(block.getBlockState()));
		});
	}

	public static void destroy(PlayerAccessor morphed, @Nullable Entity attacker) {
		Player player = morphed.player();
		Holder<DamageType> damage = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).
				getOrThrow(attacker == null ? PLAYER_DESTROYED_NULL : PLAYER_DESTROYED);
		DamageSource damageSource = new DamageSource(damage, attacker);
		player.setAbsorptionAmount(0);
		player.setInvulnerable(false);
		player.getCombatTracker().recordDamage(damageSource, Float.MAX_VALUE);
		player.setHealth(0);
		player.die(damageSource);
		if (morphed instanceof ServerPlayer pl && pl instanceof ServerPlayerAccessor acc && !acc.isDeadAndReset$blockomorph()) {
			try {
				Component deathMessage = player.getCombatTracker().getDeathMessage();
				pl.connection.send(new ClientboundPlayerCombatKillPacket(pl.getId(), deathMessage));
				Objects.requireNonNull(pl.level().getServer()).getPlayerList().broadcastSystemMessage(deathMessage, false);
				if (!pl.isSpectator()) acc.dropAllDeathLoot$blockomorph(pl.serverLevel(), damageSource);
				player.getCombatTracker().recheckStatus();
			} catch (Throwable ex) {
				morphed.applyBlockMorph(Blocks.AIR.defaultBlockState(), null, BannedBlock.Source.SYSTEM);
				MorphUtils.LOGGER.error("While unmorph killing an exception occurred! Something might not work: ", ex);
			}
		}
	}
}
