package org.jenkinsci.plugins.p4.organization;

import com.perforce.p4java.core.IStreamSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Organization-level stream discovery adapter that keeps mapping/filtering outside StreamsScmSource.
 */
public class P4StreamDiscoveryService {

	private final P4DepotPathMapping pathMapping;
	private final P4PathPatternParser patternParser;

	public P4StreamDiscoveryService() {
		this(new P4DepotPathMapping(), new P4PathPatternParser());
	}

	public P4StreamDiscoveryService(P4DepotPathMapping pathMapping, P4PathPatternParser patternParser) {
		this.pathMapping = pathMapping;
		this.patternParser = patternParser;
	}

	public List<P4StreamProjectKey> discoverProjects(Collection<IStreamSummary> streamSummaries,
			String organizationRoot, String organizationExcludeRegex) {
		if (streamSummaries == null || streamSummaries.isEmpty()) {
			return Collections.emptyList();
		}
		Pattern excludePattern = patternParser.parseExcludePattern(organizationExcludeRegex);
		List<P4StreamProjectKey> projects = new ArrayList<>();
		for (IStreamSummary summary : streamSummaries) {
			if (summary == null || summary.getStream() == null) {
				continue;
			}
			String name = summary.getName();
			if (name != null && excludePattern.matcher(name).matches()) {
				continue;
			}
			projects.add(pathMapping.toProjectKey(summary.getStream(), organizationRoot));
		}
		return projects;
	}

	public StreamsScmSourceArguments toGeneratedMultibranchSourceArguments(Collection<String> scopedIncludePaths,
			Collection<String> excludedStreamNames) {
		return new StreamsScmSourceArguments(
				patternParser.toStreamsScmSourceIncludes(scopedIncludePaths),
				patternParser.toStreamsScmSourceExcludeRegex(excludedStreamNames));
	}

	public static final class StreamsScmSourceArguments {
		private final String includes;
		private final String excludes;

		private StreamsScmSourceArguments(String includes, String excludes) {
			this.includes = includes;
			this.excludes = excludes;
		}

		public String getIncludes() {
			return includes;
		}

		public String getExcludes() {
			return excludes;
		}
	}
}
