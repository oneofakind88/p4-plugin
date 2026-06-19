package org.jenkinsci.plugins.p4.organization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses organization-level include and exclude patterns without changing SCM source semantics.
 */
public class P4PathPatternParser {

	public List<String> parseIncludePaths(String text) {
		return parseSeparatedValues(text);
	}

	public Pattern parseExcludePattern(String regex) {
		if (regex == null || regex.trim().isEmpty()) {
			return Pattern.compile("a^", Pattern.CASE_INSENSITIVE);
		}
		return Pattern.compile(regex.trim());
	}

	public String toStreamsScmSourceIncludes(Collection<String> scopedIncludes) {
		if (scopedIncludes == null || scopedIncludes.isEmpty()) {
			return "//...";
		}
		return String.join("\n", scopedIncludes);
	}

	public String toStreamsScmSourceExcludeRegex(Collection<String> excludedStreamNames) {
		if (excludedStreamNames == null || excludedStreamNames.isEmpty()) {
			return "a^";
		}
		List<String> quoted = new ArrayList<>();
		for (String name : excludedStreamNames) {
			if (name != null && !name.trim().isEmpty()) {
				quoted.add(Pattern.quote(name.trim()));
			}
		}
		if (quoted.isEmpty()) {
			return "a^";
		}
		return "^(?:" + String.join("|", quoted) + ")$";
	}

	private List<String> parseSeparatedValues(String text) {
		if (text == null || text.trim().isEmpty()) {
			return Collections.emptyList();
		}
		String[] tokens = text.split("[\\r\\n,]+");
		List<String> values = new ArrayList<>();
		for (String token : tokens) {
			String value = token.trim();
			if (!value.isEmpty()) {
				values.add(value);
			}
		}
		return values;
	}
}
