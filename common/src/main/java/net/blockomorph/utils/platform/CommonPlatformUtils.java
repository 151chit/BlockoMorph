package net.blockomorph.utils.platform;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.tnt.TntSpawnLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ServiceLoader;

public interface CommonPlatformUtils {
	CommonPlatformUtils INSTANCE = ServiceLoader.load(CommonPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load common platform-depended utils, mod cannot run!"));
	void litTnt(TntBlock blockVanilla, TntSpawnLevel lv, BlockInPlayer2 block, PlayerAccessor pl);
	TntSpawnLevel createLevel(Level orig, BlockState need);
	void loadNbtToBlockEntityOnClient(BlockEntity blockEntity, ClientBoundMorphUpdatePacket pkt, RegistryAccess registries, CompoundTag tag);
	void sendServer(BlockMorphPacket packet);
	void sendPlayer(BlockMorphPacket packet, ServerPlayer player);
	void sendAll(BlockMorphPacket packet);

	static void onRightClick(Player serverPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (serverPlayer.getItemInHand(interactionHand).getItem() instanceof BlockItem) {
			ConfigEnums.PlaceMode mode = Config.get().placeMode.getValue();
			if (mode == ConfigEnums.PlaceMode.DISABLED && InPlayerBlockPos.isMorphedPlayerX(blockHitResult.getBlockPos().getX())) {
				cir.setReturnValue(InteractionResult.PASS);
			}
		}
	}
}
