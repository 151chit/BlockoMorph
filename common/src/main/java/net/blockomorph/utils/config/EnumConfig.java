package net.blockomorph.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.command.EnumArgument;
import net.blockomorph.screens.config.ConfigRenderer;
import net.blockomorph.screens.config.renderers.EnumConfigRenderer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnumConfig<T extends Enum<T>> extends ConfigInstance<T> {
	private static EnumConfigRenderer RENDERER;
	private final Class<T> classType;

	public EnumConfig(String name, T initialValue, boolean canOperatorModify, @Nullable Component tip) {
		super(name, initialValue, canOperatorModify, tip);
		this.classType = this.value.getDeclaringClass();
	}

	public Class<T> getEnumClass() {
		return this.classType;
	}

	@Override
	public void readFromStorage(JsonElement option) {
		this.parseFromUser(option.getAsString());
	}

	@Override
	public JsonElement getDataForStorage() {
		return new JsonPrimitive(this.value.name());
	}

	@Override
	public void parseFromUser(String input) {
		try {
			this.value = Enum.valueOf(this.classType, input);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid option value for option: " + this.getName() + ", value: " + input);
		}
	}

	@Override
	public void readFromNetwork(FriendlyByteBuf buf) {
		this.value = buf.readEnum(this.classType);
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		buf.writeEnum(this.value);
	}

	public LiteralArgumentBuilder<CommandSourceStack> buildArgument(LiteralArgumentBuilder<CommandSourceStack> optionNameArgument, CommandBuildContext context, Commands.CommandSelection environment) {
		return optionNameArgument.then(Commands.argument("value", EnumArgument.enumArg(this.classType)).executes(args -> {
			this.value = EnumArgument.getEnum(args, "value", this.classType);
			Config.writeAndSend();
			args.getSource().sendSuccess(() -> {
				return Component.translatable("blockomorph.commands.option_change.default", this.name, this.value.toString());
			}, true);
			return 1;
		}));
	}

	@Override
	public ConfigRenderer<?> getRenderer() {
		if (RENDERER == null) {
			RENDERER = new EnumConfigRenderer();
		}
		return RENDERER;
	}
}
