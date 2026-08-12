package net.blockomorph.mixins.main.system.virtualization;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockPos.class)
public class BlockPosEncoderMixin {
	@Shadow @Final private static int Y_OFFSET;
	@Shadow @Final private static int X_OFFSET;
	@Shadow @Final private static int Z_OFFSET;
	@Shadow @Final private static long PACKED_X_MASK;
	@Shadow @Final private static long PACKED_Y_MASK;
	@Shadow @Final private static long PACKED_Z_MASK;

	@Shadow public static int getY(long blockNode) { throw new UnsupportedOperationException("Implemented via mixin"); }
	@Shadow public static int getX(long blockNode) { throw new UnsupportedOperationException("Implemented via mixin"); }
	@Shadow public static int getZ(long blockNode) { throw new UnsupportedOperationException("Implemented via mixin"); }

	@FastInject(method = "of", at = @At("HEAD"))
	private static Object decode(long blockNode) {
		int y = getY(blockNode);
		if (y >= -2048 && y <= -2018) {
			return new BlockPos(
					getX(blockNode) + InPlayerBlockPos.X_CENTER,
					y + 2048 + InPlayerBlockPos.Y_CHUNK_START,
					getZ(blockNode) + InPlayerBlockPos.X_CHUNK_START / 2
			);
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	@ModifyReturnValue(method = "asLong(III)J", at = @At(value = "RETURN"))
	private static long boundedEncode(long original, int x, int y, int z) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
			return encodeVanilla(x - InPlayerBlockPos.X_CENTER, y - InPlayerBlockPos.Y_CHUNK_START - 2048, z - InPlayerBlockPos.X_CHUNK_START / 2);
		}
		return original;
	}

	@Unique
	private static long encodeVanilla(int x, int y, int z) {
		long i = 0L;
		i |= ((long) x & PACKED_X_MASK) << X_OFFSET;
		i |= ((long) y & PACKED_Y_MASK) << Y_OFFSET;
		return i | ((long) z & PACKED_Z_MASK) << Z_OFFSET;
	}
}
