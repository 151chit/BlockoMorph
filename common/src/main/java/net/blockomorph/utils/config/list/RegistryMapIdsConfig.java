package net.blockomorph.utils.config.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.blockomorph.command.FilteredResourceArgument;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class RegistryMapIdsConfig<VALUE, REG> extends ConfigInstance<Map<Identifier, VALUE>> {
	private static final SimpleCommandExceptionType NOT_VALID = new SimpleCommandExceptionType(Component.translatable("blockomorph.commands.option_change.invalid"));
	protected final ResourceKey<Registry<REG>> registryId;
	protected final RegistryMapOptionContext context;
	protected RegistryMapIdsConfig(String name, Map<Identifier, VALUE> initialValue, boolean canOperatorModify, @Nullable Component tip, RegistryMapOptionContext ctx, ResourceKey<Registry<REG>> registryId) {
		super(name, initialValue, canOperatorModify, tip);
		initialValue.values().forEach(val -> {
			if (val == null) {
				throw new NullPointerException();
			}
		});
		this.registryId = registryId;
		this.context = ctx;
	}

	@NotNull protected abstract String valueToString(VALUE value);
	@Nullable protected abstract VALUE stringToValue(String value);

	@Override
	public void readFromStorage(JsonElement option) {
		this.value.clear();
		option.getAsJsonObject().asMap().forEach((key, jsonElement) -> {
			Identifier raw = Identifier.tryParse(key);
			if (raw != null && jsonElement instanceof JsonPrimitive primitive) {
				VALUE value = this.stringToValue(primitive.getAsString());
				if (value != null) {
					this.value.put(raw, value);
				}
			}
		});
	}

	@Override
	public JsonElement getDataForStorage() {
		JsonObject jsonObject = new JsonObject();
		this.value.forEach((key, value) -> {
			jsonObject.add(key.toString(), new JsonPrimitive(this.valueToString(value)));
		});
		return jsonObject;
	}

	@Override
	public void parseFromUser(ServerPlayer ctx, String value) {
		String[] parts = value.split(" ", 3);
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid input format: " + value);
		}

		String valueStr = parts[2];
		String key = parts[1];
		String action = parts[0];
		Identifier KEY = Identifier.tryParse(key);

		switch (action) {
			case "+":
				VALUE val = this.stringToValue(valueStr);
				if (val == null) throwError(action, key, valueStr);
				var registry = ctx.level().registryAccess().lookupOrThrow(this.registryId);
				if (registry.get(ResourceKey.create(this.registryId, KEY)).isEmpty()) {
					throw new IllegalArgumentException("Resource with ID " + key + " does not exist in registry!");
				}
				if (context.addCensorList == null || ((context.addCensorList.getValue()) != context.addCensorList.getKey().contains(KEY))) {
					this.value.put(KEY, val);
					break;
				} else throwError(action, key, valueStr);
			case "-":
				if (context.removeCensorList == null || ((context.removeCensorList.getValue()) != context.removeCensorList.getKey().contains(KEY))) {
					this.value.remove(KEY);
					break;
				} else throwError(action, key, valueStr);
			case "r":
				if (context.canClear) {
					this.value.clear();
					break;
				} else throwError(action, key, valueStr);
			default:
				throw new IllegalArgumentException("Invalid action: " + action);
		}
	}

	private void throwError(String option, String key, String value) {
		throw new IllegalArgumentException("Invalid action: " + option + " on key: " + key + " for value: " + value);
	}

	@Override
	public void readFromNetwork(FriendlyByteBuf buf) {
		this.value.clear();
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			Identifier key = buf.readIdentifier();
			String value = buf.readUtf();
			VALUE objectValue = this.stringToValue(value);
			if (objectValue != null)
				this.value.put(key, objectValue);
		}
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		buf.writeInt(this.value.size());
		this.value.forEach((key, value) -> {
			String encodedValue = this.valueToString(value);
			buf.writeIdentifier(key);
			buf.writeUtf(encodedValue);
		});
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> buildArgument(LiteralArgumentBuilder<CommandSourceStack> optionNameArgument, CommandBuildContext context, Commands.CommandSelection environment) {
		Map.Entry<Set<Identifier>, Boolean> preparedAddCensorListHolder = Objects.requireNonNullElseGet(this.context.addCensorList, () -> Map.entry(Set.of(), true));
		Map.Entry<Set<Identifier>, Boolean> preparedRemoveCensorListHolder = Objects.requireNonNullElseGet(this.context.removeCensorList, () -> Map.entry(Set.of(), true));
		return optionNameArgument
				.then(Commands.literal("set").then(Commands.argument("key", FilteredResourceArgument.id(context, this.registryId, preparedAddCensorListHolder.getKey(), preparedAddCensorListHolder.getValue())).then(Commands.argument("value", this.getValueArgument(context, environment)).executes(args -> {
					VALUE value = this.valueFromArgument(args);
					if (value == null) throw NOT_VALID.create();
					Identifier name = FilteredResourceArgument.getId(args, "key", this.registryId).identifier();
					Component end = Component.translatable("blockomorph.commands.option_change.map.add", this.valueToString(value), name.toString(), this.name);
					this.value.put(name, value);
					Config.writeAndSend();
					args.getSource().sendSuccess(() -> end, true);
					return 1;
				}))))
				.then(Commands.literal("get").then(Commands.argument("key", ResourceArgument.resource(context, this.registryId)).executes(args -> {
					Identifier name = ResourceArgument.getResource(args, "key", this.registryId).key().identifier();
					VALUE getValue = this.value.get(name);
					if (getValue != null) {
						args.getSource().sendSuccess(() -> Component.translatable("blockomorph.commands.option_change.map.get", name.toString(), this.valueToString(getValue)), true);
						return 1;
					}
					args.getSource().sendFailure(Component.translatable("blockomorph.commands.option_change.map.get.error"));
					return 0;
				})))
				.then(Commands.literal("remove").then(Commands.argument("key", FilteredResourceArgument.id(context, this.registryId, preparedRemoveCensorListHolder.getKey(), preparedRemoveCensorListHolder.getValue())).executes(args -> {
					Identifier name = FilteredResourceArgument.getId(args, "key", this.registryId).identifier();
					Component end = Component.translatable("blockomorph.commands.option_change.map.remove", name.toString(), this.name);
					this.value.remove(name);
					Config.writeAndSend();
					args.getSource().sendSuccess(() -> end, true);
					return 1;
				})))
				.then(Commands.literal("clear").executes(args -> {
					this.value.clear();
					Config.writeAndSend();
					args.getSource().sendSuccess(() -> Component.translatable("blockomorph.commands.option_change.map.clear", this.name), true);
					return 1;
				}));
	}

	protected abstract ArgumentType<?> getValueArgument(CommandBuildContext context, Commands.CommandSelection environment);

	protected abstract VALUE valueFromArgument(CommandContext<CommandSourceStack> args);
	@Override
	public Command<CommandSourceStack> buildGetter() {
		return args -> {
			args.getSource().sendSuccess(() -> {
				StringBuilder builder = new StringBuilder("[");
				this.value.forEach((key, value) -> {
					builder.append("{");
					builder.append(key.toString()).append("=").append(this.valueToString(value));
					builder.append("}");
				});
				builder.append("]");
				return Component.translatable("blockomorph.commands.option_get.map", this.name, this.value.size(), builder.toString());
			}, true);
			return 1;
		};
	}

	public record RegistryMapOptionContext(boolean canClear, @Nullable Map.Entry<Set<Identifier>, Boolean> addCensorList, @Nullable Map.Entry<Set<Identifier>, Boolean> removeCensorList) {
	}
}
