package org.jenkinsci.plugins.p4.organization;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Maps depot stream paths to stable organization/computed-folder keys.
 */
public class P4DepotPathMapping {

	private final String name;
	private final String includePath;
	private final String folderTemplate;
	private final String jobTemplate;
	private final String streamTemplate;
	private final Pattern includeProjectPattern;
	private final Pattern excludeProjectPattern;
	private final Pattern includeStreamPattern;
	private final Pattern excludeStreamPattern;
	private final boolean streamRegexAppliesToPath;
	private final boolean enabled;
	private final P4PathPatternParser.CompiledPattern compiledPattern;
	private final P4PathPatternParser parser = new P4PathPatternParser();

	public P4DepotPathMapping() {
		this("Default", "//{project}/{stream}", "", "{project}", "{stream}", null, null, null, null, false, true);
	}

	public P4DepotPathMapping(String name, String includePath, String folderTemplate, String jobTemplate, String streamTemplate,
			String includeProjectRegex, String excludeProjectRegex, String includeStreamRegex, String excludeStreamRegex,
			boolean streamRegexAppliesToPath, boolean enabled) {
		this.name = textOrDefault(name, "Perforce Streams");
		this.includePath = requireText(includePath, "includePath");
		this.folderTemplate = folderTemplate == null ? "" : folderTemplate.trim();
		this.jobTemplate = requireText(jobTemplate, "jobTemplate");
		this.streamTemplate = requireText(streamTemplate, "streamTemplate");
		this.includeProjectPattern = compileOptional(includeProjectRegex);
		this.excludeProjectPattern = compileOptional(excludeProjectRegex);
		this.includeStreamPattern = compileOptional(includeStreamRegex);
		this.excludeStreamPattern = compileOptional(excludeStreamRegex);
		this.streamRegexAppliesToPath = streamRegexAppliesToPath;
		this.enabled = enabled;
		this.compiledPattern = parser.compile(this.includePath);
		validateTemplate(this.folderTemplate);
		validateTemplate(this.jobTemplate);
		validateTemplate(this.streamTemplate);
	}

	public Optional<P4StreamProjectKey> match(String depotPath) {
		if (!enabled) { return Optional.empty(); }
		Optional<Map<String, String>> matched = compiledPattern.match(depotPath);
		if (!matched.isPresent()) { return Optional.empty(); }
		Map<String, String> variables = matched.get();
		String folder = parser.normalizeItemPath(parser.renderTemplate(folderTemplate, variables));
		String job = parser.toSafeItemName(parser.renderTemplate(jobTemplate, variables));
		String stream = parser.renderTemplate(streamTemplate, variables).trim();
		if (stream.isEmpty()) { return Optional.empty(); }
		if (!isProjectAllowed(job) || !isStreamAllowed(stream, depotPath)) { return Optional.empty(); }
		String root = rootForStream(normalizeDepotPath(depotPath), stream);
		String include = root.endsWith("/...") ? root : root + "/...";
		return Optional.of(new P4StreamProjectKey(name, normalizeDepotPath(depotPath), folder, job, stream, include, variables));
	}

	public P4StreamProjectKey toProjectKey(String depotPath, String organizationRoot) {
		Optional<P4StreamProjectKey> key = match(depotPath);
		if (key.isPresent()) { return key.get(); }
		String normalizedDepotPath = normalizeDepotPath(depotPath);
		String displayPath = toDisplayPath(normalizedDepotPath, organizationRoot);
		String itemName = toItemName(displayPath);
		return new P4StreamProjectKey(normalizedDepotPath, displayPath, itemName);
	}

	public String toDisplayPath(String depotPath, String organizationRoot) {
		String normalizedDepotPath = normalizeDepotPath(depotPath);
		String normalizedRoot = organizationRoot == null || organizationRoot.trim().isEmpty() ? "" : normalizeDepotPath(organizationRoot);
		if (!normalizedRoot.isEmpty() && normalizedDepotPath.startsWith(normalizedRoot + "/")) {
			return normalizedDepotPath.substring(normalizedRoot.length() + 1);
		}
		return normalizedDepotPath.replaceFirst("^//", "");
	}

	public String toItemName(String displayPath) { return parser.toSafeItemName(displayPath == null ? "" : displayPath.replace('/', '-')); }
	public boolean isProjectAllowed(String projectName) { return matches(projectName, includeProjectPattern, excludeProjectPattern); }
	public boolean isStreamAllowed(String streamName) { return isStreamAllowed(streamName, streamName); }
	public boolean isStreamAllowed(String streamName, String streamPath) {
		String value = streamRegexAppliesToPath ? streamPath : streamName;
		return matches(value, includeStreamPattern, excludeStreamPattern);
	}
	public String getName() { return name; }
	public String getIncludePath() { return includePath; }
	public boolean isEnabled() { return enabled; }

	private boolean matches(String value, Pattern include, Pattern exclude) {
		String candidate = value == null ? "" : value;
		if (include != null && !include.matcher(candidate).matches()) { return false; }
		return exclude == null || !exclude.matcher(candidate).matches();
	}

	private String rootForStream(String depotPath, String streamName) {
		String suffix = "/" + streamName;
		if (depotPath.endsWith(suffix)) {
			return depotPath.substring(0, depotPath.length() - suffix.length());
		}
		int slash = depotPath.lastIndexOf('/');
		return slash <= 1 ? depotPath : depotPath.substring(0, slash);
	}

	private void validateTemplate(String template) { parser.assertTemplateVariablesKnown(template, compiledPattern.getVariableNames()); }
	private static Pattern compileOptional(String regex) { return regex == null || regex.trim().isEmpty() ? null : Pattern.compile(regex.trim()); }
	private static String textOrDefault(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
	private static String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty()) { throw new IllegalArgumentException(field + " must not be blank"); }
		return value.trim();
	}
	private String normalizeDepotPath(String depotPath) {
		if (depotPath == null || depotPath.trim().isEmpty()) { throw new IllegalArgumentException("depotPath must not be blank"); }
		String value = depotPath.trim().replaceAll("/+$", "");
		if (!value.startsWith("//")) { throw new IllegalArgumentException("depotPath must start with //"); }
		return value;
	}
}
