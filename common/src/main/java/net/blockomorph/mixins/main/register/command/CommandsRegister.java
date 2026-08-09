package net.blockomorph.mixins.main.register.command;

import com.mojang.brigadier.CommandDispatcher;
import net.blockomorph.input.command.commands.BlockomorphCommand;
import net.blockomorph.input.command.commands.BlockomorphConfigCommand;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Commands.class)
public class CommandsRegister {
	@Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

	@FastInject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"))
	private boolean register(Commands.CommandSelection commandSelection, CommandBuildContext context) {
		BlockomorphCommand.register(this.dispatcher, context);
		BlockomorphConfigCommand.register(this.dispatcher, context, commandSelection);
		return true;
	}
}
