package net.blockomorph.utils.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.screens.config.renderers.BlockListConfigRenderer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

public class BlockIdsSetConfig extends ConfigInstance<Set<ResourceLocation>> {
	private static BlockListConfigRenderer RENDERER;
	private final ListOptionContext context;

	public BlockIdsSetConfig(String name, Set<ResourceLocation> initialValue, boolean canOperatorModify, @Nullable Component tip, ListOptionContext ctx) {
		super(name, initialValue, canOperatorModify, tip);
		this.context = ctx;
	}

	@Override
	public void readFromStorage(JsonElement option) {
		for (JsonElement el : option.getAsJsonArray()) {
			ResourceLocation raw = ResourceLocation.tryParse(el.getAsString());
			if (raw != null)
				this.value.add(raw);
		}
	}

	@Override
	public JsonElement getDataForStorage() {
		JsonArray jsonArray = new JsonArray();
		for (ResourceLocation item : this.value) {
			jsonArray.add(item.toString());
		}
		return jsonArray;
	}

	@Override
	public void readFromNetwork(FriendlyByteBuf buf) {
		this.value.clear();
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			ResourceLocation element = buf.readResourceLocation();
			this.value.add(element);
		}
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		buf.writeInt(this.value.size());
		for (ResourceLocation element : this.value) {
			buf.writeResourceLocation(element);
		}
	}

	public ResourceLocation getFrameTexture() {
		return this.context.frame;
	}

	@Override
	public void parseFromUser(String value) {
		String[] parts = value.split(" ", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid input format: " + value);
		}

		String element = parts[1];
		String action = parts[0];
		ResourceLocation blockId = ResourceLocation.parse(element);

		switch (action) {
			case "+":
				if (context.allowedAddValue != null && context.allowedAddValue.test(blockId)) {
					this.value.add(blockId);
					break;
				} else throwError(action, element);
			case "-":
				if (context.allowedRemoveValue != null && context.allowedRemoveValue.test(blockId)) {
					this.value.remove(blockId);
					break;
				} else throwError(action, element);
			case "r":
				if (context.canClear) {
					this.value.clear();
					break;
				} else throwError(action, element);
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
		return actionString.then(Commands.argument("value", ResourceArgument.resource(ctx, Registries.BLOCK)).executes(args -> {
			ResourceLocation name = ResourceArgument.getResource(args, "value", Registries.BLOCK).key().location();
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

	@Override
	public BlockListConfigRenderer getRenderer() {
		if (RENDERER == null) {
			RENDERER = new BlockListConfigRenderer();
		}
		return RENDERER;
	}

	public record ListOptionContext(ResourceLocation frame, @Nullable Predicate<ResourceLocation> allowedAddValue,
									@Nullable Predicate<ResourceLocation> allowedRemoveValue, boolean canClear) {
	}
}
