package net.blockomorph.mixins;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LevelChunk.class)
public abstract class ChunkMixin {

   @Final
   @Shadow Level level;
   
   @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
   public void getBlockState(BlockPos blockpos, CallbackInfoReturnable<BlockState> cir) {
   	    List<Player> pl = level.getEntitiesOfClass(Player.class, new AABB(0, 0, 0, 1, 1, 1).move(blockpos), e -> true);
   	    if (!pl.isEmpty() && !level.isClientSide()) {
   	    	Player p = pl.get(0);
   	    	cir.setReturnValue(((PlayerAccessor)p).getBlockState());
   	    }
   }

   @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
   public void setBlockState(BlockPos blockpos, BlockState blockstate, boolean p_62867_, CallbackInfoReturnable<BlockState> cir) {
   	    List<Player> pl = level.getEntitiesOfClass(Player.class, new AABB(0, 0, 0, 1, 1, 1).move(blockpos), e -> true);
   	    if (!pl.isEmpty() && !level.isClientSide()) {
   	    	Player p = pl.get(0);
   	    	PlayerAccessor pla = (PlayerAccessor)p;
   	    	pla.applyBlockMorph(blockstate, pla.getTag());
   	    	cir.setReturnValue(pla.getBlockState());
   	    }
   }
}
