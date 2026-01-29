package net.blockomorph.platformUtilsImpl;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.network.MainPacket;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.blockomorph.utils.tnt.TntSpawnLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class NeoCommonUtils implements CommonPlatformUtils {

	@Override
	public void litTnt(TntBlock blockVanilla, TntSpawnLevel lv, BlockInPlayer2 block, PlayerAccessor pl) {
		blockVanilla.onCaughtFire(block.getBlockState(), lv, block.getPos(), null, pl.player());
	}

	@Override
	public TntSpawnLevel createLevel(Level orig, BlockState need) {
		return new TntSpawnLevel(orig, need) {
			@Override
			public Collection<PartEntity<?>> dragonParts() {
				return realLevel.dragonParts();
			}

			@Override
			public void setDayTimeFraction(float v) {
				this.realLevel.setDayTimeFraction(v);
			}

			@Override
			public float getDayTimeFraction() {
				return this.realLevel.getDayTimeFraction();
			}

			@Override
			public float getDayTimePerTick() {
				return this.realLevel.getDayTimePerTick();
			}

			@Override
			public void setDayTimePerTick(float v) {
				this.realLevel.setDayTimePerTick(v);
			}
		};
	}

	@Override
	public void loadNbtToBlockEntityOnClient(BlockEntity blockEntity, ClientBoundMorphUpdatePacket pkt, RegistryAccess registries, CompoundTag tag) {
		blockEntity.onDataPacket(pkt.getListener().getConnection(), ClientboundBlockEntityDataPacket.create(blockEntity, (ent, access) -> tag), registries);
	}

	@Override
	public void sendServer(BlockMorphPacket packet) {
		PacketDistributor.sendToServer(new MainPacket(packet));
	}

	@Override
	public void sendPlayer(BlockMorphPacket packet, ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new MainPacket(packet));
	}

	@Override
	public void sendAll(BlockMorphPacket packet) {
		PacketDistributor.sendToAllPlayers(new MainPacket(packet));
	}
}
