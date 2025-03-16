package net.blockomorph.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

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
