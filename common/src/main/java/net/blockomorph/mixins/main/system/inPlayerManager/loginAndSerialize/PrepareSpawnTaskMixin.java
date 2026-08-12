package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.core.serialization.dataFixer.DataFixerHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Ready")
public class PrepareSpawnTaskMixin {
	@Shadow @Final private ServerLevel spawnLevel;

	@Inject(require = 1, method = "spawn", at= @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;problemPath()Lnet/minecraft/util/ProblemReporter$PathElement;"))
	private void boundBlockPos(Connection connection, CommonListenerCookie cookie, CallbackInfoReturnable<ServerPlayer> cir, @Local ServerPlayer player) {
		BlockPosBounds.registerPlayer(player);
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@ModifyVariable(method = "spawn", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", ordinal = 0))
	private Optional<ValueInput> loadData(Optional<ValueInput> input, @Local ServerPlayer player) {
		if (input.isPresent()) DataFixerHandler.checkAndFix(this.spawnLevel.getServer(), player.getUUID(), this.spawnLevel.registryAccess(), name ->
			input.orElseThrow().read(name, CompoundTag.CODEC).orElse(null)
		);
		PlayerWorldSerializer.loadNewPlayer(player);
		return input;
	}
}
