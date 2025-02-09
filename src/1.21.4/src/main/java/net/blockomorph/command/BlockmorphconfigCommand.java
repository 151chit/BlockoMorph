
package net.blockomorph.command;

import net.blockomorph.utils.config.*;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.tree.CommandNode;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class BlockmorphconfigCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
	  Config.load();
	  LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("blockmorphconfig").requires((p) -> {
         return p.hasPermission(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig");
      });
      
      for (ConfigInstance<?> op : Config.getInstance().options) {
      	 if (!op.getName().equals("canOperatorModifyConfig"))
            builder = builder.then(op.work(Commands.literal(op.getName()), event.getBuildContext()));
      }
      
      event.getDispatcher().register(builder);
	}
}
