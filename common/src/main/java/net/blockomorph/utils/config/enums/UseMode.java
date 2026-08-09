package net.blockomorph.utils.config.enums;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiPredicate;

public enum UseMode {
	DISABLED((ignore1, ignore2) -> true),

	VANILLA((lv, hit) -> {
		Identifier res = BuiltInRegistries.BLOCK.getKey(lv.getBlockState(hit.getBlockPos()).getBlock());
		return !res.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
	}),

	ALL((ignore1, ignore2) -> false);

	final BiPredicate<Level, BlockHitResult> condition;
	UseMode(BiPredicate<Level, BlockHitResult> condition) {
		this.condition = condition;
	}

	public boolean needRejectUseFor(Level lv, BlockHitResult hit) {
		return this.condition.test(lv, hit);
	}

	public static boolean needRejectUse(Level lv, BlockHitResult hit) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(hit.getBlockPos().getX())) return false;
		return Config.get().useMode.getValue().needRejectUseFor(lv, hit);
	}
}
