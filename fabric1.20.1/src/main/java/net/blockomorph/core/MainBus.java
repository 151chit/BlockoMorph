package net.blockomorph.core;

import net.blockomorph.command.BlockmorphCommand;
import net.blockomorph.core.KeyMappings;
import net.blockomorph.screens.PlayerCrackOverlay;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.*;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class MainBus {
   public static final ResourceLocation MORPH_PACKET = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_block_morph_packet");
   public static final ResourceLocation DESTROY_PACKET = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_stop_destroy_packet");
   
   public static void registerClient() {
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			PlayerCrackOverlay.render(matrices, tickDelta);
		});
		KeyBindingHelper.registerKeyBinding(KeyMappings.MORPH);
        KeyBindingHelper.registerKeyBinding(KeyMappings.MORPH_CONFIG);
        ClientTickEvents.END_CLIENT_TICK.register((mc) -> {
			MorphUtils.onClientTick();
		});
		WorldRenderEvents.BEFORE_ENTITIES.register((context) -> {
			MorphUtils.onPick();
		});
   }

   public static void registerServer() {
   	    ServerPlayNetworking.registerGlobalReceiver(MORPH_PACKET, ServerBoundBlockMorphPacket::apply);
   	    ServerPlayNetworking.registerGlobalReceiver(DESTROY_PACKET, ServerBoundStopDestroyPacket::apply);
      	CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, environment) -> {
			BlockmorphCommand.register(dispatcher, commandBuildContext, environment);
		});
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			MorphUtils.onPlayerClone(oldPlayer, newPlayer, alive);
		});
   }
}
