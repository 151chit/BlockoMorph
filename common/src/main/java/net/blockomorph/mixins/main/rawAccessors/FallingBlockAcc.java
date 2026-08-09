package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FallingBlock.class)
public abstract class FallingBlockAcc implements Accessors.FallingBlockAccessor {
	@Shadow protected abstract void falling(FallingBlockEntity entity);
	@Override
	public void prepareEntity$bm(FallingBlockEntity block) {
		this.falling(block);
	}
}
