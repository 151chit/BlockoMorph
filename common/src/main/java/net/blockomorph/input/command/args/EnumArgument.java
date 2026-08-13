package net.blockomorph.input.command.args;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class EnumArgument<ENUM extends Enum<ENUM>> implements ArgumentType<ENUM> {
	private static final Dynamic2CommandExceptionType INVALID_VALUE = new Dynamic2CommandExceptionType(
			(found, constants) -> Component.translatable("blockomorph.commands.enum_argument.wrong", found, constants)
	);
	private final Class<ENUM> type;

	private EnumArgument(Class<ENUM> type) {
		this.type = type;
	}

	@Override
	public ENUM parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readUnquotedString();
		try {
			return Enum.valueOf(this.type, name);
		} catch (IllegalArgumentException e) {
			throw INVALID_VALUE.createWithContext(reader, name, this.getEnumList().toString());
		}
	}

	public static <ENUM_CLASS extends Enum<ENUM_CLASS>> EnumArgument<ENUM_CLASS> enumArg(Class<ENUM_CLASS> type) {
		return new EnumArgument<>(type);
	}

	public static <ENUM_CLASS extends Enum<ENUM_CLASS>> ENUM_CLASS getEnum(CommandContext<CommandSourceStack> commandContext, String name, Class<ENUM_CLASS> data) {
		return commandContext.getArgument(name, data);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(this.getEnumList(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return this.getEnumList();
	}

	private List<String> getEnumList() {
		return Stream.of(this.type.getEnumConstants()).map(Enum::name).toList();
	}

	public static class ContextInfo<ENUM extends Enum<ENUM>> implements MorphUtils.ArgumentEncoder<EnumArgument<ENUM>, ContextInfo<ENUM>.Template> {
		@Override
		public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
			buffer.writeUtf(template.enumType.getName());
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
			try {
				return new Template((Class<ENUM>) Class.forName(buffer.readUtf()));
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		@Override
		public void serializeToJson(Template template, JsonObject json) {
			json.addProperty("enum", template.enumType.getName());
		}

		@Override
		public Template unpack(EnumArgument<ENUM> argument) {
			return new Template(argument.type);
		}

		@Override
		public Class<?> getArgClass() {
			return EnumArgument.class;
		}

		public class Template implements ArgumentTypeInfo.Template<EnumArgument<ENUM>> {
			final Class<ENUM> enumType;

			private Template(Class<ENUM> enumType) {
				this.enumType = enumType;
			}

			@Override
			public EnumArgument<ENUM> instantiate(CommandBuildContext c) {
				return new EnumArgument<>(this.enumType);
			}

			@Override
			public ArgumentTypeInfo<EnumArgument<ENUM>, ?> type() {
				return ContextInfo.this;
			}
		}
	}
}
