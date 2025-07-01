
package net.blockomorph.command;

import net.blockomorph.utils.accessors.BlockAccessor;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.blockomorph.utils.*;
import net.blockomorph.utils.config.*;

import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

import java.util.Collection;

@EventBusSubscriber
public class BlockmorphCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(
				Commands.literal("blockmorph").requires(s -> s.hasPermission(2)).then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext())).then(Commands.argument("targets", EntityArgument.players()).executes(arguments -> {
					return morphMany(arguments.getSource(),
							BlockStateArgument.getBlock(arguments, "block").getState(),
							EntityArgument.getPlayers(arguments, "targets"),
							BlockAccessor.of(BlockStateArgument.getBlock(arguments, "block") ).getTag());
				})).executes(arguments -> {
					return morphSingle(arguments.getSource(),
							BlockStateArgument.getBlock(arguments, "block").getState(),
							arguments.getSource().getPlayerOrException(),
							BlockAccessor.of(BlockStateArgument.getBlock(arguments, "block")).getTag());
				}))
		);
	}

	private static int morphMany(CommandSourceStack stack, BlockState blockstate, Collection<ServerPlayer> players, CompoundTag tag) {
		if (checkConfig(stack))
			return 0;
		MorphUtils.BannedBlock global = MorphUtils.isBannedBlock(blockstate, null);
		if (global != null) {
			stack.sendFailure(
					global.text()
			);
			return 0;
		}
		int success = 0;
		for (ServerPlayer entity : players) {
			if (entity instanceof PlayerAccessor pl) {
				MorphUtils.BannedBlock reason = pl.applyBlockMorph(blockstate, tag);
				if (reason == null)
					success++;
			}
		}
		final int result = success;
		stack.sendSuccess(() -> Component.translatable("commands.blockmorph.many", result, blockstate.getBlock().getName()), true);
		return players.size();
	}

	private static int morphSingle(CommandSourceStack stack, BlockState blockstate, ServerPlayer player, CompoundTag tag) {
		if (checkConfig(stack))
			return 0;
		MorphUtils.BannedBlock reason = PlayerAccessor.of(player).applyBlockMorph(blockstate, tag);
		if (reason != null) {
			stack.sendFailure(
					reason.text()
			);
			return 0;
		}
		stack.sendSuccess(() -> Component.translatable("commands.blockmorph.you", blockstate.getBlock().getName()), true);
		return 1;
	}

	private static boolean checkConfig(CommandSourceStack stack) {
		if (Config.getInstance() == null) {
			stack.sendFailure(
					Component.literal("Config not loaded, something works like that... :/")
			);
			return true;
		}
		return false;
	}
}