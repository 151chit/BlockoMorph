package net.blockomorph.mixins.main.register.command;

import net.blockomorph.input.command.entitySelector.SelectorArgType;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(EntitySelectorOptions.class)
public class SelectorRegister {

	@Shadow
	private static void register(String name, EntitySelectorOptions.Modifier modifier, Predicate<EntitySelectorParser> predicate, Component description) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;register(Ljava/lang/String;Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions$Modifier;Ljava/util/function/Predicate;Lnet/minecraft/network/chat/Component;)V", ordinal = 0))
	private static void reg(CallbackInfo ci) {
		SelectorArgType.register(SelectorRegister::register);
	}
}
