package net.blockomorph.mixins.compat.betterF3;

import net.blockomorph.utils.accessors.compat.BaseModuleAccessor;
import net.blockomorph.utils.accessors.compat.DebugLineAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(targets = "me.cominixo.betterf3.modules.BaseModule")
public class BaseModuleMixin implements BaseModuleAccessor {

	@Shadow @Final protected List<DebugLineAccessor> lines;

	@Override
	public List<DebugLineAccessor> getModules() {
		return this.lines;
	}
}
