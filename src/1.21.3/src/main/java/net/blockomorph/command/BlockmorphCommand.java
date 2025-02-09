
package net.blockomorph.command;

import org.checkerframework.checker.units.qual.s;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.blockomorph.utils.*;
import net.blockomorph.utils.config.*;

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
import com.mojang.brigadier.arguments.BoolArgumentType;

@EventBusSubscriber
public class BlockmorphCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(
				Commands.literal("blockmorph").requires(s -> s.hasPermission(2)).then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext())).then(Commands.argument("targets", EntityArgument.players()).executes(arguments -> {
					return morphBlock(arguments.getSource(), 
					BlockStateArgument.getBlock(arguments, "block").getState(), 
					EntityArgument.getPlayers(arguments, "targets"), 
					((BlockAccessor)BlockStateArgument.getBlock(arguments, "block") ).getTag(),
					true, false, false);
				}).then(Commands.argument("multiblock", BoolArgumentType.bool()).executes(arguments -> {
					return morphBlock(arguments.getSource(), 
					BlockStateArgument.getBlock(arguments, "block").getState(), 
					EntityArgument.getPlayers(arguments, "targets"), 
					((BlockAccessor)BlockStateArgument.getBlock(arguments, "block") ).getTag(),
					true, BoolArgumentType.getBool(arguments, "multiblock"), true);
				}))).executes(arguments -> {
					return morphBlock(arguments.getSource(), 
					BlockStateArgument.getBlock(arguments, "block").getState(), 
					Collections.singleton(arguments.getSource().getPlayerOrException()), 
					((BlockAccessor)BlockStateArgument.getBlock(arguments, "block") ).getTag(),
					false, false, false);
				}))
		);

	}

	private static int morphBlock(CommandSourceStack stack, BlockState blockstate, Collection<ServerPlayer> players, CompoundTag tag, boolean many, boolean mb, boolean mbUse) {
		Block state = blockstate.getBlock();
        if (Config.getInstance() == null) {
			stack.sendFailure(
				Component.literal("Config not loaded, something works like that... :/")
			);
			return 0;
		} else if (!(boolean)Config.getInstance().getValue("advancedMode") && mb) {
			stack.sendFailure(
				Component.translatable("commands.blockmorph.mbUse")
			);
			return 0;
		}
		String mess = MorphUtils.isBannedBlock(blockstate);
		if (!mess.isEmpty()) {
			if (mess.contains("black")) {
				stack.sendFailure(
					Component.translatable("commands.blockmorph.blacklist")
				);
			} else if (mess.contains("white")) {
				stack.sendFailure(
					Component.translatable("commands.blockmorph.whitelist")
				);
			} else if (mess.contains("solid")) {
				stack.sendFailure(
					Component.translatable("commands.blockmorph.solid")
				);
			}
			return 0;
		}
		for (Entity entityiterator : players) {
			if (entityiterator instanceof PlayerAccessor pl) {
				if (mbUse) {
					pl.applyBlockMorph(blockstate, tag, mb);
				} else {
					pl.applyBlockMorph(blockstate, tag);
				}
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
