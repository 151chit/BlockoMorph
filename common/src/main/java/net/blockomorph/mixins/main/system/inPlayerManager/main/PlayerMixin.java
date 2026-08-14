package net.blockomorph.mixins.main.system.inPlayerManager.main;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.core.PlayerAccessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin implements PlayerAccessor {
	@Unique private boolean initied;
	@Unique private InPlayerManager mainManager;

	@Override
	public InPlayerManager getManager() {
		this.checkInit();
		return this.mainManager;
	}

	@Override
	public void changeManager(InPlayerManager manager) {
		this.mainManager = manager;
		if (this.mainManager != null) {
			this.initied = true;
			this.mainManager.onOwnerChanged(this);
		} else this.initied = false;
	}

	@Unique
	private void checkInit() {
		if (!this.initied) {
			long section = BlockPosBounds.getSectionByPlayer(this.player());
			if (section != -1) {
				this.initied = true;
				this.mainManager = InPlayerManager.createValidFor(this);
			}
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void tick(CallbackInfo ci) {
		if (this.mainManager != null) {
			this.mainManager.baseTick();
		} else this.checkInit();
	}
}
