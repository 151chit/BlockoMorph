package net.blockomorph.utils.config;

import net.minecraft.network.FriendlyByteBuf;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandBuildContext;

public class BooleanConfig extends ConfigInstance<Boolean> {
   public BooleanConfig(String n, boolean value) {
   	  super(n, value);
   }

   public BooleanConfig(String n, boolean value, Component t) {
   	  super(n, value, t);
   }
   
   public void parse(String value) {
   	  this.value = Boolean.parseBoolean(value);
   }

   public JsonElement serialize() {
   	  return new JsonPrimitive(value);
   }

   public ArgumentBuilder work(LiteralArgumentBuilder b, CommandBuildContext c) {
   	  return b.then(Commands.argument("value", BoolArgumentType.bool()).executes(args -> {
   	  	 this.value = BoolArgumentType.getBool(args, "value");
   	  	 Config.getInstance().getOption(this.getName()).setValue(this.value);
   	  	 Config.getInstance().makeDirty();
   	  	 args.getSource().sendSuccess(() -> {
            return Component.translatable("commands.blockmorph.config", this.getName(), this.value + "");
         }, true);
         return 1;
	  }));
   }

   public void readBufer(FriendlyByteBuf buf) {
   	  this.value = buf.readBoolean();
   }
   public void writeBufer(FriendlyByteBuf buf) {
   	  buf.writeBoolean(this.value);
   }
}
