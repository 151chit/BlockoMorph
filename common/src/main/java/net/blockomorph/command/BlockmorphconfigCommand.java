package net.blockomorph.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class BlockmorphconfigCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
		Config.loadOnServer();
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("blockmorphconfig").requires((p) -> {
			return MorphUtils.hasBlockMorphActionsPermissions(p.permissions(), MorphUtils.AllowType.CONFIG_COMMAND) && Config.get().canOperatorModifyConfig.getValue();
		});

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
