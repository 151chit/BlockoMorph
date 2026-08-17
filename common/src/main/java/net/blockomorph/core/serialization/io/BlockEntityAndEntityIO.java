package net.blockomorph.core.serialization.io;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.MinecraftThreadLocal;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class BlockEntityAndEntityIO implements ProblemReporter {
	private static final MinecraftThreadLocal<BlockEntityAndEntityIO> ACTIVE_REPORTER = new MinecraftThreadLocal<>(false, null);
	private static final AtomicBoolean LOGGER_INIT = new AtomicBoolean();
	private static int ACTIVE_REPORTERS;
	private final int errMax, errMaxLength;
	private Set<String> errors;

	public BlockEntityAndEntityIO(Integer errMax, Integer errMaxLength) {
		this.errMax = errMax;
		this.errMaxLength = errMaxLength;
	}

	public BlockEntityAndEntityIO() {
		this(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public static BlockEntityAndEntityIO getCurrentReporter() {
		if (ACTIVE_REPORTERS == 0) return null;
		return ACTIVE_REPORTER.get();
	}

	private static void ensureActive() {
		if (ACTIVE_REPORTERS == 0)
			throw new IllegalStateException();
	}

	private boolean isValidForRecord() {
		if (this.errMaxLength <= 0 || this.errMax <= 0) return false;
		if (this.errors == null) {
			this.errors = new ObjectLinkedOpenHashSet<>();
		}
		return this.errors.size() < this.errMax;
	}

	public record ResultAndErrors<RES>(RES result, @Nullable List<String> errors) {}

	@Override
	public ProblemReporter forChild(PathElement pathElement) {
		return this;
	}

	@Override
	public void report(Problem problem) {
		ensureActive();
		if (this.isValidForRecord()) {
			this.errors.add(problem.description());
		}
	}

	public void recordLog(String problem) {
		ensureActive();
		if (this.isValidForRecord()) {
			this.errors.add(problem);
		}
	}

	public static void log(@Nullable List<String> errs, String action) {
		if (errs != null) for (String problem : errs) {
			MorphUtils.LOGGER.error("Error when {}: {}", action, problem);
		}
	}

	@Nullable
	public List<String> loadInBlockEntity(BlockEntity blockEntity, RegistryAccess access, CompoundTag tag) {
		ValueInput valueInput = TagValueInput.create(this, access, tag);
		try {
			this.setReporterActive(true);
			blockEntity.loadWithComponents(valueInput);
		} catch (Exception e) { this.handleMainError(e); } finally {
			this.setReporterActive(false);
		}
		return this.flushProblems();
	}

	public ResultAndErrors<CompoundTag> saveBlockEntity(BlockEntity blockEntity, RegistryAccess access) {
		TagValueOutput output = TagValueOutput.createWithContext(this, access);
		try {
			this.setReporterActive(true);
			blockEntity.saveWithoutMetadata(output);
		} catch (Exception e) { this.handleMainError(e); } finally {
			this.setReporterActive(false);
		}
		return new ResultAndErrors<>(output.buildResult(), this.flushProblems());
	}

	@Nullable
	public List<String> loadInEntity(Entity entity, RegistryAccess access, CompoundTag tag) {
		ValueInput valueInput = TagValueInput.create(this, access, tag);
		try {
			this.setReporterActive(true);
			entity.load(valueInput);
		} catch (Exception e) { this.handleMainError(e); } finally {
			this.setReporterActive(false);
		}
		return this.flushProblems();
	}

	public ResultAndErrors<Entity> makeEntityFromTag(Level level, CompoundTag tag) {
		ValueInput valueInput = TagValueInput.create(this, level.registryAccess(), tag);
		try {
			this.setReporterActive(true);
			Optional<Entity> entity = EntityType.create(valueInput, level, EntitySpawnReason.LOAD);
			if (entity.isPresent()) {
				return new ResultAndErrors<>(entity.get(), this.flushProblems());
			}
		} catch (Exception e) { this.handleMainError(e); } finally {
			this.setReporterActive(false);
		}
		return new ResultAndErrors<>(null, this.flushProblems());
	}

	public ResultAndErrors<CompoundTag> saveEntity(Entity entity, RegistryAccess access) {
		TagValueOutput output = TagValueOutput.createWithContext(this, access);
		try {
			this.setReporterActive(true);
			entity.save(output);
		} catch (Exception e) { this.handleMainError(e); } finally {
			this.setReporterActive(false);
		}
		return new ResultAndErrors<>(output.buildResult(), this.flushProblems());
	}

	private void handleMainError(Throwable e) {
		this.report(() -> e.getMessage() != null ? e.getMessage() : e.getClass().getName());
	}

	private void setReporterActive(boolean yes) {
		if (yes) {
			initLoggerListener();
			ACTIVE_REPORTERS++;
			ACTIVE_REPORTER.set(this);
		} else {
			ACTIVE_REPORTERS--;
			ACTIVE_REPORTER.set(null);
		}
	}

	private static void initLoggerListener() {
		if (LOGGER_INIT.compareAndSet(false, true)) {
			var ctx = LogManager.getContext(false);
			if (ctx instanceof LoggerContext slf4j) {
				slf4j.getConfiguration().getRootLogger().addFilter(new LoggerRedirector());
				slf4j.updateLoggers();
			} else {
				MorphUtils.LOGGER.warn("Cannot set logger listener, block entity or entity load errors will be sent to the general console: {}", ctx);
			}
		}
	}

	@Nullable
	private List<String> flushProblems() {
		if (this.errors == null) return null;
		var list = this.errors.stream().map(error -> {
			if (error.length() > this.errMaxLength)
				error = error.substring(0, this.errMaxLength);
			return error;
		}).collect(Collectors.toList());
		this.errors = null;
		return list;
	}
}
