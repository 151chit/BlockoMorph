package net.blockomorph.mixins.main;

import net.blockomorph.command.EnumArgument;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ArgumentTypeInfos.class)
public abstract class EnumArgumentRegister {
	@Shadow @Final private static Map<Class<?>, ArgumentTypeInfo<?, ?>> BY_CLASS;

	@Inject(method = "bootstrap", at = @At("RETURN"))
	private static void register(Registry<ArgumentTypeInfo<?, ?>> registry, CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> cir) {
		ArgumentTypeInfo<?, ?> serializer = new EnumArgument.ContextInfo<>();
		BY_CLASS.put(EnumArgument.class, serializer);
		Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, MorphUtils.res("enum_argument"), serializer);
	}

}
