package net.blockomorph.core;

import net.blockomorph.BlockomorphServer;
import net.blockomorph.command.*;
import net.blockomorph.screens.PlayerCrackOverlay;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.network.*;
import net.blockomorph.utils.config.*;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

public class MainBus {
   
    public static void registerClient() {
		registerMain();
   	    ClientPlayNetworking.registerGlobalReceiver(MainPacket.ID, (payload, context) -> {
			try {
				MainPacket.apply(payload, null, true);
			} catch (Exception e) {
				context.client().getConnection().getConnection().disconnect(Component.literal(e.getMessage()));
			}
		});
		HudRenderCallback.EVENT.register(PlayerCrackOverlay::render);
		KeyMappings.registerKeyMappings(KeyBindingHelper::registerKeyBinding);
        ClientTickEvents.END_CLIENT_TICK.register((mc) -> {
			MorphUtils.onClientTick();
		});
		WorldRenderEvents.BEFORE_ENTITIES.register((context) -> {
			MorphUtils.onPick();
		});
    }

    public static void registerServer() {
		registerMain();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
        	ServerPlayer p = handler.player;
   	    	MorphUtils.sendPlayer(new ClientBoundConfigUpdatePacket(Config.getInstance()), p);
   	    });
		ServerPlayerEvents.COPY_FROM.register(MorphUtils::onPlayerClone);
    }

	private static void registerMain() {
		ArgumentTypeRegistry.registerArgumentType(
				ResourceLocation.fromNamespaceAndPath(BlockomorphServer.MOD_ID, "enum_argument"),
				EnumArgument.class,
				new EnumArgument.ContextInfo()
		);
		CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, environment) -> {
			BlockmorphCommand.register(dispatcher, commandBuildContext, environment);
			BlockmorphconfigCommand.register(dispatcher, commandBuildContext, environment);
		});
		ServerLifecycleEvents.SERVER_STARTING.register(Config::setServer);
		PayloadTypeRegistryImpl.PLAY_C2S.register(MainPacket.ID, MainPacket.STREAM_CODEC);
		PayloadTypeRegistryImpl.PLAY_S2C.register(MainPacket.ID, MainPacket.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(MainPacket.ID, (packet, context) -> {
			try {
				MainPacket.apply(packet, context.player(), false);
			} catch (Exception e) {
				context.player().connection.disconnect(Component.literal(e.getMessage()));
			}
		});
		MorphUtils.registerPacket(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket::new, true);
		MorphUtils.registerPacket(ServerBoundUseBlockPacket.ID, ServerBoundUseBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, false);
		MorphUtils.registerPacket(ServerBoundInteractBlockPacket.ID, ServerBoundInteractBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, false);
	}

}
