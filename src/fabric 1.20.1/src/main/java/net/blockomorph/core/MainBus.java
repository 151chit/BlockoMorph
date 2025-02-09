package net.blockomorph.core;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.blockomorph.utils.*;
import net.blockomorph.utils.config.*;
import net.blockomorph.screens.PlayerCrackOverlay;
import net.blockomorph.network.*;
import net.blockomorph.command.*;
import net.blockomorph.BlockomorphMod;

public class MainBus {
	public static final ResourceLocation MORPH_PACKET = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_block_morph_packet");
	public static final ResourceLocation INTERACT_PACKET = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_interact_block_packet");
	public static final ResourceLocation CLIENT_CONFIG = new ResourceLocation(BlockomorphMod.MOD_ID, "client_bound_config_update_packet");
	public static final ResourceLocation SERVER_CONFIG = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_config_update_packet");

	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(CLIENT_CONFIG, ClientBoundConfigUpdatePacket::apply);
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			PlayerCrackOverlay.render(matrices, tickDelta);
		});
		KeyBindingHelper.registerKeyBinding(KeyMappings.MORPH);
		KeyBindingHelper.registerKeyBinding(KeyMappings.CONFIG);
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
		ServerPlayNetworking.registerGlobalReceiver(INTERACT_PACKET, ServerBoundInteractBlockPacket::apply);
		ServerPlayNetworking.registerGlobalReceiver(SERVER_CONFIG, ServerBoundConfigUpdatePacket::apply);
		ServerLifecycleEvents.SERVER_STARTING.register(Config::setServer);
		ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(BlockomorphMod.MOD_ID, "enum_argument"), EnumArgument.class, new EnumArgument.ContextInfo());
		CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, environment) -> {
			BlockmorphCommand.register(dispatcher, commandBuildContext, environment);
			BlockmorphconfigCommand.register(dispatcher, commandBuildContext, environment);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.player;
			ServerPlayNetworking.send(p, CLIENT_CONFIG, new ClientBoundConfigUpdatePacket(Config.getInstance()));
		});
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			MorphUtils.onPlayerClone(oldPlayer, newPlayer, alive);
		});
	}

}
