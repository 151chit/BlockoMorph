package net.blockomorph.mixins.compat.betterF3;

import net.blockomorph.utils.accessors.compat.DebugLineAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "me.cominixo.betterf3.utils.DebugLine", remap = false)
public abstract class DebugLineMixin implements DebugLineAccessor {
	@Shadow public abstract void value(Object value);

	@Shadow @Final private String id;

	@Override
	public String id$blockomroph() {
		return this.id;
	}

	@Override
	public void setValue$blockomorph(Object value) {
		this.value(value);
	}
}
