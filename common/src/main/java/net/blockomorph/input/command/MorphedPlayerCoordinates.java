package net.blockomorph.input.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public record MorphedPlayerCoordinates(EntitySelector selector, InPlayerBlockPos offset) implements Coordinates {
	private static final char START_CHAR = '*';
	public static class ContainerException extends RuntimeException {
		public ContainerException(CommandSyntaxException e) {
			super(null, e, false, false);
		}
	}

	@Override
	public Vec3 getPosition(CommandSourceStack sender) {
		return new Vec3(this.getBlockPos(sender));
	}

	@Override
	public BlockPos getBlockPos(CommandSourceStack sender) {
		try {
			ServerPlayer player = this.selector.findSinglePlayer(sender);
			BlockPos keyPos = this.offset.boundedBlockPos(player);
			if (keyPos == null) throw EntityArgument.NO_PLAYERS_FOUND.create();
			return keyPos;
		} catch (CommandSyntaxException e) {
			throw new ContainerException(e);
		}
	}

	@Nullable
	public static MorphedPlayerCoordinates parse(StringReader reader) throws CommandSyntaxException {
		if (reader.canRead(2) && reader.peek() == START_CHAR && reader.peek(1) == '(') {
			reader.skip(); reader.skip();
			EntitySelector selector = EntityArgument.player().parse(reader);
			reader.skipWhitespace();
			InPlayerBlockPos pos = InPlayerBlockPos.ZERO;
			if (reader.canRead() && reader.peek() == ',') {
				reader.skip(); pos = InPlayerBlockPos.get(
						readOffsetAxis(reader, Direction.Axis.X),
						readOffsetAxis(reader, Direction.Axis.Y),
						readOffsetAxis(reader, Direction.Axis.Z));
			}
			reader.expect(')');
			return new MorphedPlayerCoordinates(selector, pos);
		}
		return null;
	}

	@Nullable
	public static <S> CompletableFuture<Suggestions> parseSuggestion(CommandContext<S> context, SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof SharedSuggestionProvider source)) {
			return null;
		}
		String remaining = builder.getRemaining();
		if (remaining.isEmpty() || remaining.charAt(0) != START_CHAR) {
			return null;
		}
		if (remaining.equals(String.valueOf(START_CHAR))) {
			builder.suggest(START_CHAR + "(");
			return builder.buildFuture();
		}
		if (remaining.startsWith(START_CHAR + "(")) {
			StringReader reader = new StringReader(builder.getInput());
			int selectorStartIndex = builder.getStart() + 2;
			reader.setCursor(selectorStartIndex);

			EntitySelectorParser parser = new EntitySelectorParser(reader, true);
			boolean parsedSuccessfully = true;
			try {
				parser.parse();
			} catch (CommandSyntaxException e) {
				parsedSuccessfully = false;
			}
			reader.skipWhitespace();
			var endSuggests = checkTerminate(reader, builder);
			if (endSuggests != null) return endSuggests;
			if (parsedSuccessfully && reader.getCursor() == reader.getTotalLength()) {
				SuggestionsBuilder offsetBuilder = builder.createOffset(reader.getCursor());
				offsetBuilder.suggest(",");
				offsetBuilder.suggest(")");
				return offsetBuilder.buildFuture();
			}
			SuggestionsBuilder selectorBuilder = builder.createOffset(selectorStartIndex);
			return parser.fillSuggestions(selectorBuilder, suggests ->
					SharedSuggestionProvider.suggest(source.getOnlinePlayerNames(), suggests)
			);
		}

		return null;
	}

	private static CompletableFuture<Suggestions> checkTerminate(StringReader reader, SuggestionsBuilder builder) {
		if (reader.canRead()) {
			char nextChar = reader.peek();
			if (nextChar == ',') {
				StringReader readerEnd = new StringReader(builder.getInput().substring(reader.getCursor()));
				readerEnd.skip();

				boolean hasPos = true;
				try {
					for (int i = 0; i < 3; i++) {
						readOffsetAxis(readerEnd, Direction.Axis.values()[i]);
					}
				} catch (CommandSyntaxException e) {
					hasPos = false;
				}
				if (hasPos) {
					SuggestionsBuilder offsetBuilder = builder.createOffset(reader.getCursor() + readerEnd.getCursor());
					offsetBuilder.suggest(")");
					return offsetBuilder.buildFuture();
				}
				return Suggestions.empty();
			} else if (nextChar == ')') return Suggestions.empty();
		}
		return null;
	}

	private static int readOffsetAxis(StringReader reader, Direction.Axis axis) throws CommandSyntaxException {
		reader.skipWhitespace();
		int pos = reader.readInt();
		InPlayerBlockPos.isValidForCommand(pos, axis);
		reader.skipWhitespace();
		return pos;
	}

	@Override public Vec2 getRotation(CommandSourceStack sender) { return Vec2.ZERO; }
	@Override public boolean isXRelative() { return false; }
	@Override public boolean isYRelative() { return false; }
	@Override public boolean isZRelative() { return false; }
}
