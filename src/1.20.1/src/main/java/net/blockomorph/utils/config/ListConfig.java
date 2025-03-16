package net.blockomorph.utils.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ListConfig extends ConfigInstance<List<String>> {
    public ListConfig(String name, List<String> value) {
        super(name, value);
    }

    public ListConfig(String name, List<String> value, Component t) {
        super(name, value, t);
    }

    @Override
    public void parse(String value) {
        String[] parts = value.split(" ", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid input format: " + value);
        }

        String element = parts[0];
        String action = parts[1];

        switch (action) {
            case "+":
                if (!getValue().contains(element))
                   getValue().add(element);
                break;
            case "-":
                if (getValue().contains(element))
                   getValue().remove(element);
                break;
            case "r":
                getValue().clear();
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    public ArgumentBuilder work(LiteralArgumentBuilder b, CommandBuildContext c) {
   	  return b.then(Commands.argument("value", ResourceArgument.resource(c, Registries.BLOCK)).
                then(Commands.literal("add").executes(args -> {
                	return this.parseArgument(args, " +");
				})).then(Commands.literal("delete").executes(args -> {
                	return this.parseArgument(args, " -");
				})).then(Commands.literal("clear").executes(args -> {
                	return this.parseArgument(args, " r");
				})) );
    }

    private int parseArgument(CommandContext<CommandSourceStack> s, String mode) throws CommandSyntaxException {
    	Holder.Reference<Block> bl = ResourceArgument.getResource(s, "value", Registries.BLOCK);
        String name = bl.key().location().toString();
        this.parse(name + mode);
        Config.getInstance().getOption(this.getName()).setValue(this.value);
        Config.getInstance().makeDirty();
        s.getSource().sendSuccess(() -> {
        	if (this.value.isEmpty()) {
        		return Component.translatable("commands.blockmorph.config.emptyList");
        	}
            return Component.translatable("commands.blockmorph.config", this.getName(), name);
         }, true);
        return 1;
    }

    public void deserialize(JsonArray arr) {
    	for (JsonElement el : arr) {
    		value.add(el.getAsString());
    	}
    }

    @Override
    public void readBufer(FriendlyByteBuf buf) {
        value.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
          String block = buf.readUtf(); 
          value.add(block); 
        }
    }

    @Override
    public void writeBufer(FriendlyByteBuf buf) {
        buf.writeInt(value.size());
   	    for (String s : value) {
   	  	  buf.writeUtf(s);
   	    }
    }

    @Override
    public JsonElement serialize() {
        JsonArray jsonArray = new JsonArray();
        for (String item : getValue()) {
            jsonArray.add(item);
        }
        return jsonArray;
    }
}
