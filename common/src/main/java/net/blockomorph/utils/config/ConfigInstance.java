package net.blockomorph.utils.config;

import com.google.gson.JsonElement;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.screens.config.ConfigRenderer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ConfigInstance<T> {
	private final List<DataFixer<T>> fixers = new ArrayList<>();
	protected final String name;
	protected T value;
	private final Component tip;
	private final boolean canOperatorModify;

	protected ConfigInstance(String name, T initialValue, boolean canOperatorModify, @Nullable Component tip) {
		this.name = name;
		this.value = initialValue;
		this.tip = tip;
		this.canOperatorModify = canOperatorModify;
	}

	public String getName() {
		return this.name;
	}

	@Nullable
	public Component getTooltip() {
		return this.tip;
	}

	public abstract void readFromStorage(JsonElement option);

	public abstract JsonElement getDataForStorage();

	public abstract void parseFromUser(ServerPlayer ctx, String value);

	public abstract void readFromNetwork(FriendlyByteBuf buf);

	public abstract void writeToNetwork(FriendlyByteBuf buf);

	public boolean canEditedByOperators() {
		return this.canOperatorModify;
	}

	public abstract LiteralArgumentBuilder<CommandSourceStack> buildArgument(LiteralArgumentBuilder<CommandSourceStack> optionNameArgument, CommandBuildContext context, Commands.CommandSelection environment);

	public Command<CommandSourceStack> buildGetter() {
		return args -> {
			args.getSource().sendSuccess(() -> {
				return Component.translatable("blockomorph.commands.option_get.default", this.name, this.value.toString());
			}, true);
			return 1;
		};
	}

	public List<ConfigInstance<?>> getEnterableInstances() {
		return List.of();
	}


	public abstract ConfigRenderer<?> getRenderer();

	@SuppressWarnings({"unchecked", "BooleanMethodIsAlwaysInverted"})
	public boolean trySetValue(ConfigInstance<?> instance) {
		try {
			this.value = (T) instance.value;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected final void fixOldData(JsonElement element, Runnable rewriteStart) {
		for (DataFixer<T> fixer : this.fixers) {
			T value = fixer.fix(this, element, rewriteStart);
			if (value != null) this.value = value;
		}
	}

	public final ConfigInstance<T> boundDataFixer(DataFixer<T> fixer) {
		this.fixers.add(fixer);
		return this;
	}

	public T getValue() {
		return this.value;
	}

	@FunctionalInterface
	public interface DataFixer<VALUE> {
		VALUE fix(ConfigInstance<VALUE> configInstance, JsonElement root, Runnable rewriteStart);
	}
}
