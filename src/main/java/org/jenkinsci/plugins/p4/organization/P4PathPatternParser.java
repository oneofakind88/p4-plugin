package org.jenkinsci.plugins.p4.organization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Perforce organization path patterns and renders folder/job templates.
 */
public class P4PathPatternParser {

	private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}");

	public CompiledPattern compile(String pattern) {
		if (pattern == null || pattern.trim().isEmpty()) { throw new IllegalArgumentException("pattern must not be blank"); }
		String normalized = normalizeDepotPattern(pattern);
		Matcher matcher = VARIABLE.matcher(normalized);
		StringBuffer regex = new StringBuffer();
		List<String> variables = new ArrayList<>();
		int lastEnd = 0;
		while (matcher.find()) {
			String literal = normalized.substring(lastEnd, matcher.start());
			regex.append(toDepotRegex(literal));
			String variable = matcher.group(1);
			variables.add(variable);
			regex.append("([^/]+)");
			lastEnd = matcher.end();
		}
		regex.append(toDepotRegex(normalized.substring(lastEnd)));
		return new CompiledPattern(normalized, variables, Pattern.compile("^" + regex + "$"));
	}

	public String renderTemplate(String template, Map<String, String> variables) {
		if (template == null || template.trim().isEmpty()) { return ""; }
		Matcher matcher = VARIABLE.matcher(template);
		StringBuffer rendered = new StringBuffer();
		while (matcher.find()) {
			String name = matcher.group(1);
			String value = variables == null ? null : variables.get(name);
			if (value == null) { throw new IllegalArgumentException("Unknown template variable: " + name); }
			matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
		}
		matcher.appendTail(rendered);
		return rendered.toString();
	}

	public void assertTemplateVariablesKnown(String template, Collection<String> knownVariables) {
		Set<String> known = new HashSet<>(knownVariables == null ? Collections.emptySet() : knownVariables);
		Matcher matcher = VARIABLE.matcher(template == null ? "" : template);
		while (matcher.find()) {
			String variable = matcher.group(1);
			if (!known.contains(variable)) { throw new IllegalArgumentException("Template references unknown variable: " + variable); }
		}
	}

	public String normalizeItemPath(String renderedPath) {
		if (renderedPath == null || renderedPath.trim().isEmpty()) { return ""; }
		String[] segments = renderedPath.replace('\\', '/').split("/+");
		List<String> safe = new ArrayList<>();
		for (String segment : segments) {
			if (!segment.trim().isEmpty()) { safe.add(toSafeItemName(segment)); }
		}
		return String.join("/", safe);
	}

	public String toSafeItemName(String value) {
		String itemName = value == null ? "" : value.trim();
		if (itemName.contains("..")) { throw new IllegalArgumentException("Jenkins item names must not contain '..'"); }
		itemName = itemName.replaceAll("[/\\\\]+", "-");
		itemName = itemName.replaceAll("[^A-Za-z0-9._-]", "_");
		itemName = itemName.replaceAll("^-+|-+$", "");
		if (itemName.isEmpty()) { throw new IllegalArgumentException("value must produce a non-empty Jenkins item name"); }
		return itemName;
	}

	public List<String> parseIncludePaths(String text) { return parseSeparatedValues(text); }

	public Pattern parseExcludePattern(String regex) {
		if (regex == null || regex.trim().isEmpty()) { return Pattern.compile("a^", Pattern.CASE_INSENSITIVE); }
		return Pattern.compile(regex.trim());
	}

	public String toStreamsScmSourceIncludes(Collection<String> scopedIncludes) {
		if (scopedIncludes == null || scopedIncludes.isEmpty()) { return "//..."; }
		return String.join("\n", scopedIncludes);
	}

	public String toStreamsScmSourceExcludeRegex(Collection<String> excludedStreamNames) {
		if (excludedStreamNames == null || excludedStreamNames.isEmpty()) { return "a^"; }
		List<String> quoted = new ArrayList<>();
		for (String name : excludedStreamNames) {
			if (name != null && !name.trim().isEmpty()) { quoted.add(Pattern.quote(name.trim())); }
		}
		return quoted.isEmpty() ? "a^" : "^(?:" + String.join("|", quoted) + ")$";
	}

	private List<String> parseSeparatedValues(String text) {
		if (text == null || text.trim().isEmpty()) { return Collections.emptyList(); }
		String[] tokens = text.split("[\\r\\n,]+");
		List<String> values = new ArrayList<>();
		for (String token : tokens) {
			String value = token.trim();
			if (!value.isEmpty()) { values.add(value); }
		}
		return values;
	}

	private String normalizeDepotPattern(String pattern) {
		String normalized = pattern.trim().replaceAll("/+$", "");
		if (!normalized.startsWith("//")) { throw new IllegalArgumentException("pattern must start with //"); }
		return normalized;
	}

	private String toDepotRegex(String literal) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < literal.length(); i++) {
			char c = literal.charAt(i);
			if (c == '*' && literal.startsWith("...", i)) { out.append(".*"); i += 2; }
			else if (c == '*') { out.append("[^/]*"); }
			else { out.append(Pattern.quote(String.valueOf(c))); }
		}
		return out.toString();
	}

	public static final class CompiledPattern {
		private final String pattern;
		private final List<String> variableNames;
		private final Pattern regex;

		private CompiledPattern(String pattern, List<String> variableNames, Pattern regex) {
			this.pattern = pattern;
			this.variableNames = Collections.unmodifiableList(new ArrayList<>(variableNames));
			this.regex = regex;
		}

		public Optional<Map<String, String>> match(String depotPath) {
			String normalized = depotPath == null ? "" : depotPath.trim().replaceAll("/+$", "");
			Matcher matcher = regex.matcher(normalized);
			if (!matcher.matches()) { return Optional.empty(); }
			Map<String, String> values = new LinkedHashMap<>();
			for (int i = 0; i < variableNames.size(); i++) { values.put(variableNames.get(i), matcher.group(i + 1)); }
			return Optional.of(values);
		}

		public String renderRoot(Map<String, String> variables) {
			String root = pattern;
			for (String name : variableNames) {
				String value = variables.get(name);
				if (value == null) { break; }
				root = root.replace("{" + name + "}", value);
			}
			int unresolved = root.indexOf('{');
			if (unresolved >= 0) {
				int slash = root.lastIndexOf('/', unresolved);
				root = slash <= 1 ? "//..." : root.substring(0, slash);
			}
			return root.replaceAll("/+$", "");
		}

		public List<String> getVariableNames() { return variableNames; }
	}
}
