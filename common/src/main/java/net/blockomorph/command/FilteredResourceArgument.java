package net.blockomorph.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.arguments.ResourceArgument.ERROR_INVALID_RESOURCE_TYPE;
import static net.minecraft.commands.arguments.ResourceArgument.ERROR_UNKNOWN_RESOURCE;

public class FilteredResourceArgument<REGISTRY> implements ArgumentType<ResourceKey<REGISTRY>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
	private static final DynamicCommandExceptionType BANNED_RESOURCE = new DynamicCommandExceptionType(obj -> Component.translatable("blockomorph.commands.filtered_resource_argument.banned", obj));
	private final ResourceKey<? extends Registry<REGISTRY>> registryId;
	private final HolderLookup<REGISTRY> registryLookup;
	private final Set<ResourceLocation> censorList;
	private final boolean blackList;

	public FilteredResourceArgument(CommandBuildContext ctx, ResourceKey<? extends Registry<REGISTRY>> registryId, Set<ResourceLocation> censorList, boolean blackList) {
		this.registryId = registryId;
		this.censorList = censorList;
		this.registryLookup = ctx.holderLookup(this.registryId);
		this.blackList = blackList;
	}

	public static <TYPE> FilteredResourceArgument<TYPE> id(CommandBuildContext ctx, ResourceKey<? extends Registry<TYPE>> registryId, Set<ResourceLocation> censorList, boolean blackList) {
		return new FilteredResourceArgument<>(ctx, registryId, censorList, blackList);
	}

	public static <TYPE> ResourceKey<TYPE> getId(CommandContext<CommandSourceStack> ctx, String name, ResourceKey<Registry<TYPE>> registryId) throws CommandSyntaxException {
		ResourceKey<?> rawKey = ctx.getArgument(name, ResourceKey.class);
		Optional<ResourceKey<TYPE>> verifiedKey = rawKey.cast(registryId);
		return verifiedKey.orElseThrow(() ->
				ERROR_INVALID_RESOURCE_TYPE.create(rawKey.location(), rawKey.registry(), registryId.location()));
	}

	@Override
	public ResourceKey<REGISTRY> parse(StringReader stringReader) throws CommandSyntaxException {
		ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
		ResourceKey<REGISTRY> key = ResourceKey.create(this.registryId, resourceLocation);
		if (this.blackList == this.censorList.contains(resourceLocation)) {
			throw BANNED_RESOURCE.createWithContext(stringReader, resourceLocation);
		}
		this.registryLookup.get(key).orElseThrow(() -> ERROR_UNKNOWN_RESOURCE.createWithContext(stringReader, resourceLocation, this.registryId.location()));
		return key;
	}

	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
		if (commandContext.getSource() instanceof SharedSuggestionProvider provider) {
			return provider.registryAccess().lookup(this.registryId).map((registry) -> {
				SharedSuggestionProvider.suggestResource(registry.listElementIds().map(ResourceKey::location)
						.filter(key -> this.blackList != this.censorList.contains(key)), suggestionsBuilder);
				return suggestionsBuilder.buildFuture();
			}).orElseGet(() -> provider.customSuggestion(commandContext));
		} 
		return suggestionsBuilder.buildFuture();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public static class ContextInfo<T> implements MorphUtils.ArgumentEncoder<FilteredResourceArgument<T>, ContextInfo<T>.Template> {

		public void serializeToNetwork(ContextInfo<T>.Template template, FriendlyByteBuf buf) {
			buf.writeResourceLocation(template.registryId.location());
			buf.writeCollection(template.bannedValues, FriendlyByteBuf::writeResourceLocation);
			buf.writeBoolean(template.blackList);
		}

		public ContextInfo<T>.Template deserializeFromNetwork(FriendlyByteBuf buf) {
			ResourceKey<? extends Registry<T>> key = ResourceKey.createRegistryKey(buf.readResourceLocation());
			Set<ResourceLocation> bannedValues = buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation);
			boolean blackList = buf.readBoolean();
			return new ContextInfo<T>.Template(key, bannedValues, blackList);
		}

		public void serializeToJson(ContextInfo<T>.Template template, JsonObject jsonObject) {
			jsonObject.addProperty("registry", template.registryId.location().toString());
			JsonArray banned = new JsonArray();
			for (ResourceLocation key : template.bannedValues) {
				banned.add(key.toString());
			}
			jsonObject.add("censorList", banned);
			jsonObject.addProperty("blackList", template.blackList);
		}

		public ContextInfo<T>.Template unpack(FilteredResourceArgument<T> argument) {
			return new ContextInfo<T>.Template(argument.registryId, argument.censorList, argument.blackList);
		}

		@Override
		public Class<?> getArgClass() {
			return FilteredResourceArgument.class;
		}

		public final class Template implements ArgumentTypeInfo.Template<FilteredResourceArgument<T>> {
			private final ResourceKey<? extends Registry<T>> registryId;
			private final Set<ResourceLocation> bannedValues;
			private final boolean blackList;

			private Template(ResourceKey<? extends Registry<T>> registryId, Set<ResourceLocation> bannedValues, boolean blackList) {
				this.registryId = registryId;
				this.bannedValues = bannedValues;
				this.blackList = blackList;
			}

			public FilteredResourceArgument<T> instantiate(CommandBuildContext ctx) {
				return new FilteredResourceArgument<>(ctx, this.registryId, this.bannedValues, this.blackList);
			}

			public ArgumentTypeInfo<FilteredResourceArgument<T>, ?> type() {
				return ContextInfo.this;
			}
		}
	}
}
