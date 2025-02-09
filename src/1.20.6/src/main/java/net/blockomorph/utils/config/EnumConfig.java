package net.blockomorph.utils.config;

import net.minecraft.network.FriendlyByteBuf;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonElement;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.neoforged.neoforge.server.command.EnumArgument;

public class EnumConfig<T extends Enum<T>> extends ConfigInstance<T> {
    private final Class<T> classType;
    public EnumConfig(String name, T value) {
        super(name, value);
        this.classType = (Class<T>) ((Enum<?>) getValue()).getDeclaringClass();
    }
    
    @Override
    public void parse(String value) {
        try {
            T enumValue = Enum.valueOf(this.classType, value);
            setValue(enumValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid enum value: " + value);
        }
    }

    public ArgumentBuilder work(LiteralArgumentBuilder b, CommandBuildContext c) {
   	  return b.then(Commands.argument("value", EnumArgument.enumArgument(this.classType)).executes(args -> {
   	  	 this.value = args.getArgument("value", this.classType);
   	  	 Config.getInstance().getOption(this.getName()).setValue(this.value);
   	  	 Config.getInstance().makeDirty();
   	  	 args.getSource().sendSuccess(() -> {
            return Component.translatable("commands.blockmorph.config", this.getName(), this.value + "");
         }, true);
         return 1;
	  }));
    }

    public JsonElement serialize() {
   	    return new JsonPrimitive(value.name());
    }

    public void readBufer(FriendlyByteBuf buf) {
   	    this.value = buf.readEnum(this.classType);
    }
    
    public void writeBufer(FriendlyByteBuf buf) {
   	    buf.writeEnum(this.value);
    }

    public T[] getAllEnumValues() {
        return (T[]) this.classType.getEnumConstants();
    }
}
