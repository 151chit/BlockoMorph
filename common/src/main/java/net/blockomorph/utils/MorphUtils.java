package net.blockomorph.utils;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.DataResult;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.enums.ConfigEnums;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.*;


public class MorphUtils {
	public static final String MODID = "blockomorph";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static Path getGameDir() {
		return EarlyLoadingPlatformService.INSTANCE.getGameDir();
	}

	public static Identifier res(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}

	public static Identifier vanillaRes(String path) {
		return Identifier.withDefaultNamespace(path);
	}


	/****************************PACKET SYSTEM************************************/

	public static void sendServer(BlockMorphPacket packet) {
		Client.send(packet.toVanillaServerBound());
	}

	/****************************PACKET SYSTEM************************************/

	public interface ArgumentEncoder<ARG extends ArgumentType<?>, TEMPLATE extends ArgumentTypeInfo.Template<ARG>> extends ArgumentTypeInfo<ARG, TEMPLATE> {
		Class<?> getArgClass();
	}

	public static boolean hasBlockMorphActionsPermissions(PermissionSet set, AllowType type) {
		return Commands.LEVEL_GAMEMASTERS.check(set);
	}

	public enum AllowType {
		CONFIG_SCREEN,
		MORPH_SCREENS,
		MORPH_COMMAND,
		CONFIG_COMMAND
	}

	public static Predicate<String> blockPredicate() {
		return value -> {
			DataResult<Identifier> result = Identifier.read(value);
			if (result.result().isPresent()) {
				return BuiltInRegistries.BLOCK.containsKey(result.result().get());
			}
			return false;
		};
	}

	public static boolean canUseMorphLogicForProjectile(Entity activator) {
		if (Config.get().hitReaction.getValue().projectile) {
			return !(activator instanceof Projectile);
		}
		return true;
	}

	public static BlockState lockExternalMorphedGetter(BlockState orig, BlockPos bounded) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(bounded);
		if (pl != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(bounded);
			if (posIn != -1) return pl.getBlockState(posIn);
		}
		return orig;
	}

	public enum Client {;
		public static Level getLevel() {
			return GuiUtils.MC.level;
		}

		public static void send(Packet<? extends ServerCommonPacketListener> pkt) {
			if (GuiUtils.MC.getConnection() != null) GuiUtils.MC.getConnection().send(pkt);
		}
	}

	public static ConfigEnums.ScreenAccess getScreenAccess(Player player) {
		if (player != null && hasBlockMorphActionsPermissions(player.permissions(), AllowType.MORPH_SCREENS)) return ConfigEnums.ScreenAccess.ALL;
		return Config.get().screenAccess.getValue();
	}
}
