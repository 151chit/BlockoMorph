
package net.blockomorph.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blockomorph.utils.*;
import net.blockomorph.utils.accessors.BlockAccessor;
import net.blockomorph.utils.config.*;

import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.commands.CommandBuildContext;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;

public class BlockmorphCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
		dispatcher.register(
				Commands.literal("blockmorph").requires(s -> s.hasPermission(2)).then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("targets", EntityArgument.players()).executes(arguments -> {
					SavedBlock block = getBlockForPos(arguments);
					return morphMany(arguments.getSource(),
							block.getState(),
							EntityArgument.getPlayers(arguments, "targets"),
							block.getTag());
				})).executes(arguments -> {
					SavedBlock block = getBlockForPos(arguments);
					return morphSingle(arguments.getSource(),
							block.getState(),
							arguments.getSource().getPlayerOrException(),
							block.getTag());
				})).then(Commands.argument("block", BlockStateArgument.block(commandBuildContext)).then(Commands.argument("targets", EntityArgument.players()).executes(arguments -> {
					return morphMany(arguments.getSource(),
							BlockStateArgument.getBlock(arguments, "block").getState(),
							EntityArgument.getPlayers(arguments, "targets"),
							BlockAccessor.of(BlockStateArgument.getBlock(arguments, "block")).getTag());
				})).executes(arguments -> {
					return morphSingle(arguments.getSource(),
							BlockStateArgument.getBlock(arguments, "block").getState(),
							arguments.getSource().getPlayerOrException(),
							BlockAccessor.of(BlockStateArgument.getBlock(arguments, "block")).getTag());
				}))
		);
	}

	private static SavedBlock getBlockForPos(CommandContext<CommandSourceStack> arguments) throws CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(arguments, "pos");
		Level level = arguments.getSource().getLevel();
		BlockState blockState = level.getBlockState(pos);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		CompoundTag tag = null;
		if (blockEntity != null) {
			try {
				tag = blockEntity.saveWithoutMetadata(level.registryAccess());
			} catch (Throwable e) {
				arguments.getSource().sendFailure(Component.literal(e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
			}
		}
		return new SavedBlock(blockState, tag, null);
	}

	private static int morphMany(CommandSourceStack stack, BlockState blockstate, Collection<ServerPlayer> players, CompoundTag tag) throws CommandSyntaxException {
		if (checkConfig(stack))
			return 0;
		BannedBlock global = BannedBlock.isBannedBlock(blockstate, null, BannedBlock.Source.COMMAND);
		if (global != null) {
			stack.sendFailure(
					global.text()
			);
			return 0;
		}
		int success = 0;
		for (ServerPlayer entity : players) {
			if (entity instanceof PlayerAccessor pl) {
				BannedBlock reason = pl.applyBlockMorph(blockstate, tag, BannedBlock.Source.COMMAND);
				if (reason == null)
					success++;
			}
		}
		if (success == 0) {
			throw EntityArgument.NO_PLAYERS_FOUND.create();
		}
		final int result = success;
		stack.sendSuccess(() -> Component.translatable("blockomorph.morphCommand.many", result, blockstate.getBlock().getName()), true);
		return players.size();
	}

	private static int morphSingle(CommandSourceStack stack, BlockState blockstate, ServerPlayer player, CompoundTag tag) {
		if (checkConfig(stack))
			return 0;
		BannedBlock reason = PlayerAccessor.of(player).applyBlockMorph(blockstate, tag, BannedBlock.Source.COMMAND);
		if (reason != null) {
			stack.sendFailure(
					reason.text()
			);
			return 0;
		}
		stack.sendSuccess(() -> Component.translatable("blockomorph.morphCommand.single", blockstate.getBlock().getName()), true);
		return 1;
	}

	private static boolean checkConfig(CommandSourceStack stack) {
		if (Config.get() == null) {
			stack.sendFailure(
					Component.literal("Config not loaded, something works like that... :/")
			);
			return true;
		}
		return false;
	}
}
