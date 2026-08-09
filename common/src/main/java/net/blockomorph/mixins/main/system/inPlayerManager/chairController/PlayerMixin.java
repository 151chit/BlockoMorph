package net.blockomorph.mixins.main.system.inPlayerManager.chairController;

import net.blockomorph.core.misc.chairController.EntityChairController;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin implements EntityChairController.ChairPlayer {
	@Unique private final EntityChairController.ChairHolder holder = new EntityChairController.ChairHolder((Player) (Object)this);

	@Override
	public EntityChairController.ChairHolder getHolder() {
		return this.holder;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void checkLeaks(CallbackInfo ci) {
		this.holder.tick();
	}
}
