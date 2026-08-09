package net.blockomorph.mixins.main.system.virtualization.normalize.level;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static net.blockomorph.core.coords.math.MorphNormalizer.normalizeOneOf;

@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {
	@Unique private final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

	@ModifyVariable(method = "post", at = @At("STORE"))
	private BlockPos norm(BlockPos center) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(center.getX())) {
			return this.mutableBlockPos.set(
					Mth.floor(normalizeOneOf(center, Direction.Axis.X)),
					Mth.floor(normalizeOneOf(center, Direction.Axis.Y)),
					Mth.floor(normalizeOneOf(center, Direction.Axis.Z))
			);
		}
		return center;
	}
}
