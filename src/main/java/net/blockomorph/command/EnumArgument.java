package net.blockomorph.command;

import java.util.stream.Stream;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;

public class EnumArgument<T extends Enum<T>> implements ArgumentType<T> {
    private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType(
            (found, constants) -> Component.translatable("commands.blockmorph.enumArg", found, constants));
    private final Class<T> enumClass;
    

    public static <R extends Enum<R>> EnumArgument<R> enumArgument(Class<R> enumClass) {
        return new EnumArgument<>(enumClass);
    }
    
    private EnumArgument(final Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    public T parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            throw INVALID_ENUM.createWithContext(reader, name, Arrays.toString(Stream.of(enumClass.getEnumConstants()).map(Enum::name).toArray()));
        }
    }

    public static <R extends Enum<R>> R getEnum(final CommandContext<?> context, Class<R> enumClass, final String name) {
        return context.getArgument(name, enumClass);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Stream.of(enumClass.getEnumConstants()).map(Enum::name), builder);
    }

    public Collection<String> getExamples() {
        return Stream.of(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
    }

    public static class ContextInfo<T extends Enum<T>> implements ArgumentTypeInfo<EnumArgument<T>, ContextInfo<T>.Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
            buffer.writeUtf(template.enumClass.getName());
        }

        //@SuppressWarnings("unchecked")
        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            try {
                String name = buffer.readUtf();
                return new Template((Class<T>) Class.forName(name));
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("enum", template.enumClass.getName());
        }

        @Override
        public Template unpack(EnumArgument<T> argument) {
            return new Template(argument.enumClass);
        }

        public class Template implements ArgumentTypeInfo.Template<EnumArgument<T>> {
            final Class<T> enumClass;

            private Template(Class<T> enumClass) {
                this.enumClass = enumClass;
            }

            @Override
            public EnumArgument<T> instantiate(CommandBuildContext c) {
                return new EnumArgument<>(this.enumClass);
            }

            @Override
            public ArgumentTypeInfo<EnumArgument<T>, ?> type() {
                return ContextInfo.this;
            }
        }
    }

}

