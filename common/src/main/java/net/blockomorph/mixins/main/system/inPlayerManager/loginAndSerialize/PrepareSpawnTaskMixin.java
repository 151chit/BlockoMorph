package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.core.serialization.dataFixer.DataFixerHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerList.class)
public class PrepareSpawnTaskMixin {
	@Shadow @Final private MinecraftServer server;

	@Inject(require = 1, method = "placeNewPlayer", at= @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;problemPath()Lnet/minecraft/util/ProblemReporter$PathElement;"))
	private void boundBlockPos(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
		BlockPosBounds.registerPlayer(serverPlayer);
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@ModifyVariable(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", ordinal = 0))
	private Optional<ValueInput> loadData(Optional<ValueInput> input, @Local(argsOnly = true) ServerPlayer player) {
		if (input.isPresent()) DataFixerHandler.checkAndFix(this.server, player.getUUID(), this.server.registryAccess(), name ->
			input.orElseThrow().read(name, CompoundTag.CODEC).orElse(null)
		);
		PlayerWorldSerializer.loadNewPlayer(player);
		return input;
	}
}
