package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.accessors.ForceLevelChanger;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements ForceLevelChanger {
	@Shadow @Nullable protected Level level;

	@Override
	public void forceLevelChange(Level lv) {
		this.level = lv;
	}
}
