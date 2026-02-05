package net.blockomorph.utils.config;

import net.blockomorph.screens.morphConfig.nbtEditor.NbtPath;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;

public class ConfigStorage {
	public final EnumConfig<ConfigEnums.Mode> listMode = new EnumConfig<>("listMode", ConfigEnums.Mode.NONE, true, null);
	public final BlockIdsSetConfig allowedBlocks = new BlockIdsSetConfig("allowedBlocks", new HashSet<>(), true, null, new BlockIdsSetConfig.ListOptionContext(MorphUtils.res("textures/screens/sel_good.png"), BuiltInRegistries.BLOCK::containsKey, BuiltInRegistries.BLOCK::containsKey, true));
	public final BlockIdsSetConfig bannedBlocks = new BlockIdsSetConfig("bannedBlocks", new HashSet<>(), true, null, new BlockIdsSetConfig.ListOptionContext(MorphUtils.res("textures/screens/sel_bad.png"), BuiltInRegistries.BLOCK::containsKey, BuiltInRegistries.BLOCK::containsKey, true));
	public final BooleanConfig solidBlocksOnly = new BooleanConfig("solidBlocksOnly", false, true, null);
	public final BooleanConfig offUnbreakableBlocks = new BooleanConfig("offUnbreakableBlocks", false, true, null);
	public final EnumConfig<ConfigEnums.ScreenAccess> screenAccess = new EnumConfig<>("screenAccess", ConfigEnums.ScreenAccess.ALL, true, null);
	public final CategoryOption access = (CategoryOption) new CategoryOption("access", List.of(listMode, allowedBlocks, bannedBlocks, solidBlocksOnly, offUnbreakableBlocks, screenAccess), null, false)
			.boundDataFixer(new CategoryOption.MovingFixer(new NbtPath.RootNbtPath()));

	public final EnumConfig<ConfigEnums.UseMode> useMode = new EnumConfig<>("useMode", ConfigEnums.UseMode.ALL, true, null);
	public final EnumConfig<ConfigEnums.PlaceMode> placeMode = new EnumConfig<>("placeMode", ConfigEnums.PlaceMode.OUT, true, null);
	public final EnumConfig<ConfigEnums.HitReaction> hitReaction = new EnumConfig<>("hitReaction", ConfigEnums.HitReaction.BRAKING, true, null);
	public final BooleanConfig entityInside = new BooleanConfig("entityInside", true, true, Component.translatable("blockomorph.config_option.entityInside.tooltip"));
	public final BooleanConfig liquidsInBlocks = new BooleanConfig("liquidsInBlocks", false, true, Component.translatable("blockomorph.config_option.liquidsInBlocks.tooltip"));
	public final BooleanConfig blockClientParticles = new BooleanConfig("blockClientParticles", true, true, null);
	public final BooleanConfig gameEvents = new BooleanConfig("gameEvents", true, true, null);
	public final CategoryOption blockBehaviour = (CategoryOption) new CategoryOption("blockBehaviour", List.of(useMode, placeMode, hitReaction, entityInside, liquidsInBlocks, blockClientParticles, gameEvents), null, false)
			.boundDataFixer(new CategoryOption.MovingFixer(new NbtPath.RootNbtPath()));

	public final BooleanConfig playerDieAfterDestroy = new BooleanConfig("playerDieAfterDestroy", true, true, null);
	public final BooleanConfig canOperatorModifyConfig = new BooleanConfig("canOperatorModifyConfig", true, false, null);


	public final List<ConfigInstance<?>> RECURSIVE_OPTIONS;//for GUI
	public final List<ConfigInstance<?>> LINEAR_OPTIONS;//for COMMAND

	public ConfigStorage() {
		List<ConfigInstance<?>> allFields = new ArrayList<>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			try {
				Object value = field.get(this);
				if (value instanceof ConfigInstance<?> instance) {
					allFields.add(instance);
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		Set<ConfigInstance<?>> allChildren = new HashSet<>();
		for (ConfigInstance<?> opt : allFields) {
			allChildren.addAll(opt.getEnterableInstances());
		}
		List<ConfigInstance<?>> recursive = new ArrayList<>();
		for (ConfigInstance<?> opt : allFields) {
			if (!allChildren.contains(opt)) {
				recursive.add(opt);
			}
		}
		List<ConfigInstance<?>> linear = new ArrayList<>();
		for (ConfigInstance<?> opt : allFields) {
			if (opt.getEnterableInstances().isEmpty()) {
				linear.add(opt);
			}
		}
		this.LINEAR_OPTIONS = Collections.unmodifiableList(linear);
		this.RECURSIVE_OPTIONS = Collections.unmodifiableList(recursive);
	}
}