package net.blockomorph.mixins.main.blockFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantingTableBlockEntity.class)
public class EnchantmentTableMixin {

    @Inject(method = "bookAnimationTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;tRot:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, ordinal = 0))
    private static void getRealBlockPos(Level lv, BlockPos pos, BlockState p_155506_, EnchantingTableBlockEntity table, CallbackInfo ci, @Local Player player) {
        InPlayerBlockPos.check(pos, (pl, realPos) -> {
            Vec3 vec = MorphUtils.getRealBlockPos(pl, realPos);
            Vec3 vec3 = player.position();
            double x = vec3.x - (vec.x + 0.5D);
            double z = vec3.z - (vec.z + 0.5D);
            table.tRot = (float) Mth.atan2(z, x);
        }, null, lv);
    }

}
