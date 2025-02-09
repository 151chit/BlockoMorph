package net.blockomorph.utils.config;

import java.util.ArrayList;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.ClientBoundConfigUpdatePacket;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

public class Config {
   private static final String configDir = FMLPaths.GAMEDIR.get()+ "\\config\\blockomorph.json";
   public final List<ConfigInstance<?>> options = List.of(
   	   new EnumConfig("listMode", Mode.NONE), 
   	   new BooleanConfig("solidBlocksOnly", false),
   	   new ListConfig("allowedBlocks", new ArrayList<>()),
   	   new ListConfig("bannedBlocks", new ArrayList<>()),
   	   new BooleanConfig("playerDieAfterDestroy", true),
   	   new BooleanConfig("canOperatorModifyConfig", true)
   );
   static Config INSTANCE;

   private Config() {
   	  INSTANCE = this;
   }

   public <T> T getValue(String option) {
   	  return (T) this.getOption(option).getValue();
   }

   public <T> ConfigInstance<T> getOption(String option) {
   	  for (ConfigInstance<?> con : this.options) {
   	  	if (con.getName().equals(option)) {
   	  		return (ConfigInstance<T>) con;
   	  	}
   	  }
   	  throw new IllegalArgumentException("Option not found: " + option);
   }

   public void makeDirty() {
   	  write();
   	  PacketDistributor.sendToAllPlayers(new ClientBoundConfigUpdatePacket(this));
   }

   public void parse(String op, String val, boolean isPacket) {
   	  for (ConfigInstance<?> con : this.options) {
   	  	if (con.getName().equals(op) && !con.getName().equals("canOperatorModifyConfig")) {
   	  		con.parse(val);
   	  		this.makeDirty();
   	  		return;
   	  	}
   	  }
   	  if (isPacket)
        throw new IllegalStateException("Invalid option name: " + op);
   }

   public void writeInBufer(FriendlyByteBuf buf) {
   	  for (ConfigInstance<?> con : this.options) {
   	  	con.writeBufer(buf);
   	  }
   }

   public static Config readFromBufer(FriendlyByteBuf buf) {
   	  new Config();
   	  for (ConfigInstance<?> con : INSTANCE.options) {
   	  	con.readBufer(buf);
   	  }
   	  return INSTANCE;
   }

   public static Config getInstance() {
   	  return INSTANCE;
   }

   public static Config load() {
   	  Path path = Path.of(configDir);
   	  new Config();
   	  if (!Files.exists(path)) {
   	  	INSTANCE.write();
   	  	return INSTANCE;
   	  }
   	  Gson gson = new GsonBuilder().setPrettyPrinting().create();
      try (BufferedReader reader = new BufferedReader(new FileReader(configDir))) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            for (ConfigInstance<?> option : INSTANCE.options) {
                JsonElement element = jsonObject.get(option.getName());
                if (element != null) {
                	if (option instanceof ListConfig e) {
                	    e.deserialize(element.getAsJsonArray());
                	} else option.parse(element.getAsString());
                }
            }
      } catch (Exception e) {
            e.printStackTrace();
      }
      return INSTANCE;
   }

   protected void write() {
   	  Gson gson = new GsonBuilder().setPrettyPrinting().create();
   	  
   	  JsonObject jsonObject = new JsonObject();
      for (ConfigInstance<?> option : this.options) {
            jsonObject.add(option.getName(), option.serialize());
      }
      
   	  try (FileWriter writer = new FileWriter(configDir)) {
            gson.toJson(jsonObject, writer);
      } catch (IOException e) {
            e.printStackTrace();
      }
   }

   public enum Mode {
   	  NONE,
   	  BLACKLIST,
   	  WHITELIST
   }
}
