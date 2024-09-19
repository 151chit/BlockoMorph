
package net.blockomorph.command;

import org.checkerframework.checker.units.qual.s;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.BlockAccessor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.Collections;

@Mod.EventBusSubscriber
public class BlockmorphCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(
				Commands.literal("blockmorph").requires(s -> s.hasPermission(2)).then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext())).then(Commands.argument("targets", EntityArgument.players()).executes(arguments -> {
					return morphBlock(arguments.getSource(), 
					BlockStateArgument.getBlock(arguments, "block").getState(), 
					EntityArgument.getPlayers(arguments, "targets"), 
					((BlockAccessor)BlockStateArgument.getBlock(arguments, "block") ).getTag(),
					true);
				})).executes(arguments -> {
					return morphBlock(arguments.getSource(), 
					BlockStateArgument.getBlock(arguments, "block").getState(), 
					Collections.singleton(arguments.getSource().getPlayerOrException()), 
					((BlockAccessor)BlockStateArgument.getBlock(arguments, "block") ).getTag(),
					false);
				})));
	}

	private static int morphBlock(CommandSourceStack stack, BlockState blockstate, Collection<ServerPlayer> players, CompoundTag tag, boolean many) {
		Block state = blockstate.getBlock();
		for (Entity entityiterator : players) {
			if (entityiterator instanceof PlayerAccessor pl) {
				pl.applyBlockMorph(blockstate, tag);
			}
		}
		if (many) {
			if (players.size() == 1) {
				stack.sendSuccess(() -> {
                return Component.translatable("commands.blockmorph.single", players.iterator().next().getDisplayName(), state.getName());
                }, true);
			} else {
				stack.sendSuccess(() -> {
                return Component.translatable("commands.blockmorph.many", players.size(), state.getName());
                }, true);
			}
		} else {
			stack.sendSuccess(() -> {
            return Component.translatable("commands.blockmorph.you", state.getName());
            }, true);
		}
		return players.size();
	}
}
