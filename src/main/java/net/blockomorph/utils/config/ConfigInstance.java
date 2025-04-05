package net.blockomorph.utils.config;

import net.minecraft.network.FriendlyByteBuf;
import com.google.gson.JsonElement;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public abstract class ConfigInstance<T> {
   private final String name;
   protected T value;
   private final Component tip;

   public ConfigInstance(String n, T value) {
   	  this(n, value, null);
   }

   public ConfigInstance(String n, T value, @Nullable Component tip) {
   	  this.name = n;
   	  this.value = value;
   	  this.tip = tip;
   }

   public String getName() {
   	  return this.name;
   }

   @Nullable
   public Component getTooltip() {
   	  return this.tip;
   }
   
   public abstract void parse(String value);

   public abstract void readBufer(FriendlyByteBuf buf);
   public abstract void writeBufer(FriendlyByteBuf buf);
   public abstract ArgumentBuilder work(LiteralArgumentBuilder b, CommandBuildContext c);

   public abstract JsonElement serialize();

   public T setValue(T value) {
   	  this.value = value;
   	  return this.value;
   }
   public T getValue() {
   	  return this.value;
   }
}
