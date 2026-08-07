package net.blockomorph.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blockomorph.screens.config.renderers.CategoryConfigRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.NbtPath;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryOption extends ConfigInstance<List<ConfigInstance<?>>> {
	private static CategoryConfigRenderer RENDERER;

	public CategoryOption(String name, List<ConfigInstance<?>> options, @Nullable Component tip, boolean hidedCategory) {
		super(name, Collections.unmodifiableList(options), !hidedCategory, tip);
		if (options.isEmpty()) {
			throw new IllegalArgumentException("Option list must not be empty!");
		}
	}

	@Override
	public List<ConfigInstance<?>> getEnterableInstances() {
		return this.value;
	}

	@Override
	public void readFromStorage(JsonElement option) {
		JsonObject object = option.getAsJsonObject();
		for (ConfigInstance<?> instance : this.value) {
			JsonElement element = object.get(instance.getName());
			if (element != null)
				instance.readFromStorage(element);
		}
	}

	@Override
	public JsonElement getDataForStorage() {
		JsonObject object = new JsonObject();
		for (ConfigInstance<?> instance : this.value) {
			object.add(instance.getName(), instance.getDataForStorage());
		}
		return object;
	}

	@Override
	public void parseFromUser(ServerPlayer ctx, String value) {
		throw new UnsupportedOperationException("You can't change the value ofObj a category property directly, you need to specify the specific property name in the category.");
	}

	@Override
	public void readFromNetwork(FriendlyByteBuf buf) {
		for (ConfigInstance<?> instance : this.value) {
			instance.readFromNetwork(buf);
		}
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buf) {
		for (ConfigInstance<?> instance : this.value) {
			instance.writeToNetwork(buf);
		}
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> buildArgument(LiteralArgumentBuilder<CommandSourceStack> optionNameArgument, CommandBuildContext context, Commands.CommandSelection environment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Command<CommandSourceStack> buildGetter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CategoryConfigRenderer getRenderer() {
		if (RENDERER == null) {
			RENDERER = new CategoryConfigRenderer();
		}
		return RENDERER;
	}

	@Override
	public boolean trySetValue(ConfigInstance<?> instance) {
		boolean result = true;
		if (instance instanceof CategoryOption option) {
			for (ConfigInstance<?> configInstance : option.value) {
				ConfigInstance<?> finded = this.findOptionByName(configInstance.getName());
				if (finded != null) {
					if (!finded.trySetValue(configInstance)) {
						result = false;
					}
				} else result = false;
			}
		} else return false;
		return result;
	}

	@Nullable
	private ConfigInstance<?> findOptionByName(String name) {
		for (ConfigInstance<?> configInstance : this.value) {
			if (configInstance.getName().equals(name)) {
				return configInstance;
			}
		}
		return null;
	}

	public record MovingFixer(NbtPath source) implements DataFixer<List<ConfigInstance<?>>> {

		@Override
		public List<ConfigInstance<?>> fix(ConfigInstance<List<ConfigInstance<?>>> configInstance, JsonElement root, Runnable rewriteStart) {
			JsonElement element = root;
			for (String folder : this.source.constructPath(new ArrayList<>())) {
				if (!element.isJsonObject()) return null;
				element = element.getAsJsonObject().get(folder);
				if (element == null) return null;
			}
			if (element instanceof JsonObject object) {
				for (ConfigInstance<?> instance : configInstance.getValue()) {
					try {
						JsonElement property = object.get(instance.getName());
						if (property != null) instance.readFromStorage(property);
					} catch (Exception ignored) {
					}
				}
				rewriteStart.run();
			}
			return null;
		}
	}
}
