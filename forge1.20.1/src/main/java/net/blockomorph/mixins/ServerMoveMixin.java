package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMoveMixin {
    
    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
    public void checkCollision(LevelReader levelReader, AABB playerBox, double moveX, double moveY, double moveZ, CallbackInfoReturnable<Boolean> cir) {
    	ServerPlayer player = ((ServerGamePacketListenerImpl) (Object)this).player;
    	
        AABB movedBox = player.getBoundingBox().move(moveX - player.getX(), moveY - player.getY(), moveZ - player.getZ());
        Iterable<VoxelShape> collisions = levelReader.getCollisions(player, movedBox.deflate((double)1.0E-5F));
        VoxelShape playerShape = Shapes.create(playerBox.deflate((double)1.0E-5F));

        boolean isAlreadyInsideShape = false;
        boolean isTryingToEnterShape = false;

        for (VoxelShape collisionShape : collisions) {
            if (Shapes.joinIsNotEmpty(Shapes.create(playerBox.deflate(1.0E-5F)), collisionShape, BooleanOp.AND)) {
                isAlreadyInsideShape = true;
            }

            if (Shapes.joinIsNotEmpty(Shapes.create(movedBox.deflate(1.0E-5F)), collisionShape, BooleanOp.AND)) {
                isTryingToEnterShape = true;
            }
        }

        if (isTryingToEnterShape && !isAlreadyInsideShape) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }
}
