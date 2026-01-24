package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.accessors.CategoryTab;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin implements CategoryTab {
	@Shadow @Final private CreativeModeTab.DisplayItemsGenerator displayItemsGenerator;

	@Override
	public CreativeModeTab.DisplayItemsGenerator getItemsFormer() {
		return this.displayItemsGenerator;
	}
}
