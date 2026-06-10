package net.blockomorph.mixins.main.client.graphic;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAt;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(DebugEntryLookingAt.DebugEntryLookingAtState.class)
public abstract class BlockInfoMixin extends DebugEntryLookingAt {
	@Unique
	private static final Identifier FLUID_GROUP = GuiUtils.vanillaRes("looking_at_fluid");

	@WrapOperation(method = "extractInfo", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
	private boolean add(List<String> instance, Object e, Operation<Boolean> original) {
		Entity camera = GuiUtils.MC.getCameraEntity();
		if (camera != null) {
			String out = GuiUtils.redirectBlockInfo(this.group().equals(FLUID_GROUP), this.getHitResult(camera));
			if (out != null) {
				return instance.add(ChatFormatting.UNDERLINE + out);
			}
		}
		return original.call(instance, e);
	}
}
