package net.blockomorph.utils;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.DataResult;
import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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
import java.util.*;
import java.util.function.*;


public class MorphUtils {
	public static final String MODID = "blockomorph";
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

	public interface ArgumentEncoder<ARG extends ArgumentType<?>, TEMPLATE extends ArgumentTypeInfo.Template<ARG>> extends ArgumentTypeInfo<ARG, TEMPLATE> {
		Class<?> getArgClass();
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
			PlayersMultiSectionStorage.fromLevel(lv).findMorphed(entity, finderAABB).forEach(pl -> {
				addCustomShapes(shapes, pl, finderAABB, aabb);
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

	public static void executeMorphedBlockShapeUpdate(LevelAccessor level, Direction direction, BlockState state, BlockPos offsetted, BlockPos origin, int flags, int distance, BlockState external) {
		BlockState blockstate1 = external.updateShape(direction, state, level, offsetted, origin);
		Block.updateOrDestroy(external, blockstate1, level, offsetted, flags, distance);
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
		Vec3i step = dir.getNormal();
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
}
