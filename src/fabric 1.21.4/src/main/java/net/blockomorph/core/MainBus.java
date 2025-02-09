package net.blockomorph.core;

import net.blockomorph.command.*;
import net.blockomorph.core.KeyMappings;
import net.blockomorph.screens.PlayerCrackOverlay;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.*;
import net.blockomorph.utils.config.*;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

public class MainBus {
   
   public static void registerClient() {
   	    ClientPlayNetworking.registerGlobalReceiver(ClientBoundConfigUpdatePacket.ID, (payload, context) -> {
		  	context.client().execute(() -> {
                ClientBoundConfigUpdatePacket.apply(context.client());
            });
		});
		HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
			PlayerCrackOverlay.render(matrices, tickDelta);
		});
		KeyBindingHelper.registerKeyBinding(KeyMappings.MORPH);
        KeyBindingHelper.registerKeyBinding(KeyMappings.MORPH_CONFIG);
        KeyBindingHelper.registerKeyBinding(KeyMappings.CONFIG);
        ClientTickEvents.END_CLIENT_TICK.register((mc) -> {
			MorphUtils.onClientTick();
		});
		WorldRenderEvents.BEFORE_ENTITIES.register((context) -> {
			MorphUtils.onPick();
		});
   }

   public static void registerServer() {
   	    PayloadTypeRegistryImpl.PLAY_C2S.register(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket.STREAM_CODEC);
   	    PayloadTypeRegistryImpl.PLAY_C2S.register(ServerBoundInteractBlockPacket.ID, ServerBoundInteractBlockPacket.STREAM_CODEC);
   	    PayloadTypeRegistryImpl.PLAY_C2S.register(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket.STREAM_CODEC);
   	    PayloadTypeRegistryImpl.PLAY_S2C.register(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket.STREAM_CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(Config::setServer);
        ArgumentTypeRegistry.registerArgumentType(
		    ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MOD_ID, "enum_argument"),
		    EnumArgument.class,
		    new EnumArgument.ContextInfo()
		);
		
   	    ServerPlayNetworking.registerGlobalReceiver(ServerBoundBlockMorphPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerBoundBlockMorphPacket.apply(payload, context.player());
            });
        });
   	    ServerPlayNetworking.registerGlobalReceiver(ServerBoundInteractBlockPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerBoundInteractBlockPacket.apply(payload, context.player());
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(ServerBoundConfigUpdatePacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerBoundConfigUpdatePacket.apply(payload, context.player());
            });
        });
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
        	ServerPlayer p = handler.player;
   	    	ServerPlayNetworking.send(p, new ClientBoundConfigUpdatePacket(Config.getInstance()));
   	    });
      	CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, environment) -> {
			BlockmorphCommand.register(dispatcher, commandBuildContext, environment);
            BlockmorphconfigCommand.register(dispatcher, commandBuildContext, environment);
		});
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			MorphUtils.onPlayerClone(oldPlayer, newPlayer, alive);
		});
   }
}
