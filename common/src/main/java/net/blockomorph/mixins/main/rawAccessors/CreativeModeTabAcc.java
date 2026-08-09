package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabAcc implements Accessors.CategoryTabAccessor {
	@Shadow @Final private CreativeModeTab.DisplayItemsGenerator displayItemsGenerator;
	@Override
	public CreativeModeTab.DisplayItemsGenerator getItemsFormer$bm() {
		return this.displayItemsGenerator;
	}
}
