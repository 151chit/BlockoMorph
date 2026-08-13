package net.blockomorph.input.command.entitySelector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

public class SelectorArgType {
	private static final String[] ARGS = new String[]{"x", "y", "z", "block"};
	private static boolean REGISTERED;

	public static void register(SelectorTypeRegister register) {
		if (!REGISTERED) {
			REGISTERED = true;
			register.withCursorFallback().register(MorphUtils.MODID, parser -> {
				var reader = parser.getReader();
				boolean inverted = parser.shouldInvertValue();
				parser.setSuggestions((builder, ignored) -> {
					builder.suggest("[");
					return builder.buildFuture();
				});
				reader.expect('[');
				suggestNewArg(parser, inverted);
				BlockInPlayerTester block = checkArgs(parser);
				block.checkNotReady();
				parser.addPredicate(e -> {
					if (e instanceof PlayerAccessor pl) {
						if (inverted) return !SelectorArgTester.test(pl, block);
						else return SelectorArgTester.test(pl, block);
					}
					return inverted;
				});
			}, ignored -> true, Component.literal("test"));
		}
	}

	private static BlockInPlayerTester checkArgs(EntitySelectorParser parser) throws CommandSyntaxException {
		var reader = parser.getReader();
		Integer x = null, y = null, z = null;
		Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> block = null;
		while (reader.canRead()) {
			suggestTypedArg(parser);
			String key = reader.readUnquotedString();
			reader.expect('=');
			parser.setSuggestions((builder, ignored) -> builder.buildFuture());
			switch (key) {
				case "x" -> x = InPlayerBlockPos.isValidForCommand(checkIfEmpty(reader.readInt(), x, "x"), Direction.Axis.X);
				case "y" -> y = InPlayerBlockPos.isValidForCommand(checkIfEmpty(reader.readInt(), y, "y"), Direction.Axis.Y);
				case "z" -> z = InPlayerBlockPos.isValidForCommand(checkIfEmpty(reader.readInt(), z, "z"), Direction.Axis.Z);
				case "block" -> {
					int start = reader.getCursor();
					try {
						block = checkIfEmpty(BlockStateParser.parseForTesting(BuiltInRegistries.BLOCK, reader, true), block, "block");
					} catch (CommandSyntaxException e) {
						parser.setSuggestions((builder, ignored) ->
							BlockStateParser.fillSuggestions(BuiltInRegistries.BLOCK, builder.createOffset(start), true, true)
						);
						throw e;
					}
				}
				default -> throw EntitySelectorOptions.ERROR_INAPPLICABLE_OPTION.create(key);
			}

			if (!reader.canRead()) break;

			char next = reader.peek();
			if (next == ',') {
				reader.skip();
				reader.skipWhitespace();
				suggestTypedArg(parser);
			} else if (next == ']') {
				reader.skip();
				break;
			} else {
				throw EntitySelectorParser.ERROR_EXPECTED_END_OF_OPTIONS.create();
			}
		}
		return new BlockInPlayerTester(x, y, z, block);
	}

	private static void suggestNewArg(EntitySelectorParser parser, boolean offsetInvert) {
		parser.setSuggestions((builder, ignored) -> {
			SuggestionsBuilder offset = builder.createOffset(builder.getStart() + (offsetInvert ? 2 : 1));
			for (String arg : ARGS) {
				offset.suggest(arg + "=");
			}
			return offset.buildFuture();
		});
	}

	private static void suggestTypedArg(EntitySelectorParser parser) {
		parser.setSuggestions((builder, ignored) -> {
			String oldRem = builder.getRemaining().toLowerCase();
			int lastDelimiter = Math.max(oldRem.lastIndexOf(','), oldRem.lastIndexOf('['));
			SuggestionsBuilder offsetBuilder = builder.createOffset(builder.getStart() + lastDelimiter + 1);
			String remaining = offsetBuilder.getRemaining();
			if (!remaining.contains("=")) {
				for (String arg : ARGS) {
					arg = arg + "=";
					if (arg.startsWith(remaining.toLowerCase())) offsetBuilder.suggest(arg);
				}
			}
			return offsetBuilder.buildFuture();
		});
	}

	record BlockInPlayerTester(Integer x, Integer y, Integer z, Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> block) {
		void checkNotReady() throws CommandSyntaxException {
			this.checkNonNull(x, "x"); this.checkNonNull(y, "y"); this.checkNonNull(z, "z"); this.checkNonNull(block, "block");
		}

		private void checkNonNull(Object obj, String name) throws CommandSyntaxException {
			if (obj == null) throw EntitySelectorParser.ERROR_EXPECTED_OPTION_VALUE.create(name);
		}
	}

	private static <T> T checkIfEmpty(T obj, T old, String name) throws CommandSyntaxException {
		if (old != null) throw EntitySelectorOptions.ERROR_INAPPLICABLE_OPTION.create(name);
		return obj;
	}

	@FunctionalInterface
	public interface SelectorTypeRegister {
		void register(String name, final EntitySelectorOptions.Modifier modifier, final Predicate<EntitySelectorParser> predicate, final Component description);

		default SelectorTypeRegister withCursorFallback() {
			return (name, modifier, predicate, description) -> this.register(name, parser -> {
				int startCursor = parser.getReader().getCursor();
				try {
					modifier.handle(parser);
				} catch (Exception e) {
					parser.getReader().setCursor(startCursor);
					throw e;
				}
			}, predicate, description);
		}
	}
}
