package net.blockomorph.utils.config;

import com.google.gson.*;
import net.blockomorph.network.ClientBoundConfigUpdatePacket;
import net.blockomorph.network.MorphNetwork;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class Config {//todo: refactor need for remove singleton and bound to level
	private static final Gson WRITER = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = MorphUtils.getGameDir().resolve("config").resolve("blockomorph.json").toFile();
	private static volatile ConfigStorage SNAPSHOT;

	public static ConfigStorage get() {
		if (SNAPSHOT == null)
			throw new IllegalAccessError("Config not loaded!");
		return SNAPSHOT;
	}

	public static void dummyInit() {
		SNAPSHOT = new ConfigStorage();
	}


	/*-------------------------------- I/O BLOCK ------------------------------*/
	public static void loadOnServer() {
		SNAPSHOT = new ConfigStorage();
		if (!Files.exists(CONFIG_FILE.toPath())) {
			write();
			return;
		}
		AtomicBoolean needRewrite = new AtomicBoolean();
		try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			for (ConfigInstance<?> option : SNAPSHOT.RECURSIVE_OPTIONS) {
				JsonElement property = jsonObject.get(option.getName());
				if (property != null) option.readFromStorage(property);
				option.fixOldData(jsonObject, () -> needRewrite.set(true));
			}
		} catch (Exception e) {
			MorphUtils.LOGGER.error("Cannot read Blockomorph config: ", e);
		}
		if (needRewrite.get()) {
			write();
		}
	}

	private static void write() {
		JsonObject jsonObject = new JsonObject();
		for (ConfigInstance<?> option : SNAPSHOT.RECURSIVE_OPTIONS) {
			jsonObject.add(option.getName(), option.getDataForStorage());
		}

		try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
			WRITER.toJson(jsonObject, writer);
		} catch (IOException e) {
			MorphUtils.LOGGER.error("Cannot write Blockomorph config, changes lost: ", e);
		}
	}
	/*-------------------------------------------------------------------------*/


	/*------------------------------- NETWORK BLOCK ---------------------------*/
	public static void writeAndSend() {
		write();
		MorphNetwork.sendAll(Side.serverOrThrow(), new ClientBoundConfigUpdatePacket(SNAPSHOT));
	}

	public static void receiveOnClient(FriendlyByteBuf byteBuf) {
		if (SNAPSHOT == null)
			SNAPSHOT = new ConfigStorage();
		for (ConfigInstance<?> instance : SNAPSHOT.LINEAR_OPTIONS) {
			instance.readFromNetwork(byteBuf);
		}
	}

	public static void parse(ServerPlayer ctx, String optionName, String value, boolean fromNetwork) {
		if (SNAPSHOT == null) return;
		ConfigInstance<?> option = getOption(optionName);
		if (option.canEditedByOperators()) {
			option.parseFromUser(ctx, value);
			writeAndSend();
			return;
		}
		if (fromNetwork)
			throw new IllegalStateException("Invalid option name: " + optionName);
	}

	/*-------------------------------------------------------------------------*/


	private static ConfigInstance<?> getOption(String name) {
		for (ConfigInstance<?> option : SNAPSHOT.LINEAR_OPTIONS) {
			if (option.getName().equals(name)) {
				return option;
			}
		}
		throw new IllegalArgumentException("Option not found: " + name);
	}
}
