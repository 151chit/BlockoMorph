package net.blockomorph.utils;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.enums.ConfigEnums;
import net.blockomorph.utils.config.ConfigStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record BannedBlock(String reason, Component text) {
	public static final List<BanPredicate> TESTS = new ArrayList<>();
	public static final BannedBlock ALWAYS_ON = new BannedBlock(null, null);

	public static final BannedBlock ALREADY_MORPHED = new BannedBlock(
			"You have already been turned into this block.",
			Component.translatable("blockomorph.bannedBlock.same")
	);

	static {
		TESTS.add(new BanPredicate((state, player, source) -> {
			if (state.isAir() && (source != Source.NETWORK || (player != null && player.getActiveMorphTnt() == null)))
				return ALWAYS_ON;
			return null;
		}, (stateSet, player, source) -> {}));


		TESTS.add(new BanPredicate((state, player, source) -> {
			if (player != null && source == Source.NETWORK) {
				if (player.getActiveMorphTnt() != null) {
					return new BannedBlock("No access to morph when TNT is lit!", Component.translatable("blockomorph.bannedBlock.tnt"));
				}
			}
			return null;
		}, (stateSet, player, source) -> {
			if (player != null && source == Source.NETWORK && player.getActiveMorphTnt() != null) {
				stateSet.clear();
			}
		}));

		TESTS.add(new BanPredicate((state, player, source) -> {
			if (state.getBlock().defaultDestroyTime() < 0) {
				if (Config.get().offUnbreakableBlocks.getValue()) {
					return new BannedBlock("Unbreakable blocks not allowed!", Component.translatable("blockomorph.bannedBlock.unbreakable"));
				}
			}
			return null;
		}, (stateSet, player, source) -> {
			if (Config.get().offUnbreakableBlocks.getValue()) {
				stateSet.removeIf(state -> state.getBlock().defaultDestroyTime() < 0);
			}
		}));

		TESTS.add(new BanPredicate((state, player, source) -> {
			if ((!state.isSolid() || state.getRenderShape() == RenderShape.INVISIBLE) && Config.get().solidBlocksOnly.getValue()) {
				return new BannedBlock("Not solid blocks not allowed!", Component.translatable("blockomorph.bannedBlock.solid"));
			}
			return null;
		}, (stateSet, player, source) -> {
			if (Config.get().solidBlocksOnly.getValue()) {
				stateSet.removeIf(state -> !state.isSolid() || state.getRenderShape() == RenderShape.INVISIBLE);
			}
		}));

		TESTS.add(new BanPredicate((state, player, source) -> {
			ResourceLocation name = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			ConfigStorage cfg = Config.get();
			ConfigEnums.Mode mode = cfg.listMode.getValue();
			switch (mode) {
				case WHITELIST -> {
					if (!cfg.allowedBlocks.getValue().contains(name)) {
						return new BannedBlock("Block " + name + " not allowed because it not in whitelist!", Component.translatable("blockomorph.bannedBlock.whitelist"));
					}
				}
				case BLACKLIST -> {
					if (cfg.bannedBlocks.getValue().contains(name)) {
						return new BannedBlock("Block " + name + " not allowed because it in blacklist!", Component.translatable("blockomorph.bannedBlock.blacklist"));
					}
				}
			}
			return null;
		}, (stateSet, player, source) -> {
			ConfigStorage cfg = Config.get();
			ConfigEnums.Mode mode = cfg.listMode.getValue();
			if (mode == ConfigEnums.Mode.NONE) return;
			Set<ResourceLocation> filterSet = (mode == ConfigEnums.Mode.WHITELIST) ? cfg.allowedBlocks.getValue() : cfg.bannedBlocks.getValue();
			stateSet.removeIf(state -> {
				ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
				return (mode == ConfigEnums.Mode.WHITELIST) != filterSet.contains(id);
			});
		}));
	}


	@Nullable
	public static BannedBlock isBannedBlock(BlockState state, @Nullable PlayerAccessor player, Source source) {
		for (BanPredicate predicate : TESTS) {
			BannedBlock reason = predicate.singleCondition().checkBanned(state, player, source);
			if (reason != null) {
				if (reason == ALWAYS_ON) return null;
				return reason;
			}
		}
		return null;
	}

	public static void filterBlocksWithoutReasons(/* Mutable */ Set<BlockState> states, @Nullable PlayerAccessor player, Source source) {
		TESTS.forEach(banPredicate -> banPredicate.fastStreamCondition().checkBanned(states, player, source));
	}

	public record BanPredicate(SingleCondition singleCondition, FastStreamCondition fastStreamCondition) { }

	@FunctionalInterface
	public interface SingleCondition {
		BannedBlock checkBanned(BlockState state, @Nullable PlayerAccessor player, Source source);
	}

	@FunctionalInterface
	public interface FastStreamCondition {
		void checkBanned(Set<BlockState> state, @Nullable PlayerAccessor player, Source source);
	}

	public enum Source {
		NETWORK,
		COMMAND,
		SYSTEM
	}
}
