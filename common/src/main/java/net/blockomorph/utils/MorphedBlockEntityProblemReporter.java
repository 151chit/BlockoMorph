package net.blockomorph.utils;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MorphedBlockEntityProblemReporter implements ProblemReporter {
	private final Integer limit;
	private final Integer stringLength;
	private final Set<Problem> problems = new LinkedHashSet<>();

	public MorphedBlockEntityProblemReporter(@Nullable Integer limit, @Nullable Integer stringLength) {
		this.limit = limit;
		this.stringLength = stringLength;
	}

	@Override
	public MorphedBlockEntityProblemReporter forChild(PathElement pathElement) {
		return this;
	}

	@Override
	public void report(Problem problem) {
		if (this.limit != null && this.problems.size() >= this.limit) return;
		this.problems.add(problem);
	}

	@Nullable
	public List<String> getProblemsIfNotEmpty() {
		if (this.problems.isEmpty() || (this.stringLength != null && this.stringLength == 0)) return null;
		ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
		builder.add(this.problems.stream().map(problem -> {
			String error = problem.description();
			if (this.stringLength != null && error.length() > this.stringLength)
				error = error.substring(0, this.stringLength);
			return error;
		}).collect(Collectors.joining()));
		return builder.build();
	}
}
