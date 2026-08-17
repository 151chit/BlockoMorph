package net.blockomorph.input.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class BlockomorphConfigCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
		Config.loadOnServer();
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("blockmorphconfig").requires((p) ->
			p.hasPermission(2) && Config.get().canOperatorModifyConfig.getValue()
		);

		for (ConfigInstance<?> option : Config.get().LINEAR_OPTIONS) {
			if (option.canEditedByOperators()) {
				LiteralArgumentBuilder<CommandSourceStack> optionName = Commands.literal(option.getName());
				optionName = optionName.executes(option.buildGetter());
				builder = builder.then(option.buildArgument(optionName, commandBuildContext, environment));
			}
		}

		dispatcher.register(builder);
	}
}
