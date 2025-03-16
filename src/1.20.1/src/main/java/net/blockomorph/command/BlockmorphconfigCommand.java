
package net.blockomorph.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
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
