package net.blockomorph.utils;

import com.mojang.serialization.DataResult;
import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.utils.accessors.ServerPlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.Predicate;


public class MorphUtils {
	public static final String MODID = "blockomorph";
	public static final ResourceKey<DamageType> PLAYER_DESTROYED = ResourceKey.create(Registries.DAMAGE_TYPE, res("player_destroyed"));
	public static final ResourceKey<DamageType> PLAYER_DESTROYED_NULL = ResourceKey.create(Registries.DAMAGE_TYPE, res("player_destroyed_null"));
	private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final boolean ONE_PHASE_PLAYER_RENDER = EarlyLoadingPlatformUtils.INSTANCE.isModLoaded("iris");

	public static Path getGameDir() {
		return EarlyLoadingPlatformUtils.INSTANCE.getGameDir();
	}

	public static ResourceLocation res(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	public static ResourceLocation vanillaRes(String path) {
		return ResourceLocation.withDefaultNamespace(path);
	}


	/****************************PACKET SYSTEM************************************/
	private static final HashMap<ResourceLocation, PacketInfo> handlers = new HashMap<>();

	public static PacketInfo getHandler(ResourceLocation id) {
		return handlers.get(id);
	}

	public static void sendServer(BlockMorphPacket packet) {
		CommonPlatformUtils.INSTANCE.sendServer(packet);
	}

	public static void sendAll(BlockMorphPacket packet) {
		CommonPlatformUtils.INSTANCE.sendAll(packet);
	}

	public static void sendPlayer(BlockMorphPacket packet, ServerPlayer pl) {
		CommonPlatformUtils.INSTANCE.sendPlayer(packet, pl);
	}

	public static void registerPacket(String id, Function<FriendlyByteBuf, BlockMorphPacket> bl, boolean client) {
		ResourceLocation res = ResourceLocation.fromNamespaceAndPath(MODID, id);
		if (handlers.containsKey(res)) {
			throw new IllegalArgumentException("Packet with Id: " + id + " alredy registered!");
		}
		handlers.put(ResourceLocation.fromNamespaceAndPath(MODID, id), new PacketInfo(bl, client));
	}

	public record PacketInfo(Function<FriendlyByteBuf, BlockMorphPacket> packet, boolean isClient) {
	}

	/****************************PACKET SYSTEM************************************/

	public static void doBlockInMorphedPlayerOnPos(@Nullable Entity self, Iterable<Entity> entities, Vec3 pos, BiConsumer<PlayerAccessor, BlockInPlayer2> action) {
		for (Entity entity : entities) {
			if (entity != self && entity instanceof PlayerAccessor pl && pl.isFullActive()) {
				if (entity.getBoundingBox().contains(pos)) {
					pl.getBlocksData2InArea(new AABB(pos, pos), (blockOffset, block, realPos) -> {
						action.accept(pl, block);
					});
				}
			}
		}
	}

	public static boolean needModedHit(Predicate<Class<?>> predicate) {
		if (Config.get().hitReaction.getValue().projectile) {
			Optional<Class<?>> findedClazz = STACK_WALKER.walk(stackFrameStream -> {
				return stackFrameStream.skip(1).filter(frame -> predicate.test(frame.getDeclaringClass())).findFirst().map(StackWalker.StackFrame::getDeclaringClass);
			});
			if (findedClazz.isPresent()) {
				return !Projectile.class.isAssignableFrom(findedClazz.get());
			}
		}
		return true;
	}

	@Nullable
	public static AbstractMap.SimpleEntry<PlayerAccessor, BlockInPlayer2> getLiquidOnPos(Entity entity, Vec3 position) {
		AtomicReference<AbstractMap.SimpleEntry<PlayerAccessor, BlockInPlayer2>> reference = new AtomicReference<>();
		List<Entity> entities = entity.level().getEntities(entity, new AABB(BlockPos.ZERO).move(position.add(-0.5, -0.5, -0.5)), EntitySelector.NO_SPECTATORS);
		MorphUtils.doBlockInMorphedPlayerOnPos(entity, entities, position, (pl, block) -> {
			if (block.shouldDoFluidAction()) {
				reference.set(new AbstractMap.SimpleEntry<>(pl, block));
			}
		});
		return reference.get();
	}

	public static ChunkPos getChangedChunk(ChunkPos orig, boolean isClientSide) {
		Player pl = PlayerMorphedSection.getPlayerByChunkPos(orig, isClientSide);
		if (pl != null) {
			return pl.chunkPosition();
		}
		if (InPlayerBlockPos.isMorphPlayerChunk(orig)) {
			return ChunkPos.ZERO;
		}
		return orig;
	}

	public static void fillListWithPlayerCollisions(EntityGetter lv, @Nullable Entity entity, AABB aabb, List<VoxelShape> shapes) {
		if (!(aabb.getSize() < 1.0E-7D) && !(entity instanceof Projectile && Config.get().hitReaction.getValue().projectile)) {
			AABB finderAABB = aabb.inflate(1).inflate(1.0E-7);
			lv.getEntities(entity, finderAABB, EntitySelector.NO_SPECTATORS).forEach(entity1 -> {
				if (entity1 != entity && EntitySelector.NO_SPECTATORS.test(entity1)) {
					if (entity1 instanceof PlayerAccessor pl && pl.isFullActive()){
						addCustomShapes(shapes, pl, finderAABB, aabb);
					} if (false && entity instanceof PlayerAccessor pl && pl.isFullActive()) {
						AABB playerBox = entity1.getBoundingBox();
						if (playerBox.intersects(aabb))
							shapes.add(Shapes.create(playerBox));
					}
				}
			});
		}
	}

	private static void addCustomShapes(List<VoxelShape> builder, PlayerAccessor pl, AABB finderBox, AABB realBox) {
		VoxelShape playerShape = Shapes.create(realBox);
		pl.getBlocksData2InArea(finderBox, (pos, block, realPos) -> {
			VoxelShape shape = pl.getShape(pos, realPos);
			if (!shape.isEmpty() && Shapes.joinIsNotEmpty(shape, playerShape, BooleanOp.AND)) {
				builder.add(shape);
			}
		});
	}

	public static Predicate<String> blockPredicate() {
		return value -> {
			DataResult<ResourceLocation> result = ResourceLocation.read(value);
			if (result.result().isPresent()) {
				return BuiltInRegistries.BLOCK.containsKey(result.result().get());
			}
			return false;
		};
	}

	public static Vec3 getRealBlockPos(PlayerAccessor original, InPlayerBlockPos offset) {
		return getRealBlockPos(original, new Vec3(offset.x, offset.y, offset.z));
	}

	public static Vec3 getRealBlockPos(PlayerAccessor original, Vec3 offset) {
		AABB aabb = original.player().getBoundingBox();
		InPlayerBlockPos minPos = original.minPos();

		double deltaX = offset.x - (double) minPos.getX();
		double deltaY = offset.y - (double) minPos.getY();
		double deltaZ = offset.z - (double) minPos.getZ();

		double globalX = aabb.minX + deltaX;
		double globalY = aabb.minY + deltaY;
		double globalZ = aabb.minZ + deltaZ;

		return new Vec3(globalX, globalY, globalZ);
	}

	public static void distanceTo(Vec3 from, Vec3 to, boolean sqr, double offset, DoubleConsumer action) {
		if ((InPlayerBlockPos.isMorphedPlayerX(from.x) || InPlayerBlockPos.isMorphedPlayerX(to.x)) && action != null) {
			from = InPlayerBlockPos.checkOnReal(from);
			to = InPlayerBlockPos.checkOnReal(to);
			double d0 = from.x + offset - to.x;
			double d1 = from.y + offset - to.y;
			double d2 = from.z + offset - to.z;
			double result = d0 * d0 + d1 * d1 + d2 * d2;
			if (!sqr)
				result = Math.sqrt(result);
			action.accept(result);
		}
	}

	public static void executeMorphedBlockShapeUpdate(LevelAccessor levelAccessor, Direction direction, BlockPos blockPos, BlockPos blockPos2, BlockState blockState, int i, int j, BlockState external) {
		if ((i & 128) == 0 || !external.is(Blocks.REDSTONE_WIRE)) {
			BlockState blockState3 = external.updateShape(levelAccessor, levelAccessor, blockPos, direction, blockPos2, blockState, levelAccessor.getRandom());
			Block.updateOrDestroy(external, blockState3, levelAccessor, blockPos, i, j);
		}
	}

	public static Vec3 getCetneredRealBlockPos(PlayerAccessor original, InPlayerBlockPos offset) {
		Vec3 vec = getRealBlockPos(original, offset);
		return new Vec3(vec.x + 0.5, vec.y + 0.5, vec.z + 0.5);
	}

	public static boolean isAdventureCanBreak(PlayerAccessor pl, Player attacker, InPlayerBlockPos hitPart) {
		BlockInPlayer2 block = pl.getBlocksData2().get(hitPart);
		if (block != null) {
			BlockInWorld blockinworld = new BlockInWorld(pl.player().level(), block.getPos(), true);
			ItemStack itemstack = attacker.getMainHandItem();
			return !itemstack.isEmpty() && (itemstack.canBreakBlockInAdventureMode(blockinworld) || itemstack.canPlaceOnBlockInAdventureMode(blockinworld));
		}
		return false;
	}

	public static boolean onPlayerAttacked(DamageSource damage, Entity attacked) {
		if (attacked instanceof PlayerAccessor pl) {
			if (damage.getDirectEntity() instanceof Player) {
				if (Config.get().hitReaction.getValue().hand) {
					return false;
				}
			} else if (damage.getDirectEntity() instanceof Projectile) {
				if (Config.get().hitReaction.getValue().projectile) {
					return false;
				}
			}
			boolean noTnt = pl.getTnt() == null;
			boolean tntBlock = pl.getBlockState(InPlayerBlockPos.ZERO).getBlock() instanceof TntBlock;
			boolean tntDamage =
					damage.is(DamageTypes.PLAYER_EXPLOSION) ||
							damage.is(DamageTypes.EXPLOSION);
			boolean allowed =
					damage.is(DamageTypes.GENERIC_KILL) ||
							damage.is(DamageTypes.FELL_OUT_OF_WORLD);
			if (pl.isActive()) {
				if (allowed || (!tntBlock && tntDamage)) {
					destroy(pl, damage.getEntity());
				} else if (tntBlock && tntDamage && noTnt) {
					pl.setTnt();
					PrimedTnt tnt = pl.getTnt();
					if (tnt != null) {
						tnt.setFuse(tnt.getFuse() / 2);
					}
				}
				return !(damage.is(PLAYER_DESTROYED) || damage.is(PLAYER_DESTROYED_NULL));
			}
		}
		return false;
	}

	public static boolean needRejectUse(Level lv, BlockHitResult block) {
		if (InPlayerBlockPos.isMorphedPlayerX(block.getBlockPos().getX())) {
			BlockState state = lv.getBlockState(block.getBlockPos());
			ConfigEnums.UseMode mode = Config.get().useMode.getValue();
			switch (mode) {
				case DISABLED -> {
					return true;
				}
				case VANILLA -> {
					ResourceLocation res = BuiltInRegistries.BLOCK.getKey(state.getBlock());
					return !res.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
				}
			}
		}
		return false;
	}

	public static UseOnContext checkOnRealIfOut(UseOnContext ctx, ItemStack stack) {
		if (stack.getItem() instanceof BlockItem && InPlayerBlockPos.isMorphedPlayerX(ctx.getClickedPos().getX())) {
			ConfigEnums.PlaceMode mode = Config.get().placeMode.getValue();
			if (mode == ConfigEnums.PlaceMode.OUT) {
				Vec3 realHit = InPlayerBlockPos.checkOnReal(ctx.getClickLocation());
				realHit = toDirection(realHit, ctx.getClickedFace());
				BlockHitResult hit = new BlockHitResult(realHit, ctx.getClickedFace(), BlockPos.containing(realHit), ctx.isInside());
				return new UseOnContext(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getItemInHand(), hit) {};
			}
		}
		return ctx;
	}

	private static Vec3 toDirection(Vec3 vec, Direction dir) {
		Vec3i step = dir.getUnitVec3i();
		double x = switch (step.getX()) {
			case 1 -> Math.ceil(vec.x) + 1.0E-7;
			case -1 -> Math.floor(vec.x) - 1.0E-7;
			default -> vec.x;
		};
		double y = switch (step.getY()) {
			case 1 -> Math.ceil(vec.y) + 1.0E-7;
			case -1 -> Math.floor(vec.y) - 1.0E-7;
			default -> vec.y;
		};
		double z = switch (step.getZ()) {
			case 1 -> Math.ceil(vec.z) + 1.0E-7;
			case -1 -> Math.floor(vec.z) - 1.0E-7;
			default -> vec.z;
		};
		return new Vec3(x, y, z);
	}

	public static boolean canOpenMenuIn(PlayerAccessor pl, InPlayerBlockPos offset) {
		BlockInPlayer2 block = pl.getBlocksData2().get(offset);
		if (block != null) {
			MenuProvider pr = block.getBlockState().getMenuProvider(pl.player().level(), block.getPos());
			return pr != null;
		}
		return false;
	}

	public static boolean canOpenConfig() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.hasPermissions(2) && Config.get().canOperatorModifyConfig.getValue();
	}

	public static ConfigEnums.ScreenAccess getScreenAccess(Player player) {
		if (player != null && player.hasPermissions(2)) return ConfigEnums.ScreenAccess.ALL;
		return Config.get().screenAccess.getValue();
	}

	public static void destroy(PlayerAccessor mob_pl, @Nullable Entity attacker) {
		LivingEntity mob = (Player) mob_pl;
		Holder<DamageType> damage = mob.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).
				getOrThrow(attacker == null ? PLAYER_DESTROYED_NULL : PLAYER_DESTROYED);
		DamageSource damageSource = new DamageSource(damage, attacker);
		mob.getCombatTracker().recordDamage(damageSource, Float.MAX_VALUE);
		mob.setHealth(0);
		mob.die(damageSource);
		if (mob_pl instanceof ServerPlayer pl && pl.hasClientLoaded() && pl instanceof ServerPlayerAccessor acc) {
			try {
				Component deathMessage = mob.getCombatTracker().getDeathMessage();
				pl.connection.send(new ClientboundPlayerCombatKillPacket(pl.getId(), deathMessage));
				pl.level().getServer().getPlayerList().broadcastSystemMessage(deathMessage, false);
				if (!pl.isSpectator()) acc.dropAllDeathLoot$blockomorph(pl.level(), damageSource);
				mob.getCombatTracker().recheckStatus();
				pl.setClientLoaded(false);
			} catch (Throwable ex) {
				mob_pl.applyBlockMorph(Blocks.AIR.defaultBlockState(), null, BannedBlock.Source.SYSTEM);
				LOGGER.error("While unmorph killing an exception occurred! Something might not work: ", ex);
			}
		}
	}
}
