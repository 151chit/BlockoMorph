package net.blockomorph.utils.config.list;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.blockomorph.screens.config.renderers.DamageSourceOptionRenderer;
import net.blockomorph.core.misc.DamageHandler;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class DamageTypeIdsConfig extends RegistryMapIdsConfig<Integer, DamageType> {
	private static final Map.Entry<Set<ResourceLocation>, Boolean> SPECIAL_DAMAGES =
			Map.entry(Set.of(DamageHandler.PLAYER_DESTROYED.location(), DamageHandler.PLAYER_DESTROYED_NULL.location()), true);
	private static final RegistryMapOptionContext CONTEXT = new RegistryMapIdsConfig.RegistryMapOptionContext(true, SPECIAL_DAMAGES, SPECIAL_DAMAGES);
	private static DamageSourceOptionRenderer RENDERER;
	public DamageTypeIdsConfig(String name, Map<ResourceLocation, Integer> initialValue, boolean canOperatorModify, @Nullable Component tip) {
		super(name, initialValue, canOperatorModify, tip, CONTEXT, Registries.DAMAGE_TYPE);
	}

	@Override
	protected @NotNull String valueToString(Integer object) {
		return object.toString();
	}

	@Override
	protected @Nullable Integer stringToValue(String value) {
		try {
			int val = Integer.parseInt(value);
			if (val >= -1) return val;
			return null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	protected ArgumentType<?> getValueArgument(CommandBuildContext context, Commands.CommandSelection environment) {
		return IntegerArgumentType.integer(-1);
	}

	@Override
	protected Integer valueFromArgument(CommandContext<CommandSourceStack> args) {
		return IntegerArgumentType.getInteger(args, "value");
	}

	@Override
	public DamageSourceOptionRenderer getRenderer() {
		if (RENDERER == null) {
			RENDERER = new DamageSourceOptionRenderer();
		}
		return RENDERER;
	}
}
