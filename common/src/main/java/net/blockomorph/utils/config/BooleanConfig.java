package net.blockomorph.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.screens.config.renderers.BooleanConfigRenderer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BooleanConfig extends ConfigInstance<Boolean> {
	private static BooleanConfigRenderer RENDERER;

	public BooleanConfig(String name, Boolean initialValue, boolean canOperatorModify, @Nullable Component tip) {
		super(name, initialValue, canOperatorModify, tip);
	}

	@Override
	public void readFromStorage(JsonElement option) {
		this.value = option.getAsBoolean();
	}

	@Override
	public JsonElement getDataForStorage() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void parseFromUser(String value) {
		this.value = Boolean.parseBoolean(value);
	}

	@Override
	public void readFromNetwork(FriendlyByteBuf buf) {
		this.value = buf.readBoolean();
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		buf.writeBoolean(this.value);
	}

	public LiteralArgumentBuilder<CommandSourceStack> buildArgument(LiteralArgumentBuilder<CommandSourceStack> optionNameArgument, CommandBuildContext context, Commands.CommandSelection environment) {
		return optionNameArgument.then(Commands.argument("value", BoolArgumentType.bool()).executes(args -> {
			this.value = BoolArgumentType.getBool(args, "value");
			Config.writeAndSend();
			args.getSource().sendSuccess(() -> {
				return Component.translatable("blockomorph.commands.option_change.default", this.name, this.value.toString());
			}, true);
			return 1;
		}));
	}

	@Override
	public BooleanConfigRenderer getRenderer() {
		if (RENDERER == null) {
			RENDERER = new BooleanConfigRenderer();
		}
		return RENDERER;
	}
}
