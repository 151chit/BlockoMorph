package net.blockomorph.core;

import net.blockomorph.BlockomorphServer;
import net.blockomorph.command.BlockmorphCommand;
import net.blockomorph.command.BlockmorphconfigCommand;
import net.blockomorph.command.EnumArgument;
import net.blockomorph.network.*;
import net.blockomorph.screens.PlayerCrackOverlay;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MainBus {

	@Environment(value = EnvType.CLIENT)
	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(MainPacket.ID, (client, handler, buf, responseSender) -> {
			try {
				Runnable run = MainPacket.preApply(buf, null, true);
				run.run();
			} catch (Exception e) {
				handler.getConnection().disconnect(Component.literal(e.getMessage()));
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
		registerMain();
	}

	public static void registerServer() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.player;
			MorphUtils.sendPlayer(new ClientBoundConfigUpdatePacket(Config.getInstance()), p);
		});
		ServerPlayerEvents.COPY_FROM.register(MorphUtils::onPlayerClone);
		registerMain();
	}

	private static void registerMain() {
		ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(BlockomorphServer.MOD_ID, "enum_argument"), EnumArgument.class, new EnumArgument.ContextInfo());
		CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, environment) -> {
			BlockmorphCommand.register(dispatcher, commandBuildContext, environment);
			BlockmorphconfigCommand.register(dispatcher, commandBuildContext, environment);
		});
		ServerPlayNetworking.registerGlobalReceiver(MainPacket.ID, (server, player, handler, buf, responseSender) -> {
			try {
				Runnable run = MainPacket.preApply(buf, player, false);
				run.run();
			} catch (Exception e) {
				handler.disconnect(Component.literal(e.getMessage()));
			}
		});
		ServerLifecycleEvents.SERVER_STARTING.register(Config::setServer);
		MorphUtils.registerPacket(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket::new, true);
		MorphUtils.registerPacket(ServerBoundUseBlockPacket.ID, ServerBoundUseBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, false);
		MorphUtils.registerPacket(ServerBoundInteractBlockPacket.ID, ServerBoundInteractBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, false);
	}

}
