package net.blockomorph.core.serialization.io;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;

public class LoggerRedirector extends AbstractFilter {

	@Override
	public Result filter(LogEvent event) {
		if (event == null || event.getMessage() == null) return Result.NEUTRAL;
		return this.redirectIfNeed(event.getLevel(), event.getMessage().getFormattedMessage(), event.getThrown());
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
		if (msg == null) return Result.NEUTRAL;
		return this.redirectIfNeed(level, msg.getFormattedMessage(), t);
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
		return this.redirectIfNeed(level, msg, t);
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
		BlockEntityAndEntityIO recorder = this.isValid(level);
		if (recorder != null && msg != null && !msg.isEmpty()) {
			String message = ParameterizedMessage.format(msg, params);
			recorder.recordLog(message);
			return Result.DENY;
		}
		return Result.NEUTRAL;
	}

	private Result redirectIfNeed(Level level, Object error, Throwable throwable) {
		if (error != null) {
			String err = error.toString();
			if (err.isEmpty()) return Result.NEUTRAL;
			BlockEntityAndEntityIO recorder = this.isValid(level);
			if (recorder != null) {
				recorder.recordLog(err + this.formatError(throwable));
				return Result.DENY;
			}
		}
		return Result.NEUTRAL;
	}

	private String formatError(Throwable throwable) {
		if (throwable != null) {
			return " [Error]: " + (throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName());
		}
		return "";
	}

	private BlockEntityAndEntityIO isValid(Level level) {
		if (level != null && level.isMoreSpecificThan(Level.WARN)) {
			return BlockEntityAndEntityIO.getCurrentReporter();
		}
		return null;
	}
}
