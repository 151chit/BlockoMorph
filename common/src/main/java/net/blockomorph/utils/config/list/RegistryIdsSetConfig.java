package net.blockomorph.utils.config.list;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.command.FilteredResourceArgument;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class RegistryIdsSetConfig<T> extends ConfigInstance<Set<Identifier>> {
	protected final IdsSetOptionContext context;
	protected final ResourceKey<Registry<T>> registryId;
	protected RegistryIdsSetConfig(String name, Set<Identifier> initialValue, boolean canOperatorModify, @Nullable Component tip, IdsSetOptionContext context, ResourceKey<Registry<T>> registryId) {
		super(name, initialValue, canOperatorModify, tip);
		this.context = context;
		this.registryId = registryId;
	}

	@Override
	public void readFromStorage(JsonElement option) {
		this.value.clear();
		for (JsonElement el : option.getAsJsonArray()) {
			Identifier raw = Identifier.tryParse(el.getAsString());
			if (raw != null)
				this.value.add(raw);
		}
	}

	@Override
	public JsonElement getDataForStorage() {
		JsonArray jsonArray = new JsonArray();
		for (Identifier item : this.value) {
			jsonArray.add(item.toString());
		}
		return jsonArray;
	}

	@Override
	public void readFromNetwork(FriendlyByteBuf buf) {
		this.value.clear();
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			Identifier element = buf.readIdentifier();
			this.value.add(element);
		}
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		buf.writeInt(this.value.size());
		for (Identifier element : this.value) {
			buf.writeIdentifier(element);
		}
	}

	@Override
	public void parseFromUser(ServerPlayer ctx, String value) {
		String[] parts = value.split(" ", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid input format: " + value);
		}

		String id = parts[1];
		String action = parts[0];
		Identifier ID = Identifier.tryParse(id);

		switch (action) {
			case "+":
				var registry = ctx.level().registryAccess().lookupOrThrow(this.registryId);
				if (registry.get(ResourceKey.create(this.registryId, ID)).isEmpty()) {
					throw new IllegalArgumentException("Resource with ID " + id + " does not exist in registry!");
				}
				if (context.addCensorList == null || ((context.addCensorList.getValue()) != context.addCensorList.getKey().contains(ID))) {
					this.value.add(ID);
					break;
				} else throwError(action, id);
			case "-":
				if (context.removeCensorList == null || ((context.removeCensorList.getValue()) != context.removeCensorList.getKey().contains(ID))) {
					this.value.remove(ID);
					break;
				} else throwError(action, id);
			case "r":
				if (context.canClear) {
					this.value.clear();
					break;
				} else throwError(action, id);
			default:
				throw new IllegalArgumentException("Invalid action: " + action);
		}
	}

	private void throwError(String option, String value) {
		throw new IllegalArgumentException("Invalid action: " + option + " for value: " + value);
	}

	public LiteralArgumentBuilder<CommandSourceStack> buildArgument(LiteralArgumentBuilder<CommandSourceStack> optionNameArgument, CommandBuildContext context, Commands.CommandSelection environment) {
		return optionNameArgument
				.then(this.end(Commands.literal("add"), false, context))
				.then(this.end(Commands.literal("remove"), true, context))
				.then(Commands.literal("clear").executes(args -> {
							this.value.clear();
							Config.writeAndSend();
							args.getSource().sendSuccess(() -> Component.translatable("blockomorph.commands.option_change.list.clear", this.name), true);
							return 1;
						})
				);
	}

	private LiteralArgumentBuilder<CommandSourceStack> end(LiteralArgumentBuilder<CommandSourceStack> actionString, boolean remove, CommandBuildContext ctx) {
		Map.Entry<Set<Identifier>, Boolean> censorListHolder = remove ? this.context.removeCensorList : this.context.addCensorList;
		Map.Entry<Set<Identifier>, Boolean> preparedCensorListHolder = Objects.requireNonNullElseGet(censorListHolder, () -> Map.entry(Set.of(), true));
		return actionString.then(Commands.argument("value", FilteredResourceArgument.id(ctx, this.registryId, preparedCensorListHolder.getKey(), preparedCensorListHolder.getValue())).executes(args -> {
			Identifier name = FilteredResourceArgument.getId(args, "value", this.registryId).identifier();
			Component end;
			if (remove) {
				end = Component.translatable("blockomorph.commands.option_change.list.remove", name.toString(), this.name);
				this.value.remove(name);
			} else {
				this.value.add(name);
				end = Component.translatable("blockomorph.commands.option_change.list.add", name.toString(), this.name);
			}
			Config.writeAndSend();
			args.getSource().sendSuccess(() -> end, true);
			return 1;
		}));
	}

	@Override
	public Command<CommandSourceStack> buildGetter() {
		return args -> {
			args.getSource().sendSuccess(() -> {
				return Component.translatable("blockomorph.commands.option_get.list", this.name, this.value.size(), this.value.toString());
			}, true);
			return 1;
		};
	}

	public record IdsSetOptionContext(boolean canClear, @Nullable Map.Entry<Set<Identifier>, Boolean> addCensorList, @Nullable Map.Entry<Set<Identifier>, Boolean> removeCensorList) {
		public static final IdsSetOptionContext ALWAYS_TRUE = new IdsSetOptionContext(true, null, null);
	}
}