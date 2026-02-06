package net.blockomorph.mixins.main.coords;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPos.class)
public class BlockPosEncoderMixin {
	@Shadow @Final private static int Y_OFFSET;
	@Shadow @Final private static int X_OFFSET;
	@Shadow @Final private static int Z_OFFSET;
	@Shadow @Final private static long PACKED_X_MASK;
	@Shadow @Final private static long PACKED_Y_MASK;
	@Shadow @Final private static long PACKED_Z_MASK;

	@Inject(method = "of", at = @At(value = "RETURN"), cancellable = true)
	private static void decode(long packedPos, CallbackInfoReturnable<BlockPos> cir) {
		BlockPos pos = cir.getReturnValue();
		if (pos.getY() >= -2048 && pos.getY() <= -2018) {
			cir.setReturnValue(new BlockPos(
					pos.getX() + InPlayerBlockPos.X_CENTER,
					pos.getY() + 2048 + InPlayerBlockPos.Y_CHUNK_START,
					pos.getZ() + InPlayerBlockPos.X_CHUNK_START / 2)
			);
		}
	}

	@Inject(method = "asLong(III)J", at = @At(value = "HEAD"), cancellable = true)
	private static void boundedEncode(int x, int y, int z, CallbackInfoReturnable<Long> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(x)) {
			cir.setReturnValue(encode(x - InPlayerBlockPos.X_CENTER, y - InPlayerBlockPos.Y_CHUNK_START - 2048, z - InPlayerBlockPos.X_CHUNK_START / 2));
		}
	}

	private static long encode(int x, int y, int z) {
		long i = 0L;
		i |= ((long) x & PACKED_X_MASK) << X_OFFSET;
		i |= ((long) y & PACKED_Y_MASK) << Y_OFFSET;
		return i | ((long) z & PACKED_Z_MASK) << Z_OFFSET;
	}
}
