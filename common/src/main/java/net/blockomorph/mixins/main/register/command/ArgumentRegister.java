package net.blockomorph.mixins.main.register.command;

import net.blockomorph.input.command.args.EnumArgument;
import net.blockomorph.input.command.args.FilteredResourceArgument;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ArgumentTypeInfos.class)
public abstract class ArgumentRegister {
	@Shadow @Final private static Map<Class<?>, ArgumentTypeInfo<?, ?>> BY_CLASS;

	@Inject(method = "bootstrap", at = @At("RETURN"))
	private static void register(Registry<ArgumentTypeInfo<?, ?>> registry, CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> cir) {
		registerArgument("enum_argument", new EnumArgument.ContextInfo<>());
		registerArgument("filtered_resource", new FilteredResourceArgument.ContextInfo<>());
	}

	@Unique
	private static void registerArgument(String name, MorphUtils.ArgumentEncoder<?, ?> argumentEncoder) {
		BY_CLASS.put(argumentEncoder.getArgClass(), argumentEncoder);
		Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, MorphUtils.res(name), argumentEncoder);
	}

}
