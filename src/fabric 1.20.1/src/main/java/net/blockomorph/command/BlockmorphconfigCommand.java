
package net.blockomorph.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class BlockmorphconfigCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
	  Config.load();
	  LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("blockmorphconfig").requires((p) -> {
         return p.hasPermission(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig");
      });
      
      for (ConfigInstance<?> op : Config.getInstance().options) {
      	 if (!op.getName().equals("canOperatorModifyConfig"))
            builder = builder.then(op.work(Commands.literal(op.getName()), commandBuildContext));
      }
      
      dispatcher.register(builder);
	}
}
