package net.blockomorph.mixins;

import net.blockomorph.utils.accessors.MenuAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(AbstractContainerMenu.class)
public abstract class MenuMixin implements MenuAccessor {
    private Player owner;
    @Inject(method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z", at = @At(value = "HEAD"), cancellable = true)
    private static void stillValid(ContainerLevelAccess p_38890_, Player player, Block p_38892_, CallbackInfoReturnable<Boolean> cir) {
        if (player.containerMenu instanceof MenuAccessor accessor) {
            Player own = accessor.getPlayer();
            if (own != null) {
                if (!own.isAlive()) {
                    cir.setReturnValue(false);
                } else {
                    if (!(player.distanceToSqr(own.getX() + 0.5D, own.getY() + 0.5D, own.getZ() + 0.5D) <= 64.0D)) {
                        cir.setReturnValue(false);
                    } else cir.setReturnValue(true);
                }
            }
        }
    }

    public void boundToPlayer(Player pl) {
        this.owner = pl;
    }

    @Nullable
    public Player getPlayer() {
        return this.owner;
    }
}
