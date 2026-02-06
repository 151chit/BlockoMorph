package net.blockomorph.screens.utils;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.jetbrains.annotations.Nullable;

public class ModLiquidFogEnvironment extends FogEnvironment {
	private final FogLiquidModifier MODIFIER = new FogLiquidModifier();
	private FogLiquidModifier.LiquidFogData cachedFog;

	@Override
	public void setupFog(FogData fogData, Entity entity, BlockPos blockPos, ClientLevel clientLevel, float f, DeltaTracker deltaTracker) {
		if (this.cachedFog != null) {
			fogData.environmentalStart = cachedFog.start();
			fogData.environmentalEnd = cachedFog.end();
			fogData.skyEnd = cachedFog.end();
			fogData.cloudEnd = cachedFog.end();
		}
		this.cachedFog = null;
	}

	@Override
	public int getBaseColor(ClientLevel clientLevel, Camera camera, int i, float f) {
		if (this.cachedFog != null) {
			return this.cachedFog.color();
		}
		return -1;
	}

	@Override
	public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
		if (this.cachedFog != null) return true;
		if (entity.level() instanceof ClientLevel lv) {
			this.cachedFog = MODIFIER.getFog(lv.entitiesForRendering(), false);
		}
		return this.cachedFog != null;
	}

	@Override
	public void onNotApplicable() {
		this.cachedFog = null;
	}
}
