package net.blockomorph.utils.platform;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.tnt.TntSpawnLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface CommonPlatformUtils {
	CommonPlatformUtils INSTANCE = ServiceLoader.load(CommonPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load common platform-depended utils, mod cannot run!"));
	void litTnt(TntBlock blockVanilla, TntSpawnLevel lv, BlockInPlayer2 block, PlayerAccessor pl);
	TntSpawnLevel createLevel(Level orig, BlockState need);
	void loadNbtToBlockEntityOnClient(BlockEntity blockEntity, ClientBoundMorphUpdatePacket pkt, RegistryAccess registries, CompoundTag tag);
	void sendServer(BlockMorphPacket packet);
	void sendPlayer(BlockMorphPacket packet, ServerPlayer player);
	void sendAll(BlockMorphPacket packet);

	static void handleUpdateFluidOnEyes(Entity entity, Consumer<FluidState> fluidStateConsumer) {
		AbstractMap.SimpleEntry<PlayerAccessor, BlockInPlayer2> liquid = MorphUtils.getLiquidOnPos(entity, entity.getEyePosition());
		if (liquid != null) {
			BlockInPlayer2 block = liquid.getValue();
			FluidState fluidState = block.getBlockState().getFluidState();
			double y = MorphUtils.getRealBlockPos(liquid.getKey(), block.getOffset()).y;
			double height = fluidState.getHeight(entity.level(), block.getPos());
			if (y + height > entity.getEyeY()) {
				fluidStateConsumer.accept(fluidState);
			}
		}
	}

	static <T> Object2ObjectMap<T, Object[]> handleFluidDetection(Entity thisEntity, Function<T, Boolean> isPushedByFluid, Function<FluidState, Boolean> fluidValid, Function<FluidState, T> convertToSpecialType) {
		Object2ObjectMap<T, Object[]> calcs = new Object2ObjectArrayMap<>();
		AABB aabb = thisEntity.getBoundingBox().deflate(0.001);
		List<Entity> entities = thisEntity.level().getEntities(thisEntity, aabb, EntitySelector.NO_SPECTATORS);
		for (Entity entity : entities) {
			if (entity instanceof PlayerAccessor pl && pl.isFullActive()) {
				pl.getBlocksData2InArea(aabb, (pos, block, realPos) -> {
					if (block.shouldDoFluidAction()) {
						FluidState fluidState = block.getBlockState().getFluidState();
						T specialType = convertToSpecialType.apply(fluidState);
						double fluidHeight = realPos.y + fluidState.getHeight(thisEntity.level(), block.getPos());
						if (fluidValid.apply(fluidState) && fluidHeight >= aabb.minY) {
							Object[] list = calcs.computeIfAbsent(specialType, (o) -> {
								Object[] objs = new Object[3];// 0 - fluidHeight 1 - flowVector 2 - blockCount
								objs[0] = 0.0d;
								objs[1] = Vec3.ZERO;
								objs[2] = 0;
								return objs;
							});
							list[0] = Math.max(fluidHeight - aabb.minY, (double) list[0]);
							if (isPushedByFluid.apply(specialType)) {
								Vec3 flowSpeed = fluidState.getFlow(thisEntity.level(), block.getPos());
								if ((double) list[0] < 0.4) {
									flowSpeed = flowSpeed.scale((double) list[0]);
								}

								list[1] = ((Vec3) list[1]).add(flowSpeed);
								list[2] = (int) list[2] + 1;
							}
						}
					}
				});
			}
		}
		return calcs;
	}

	static <T> void calculateLiquidsPower(Entity thisEntity, Object2ObjectMap<T, Object[]> map, Function<T, Double> fluidTypeToCoefficient, BiConsumer<T, Double> handler) {
		map.forEach((fluidType, objects) -> {
			double fluidHeight = (double) objects[0];
			Vec3 flow = (Vec3) objects[1];
			int count = (int) objects[2];
			if (flow.length() > 0f) {
				if (count > 0) {
					flow = flow.scale((double) 1.0F / (double) count);
				}

				if (!(thisEntity instanceof Player)) {
					flow = flow.normalize();
				}

				Vec3 vec32 = thisEntity.getDeltaMovement();
				flow = flow.scale(fluidTypeToCoefficient.apply(fluidType));
				double d2 = 0.003;
				if (Math.abs(vec32.x) < d2 && Math.abs(vec32.z) < d2 && flow.length() < 0.0045000000000000005) {
					flow = flow.normalize().scale(0.0045000000000000005);
				}

				thisEntity.setDeltaMovement(thisEntity.getDeltaMovement().add(flow));
			}
			handler.accept(fluidType, fluidHeight);
		});
	}
}
