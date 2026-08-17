package net.blockomorph.mixins.main.system.virtualization.normalize.level.color;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.side.ThreadLocalMutableBlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Unique private final ThreadLocalMutableBlockPos blockPos = new ThreadLocalMutableBlockPos();

	@ModifyVariable(method = "calculateBlockTint", at = @At("HEAD"))
	private BlockPos norm(BlockPos orig) {
		return MorphNormalizer.normalize(orig, this.blockPos);
	}
}
