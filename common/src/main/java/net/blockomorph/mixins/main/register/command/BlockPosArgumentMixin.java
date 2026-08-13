package net.blockomorph.mixins.main.register.command;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockomorph.input.command.MorphedPlayerCoordinates;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockPosArgument.class)
public class BlockPosArgumentMixin {

	@WrapOperation(method = "getBlockPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/coordinates/Coordinates;getBlockPos(Lnet/minecraft/commands/CommandSourceStack;)Lnet/minecraft/core/BlockPos;"))
	private static BlockPos checkPlayerKeyPos(Coordinates instance, CommandSourceStack sender, Operation<BlockPos> original) throws Throwable {
		try {
			return original.call(instance, sender);
		} catch (MorphedPlayerCoordinates.ContainerException e) {
			throw e.getCause();
		}
	}

	@FastInject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/coordinates/Coordinates;", at = @At("HEAD"))
	private Object parse(StringReader reader) throws CommandSyntaxException {
		MorphedPlayerCoordinates coords = MorphedPlayerCoordinates.parse(reader);
		if (coords != null) return coords;
		return FastInject.CONTINUE_EXECUTION;
	}

	@FastInject(method = "listSuggestions", at = @At("HEAD"))
	private <S> Object suggest(CommandContext<S> context, SuggestionsBuilder builder) {
		var suggests = MorphedPlayerCoordinates.parseSuggestion(context, builder);
		if (suggests != null) return suggests;
		return FastInject.CONTINUE_EXECUTION;
	}
}
