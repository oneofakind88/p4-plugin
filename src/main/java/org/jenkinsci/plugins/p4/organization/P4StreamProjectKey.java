package org.jenkinsci.plugins.p4.organization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stable identity for a generated Perforce organization project.
 */
public final class P4StreamProjectKey {

	private final String mappingName;
	private final String depotPath;
	private final String folderPath;
	private final String jobName;
	private final String streamName;
	private final String itemPath;
	private final String streamIncludePath;
	private final Map<String, String> variables;

	public P4StreamProjectKey(String mappingName, String depotPath, String folderPath, String jobName, String streamName,
			String streamIncludePath, Map<String, String> variables) {
		this.mappingName = optionalText(mappingName);
		this.depotPath = requireText(depotPath, "depotPath");
		this.folderPath = normalizeRelativePath(folderPath);
		this.jobName = requireText(jobName, "jobName");
		this.streamName = requireText(streamName, "streamName");
		this.itemPath = this.folderPath.isEmpty() ? this.jobName : this.folderPath + "/" + this.jobName;
		this.streamIncludePath = requireText(streamIncludePath, "streamIncludePath");
		this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables == null ? Collections.emptyMap() : variables));
	}

	/**
	 * Backwards-compatible constructor used by earlier organization discovery helpers.
	 */
	public P4StreamProjectKey(String depotPath, String displayPath, String itemName) {
		this(null, depotPath, parentPath(displayPath), itemName, leafName(displayPath), depotPath, Collections.emptyMap());
	}

	public String getMappingName() { return mappingName; }
	public String getDepotPath() { return depotPath; }
	public String getFolderPath() { return folderPath; }
	public String getJobName() { return jobName; }
	public String getStreamName() { return streamName; }
	public String getItemPath() { return itemPath; }
	public String getItemName() { return itemPath; }
	public String getDisplayPath() { return itemPath; }
	public String getStreamIncludePath() { return streamIncludePath; }
	public Map<String, String> getVariables() { return variables; }

	private static String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value.trim();
	}

	private static String optionalText(String value) { return value == null ? "" : value.trim(); }

	private static String normalizeRelativePath(String value) {
		if (value == null) {
			return "";
		}
		String normalized = value.trim().replace('\\', '/').replaceAll("/{2,}", "/").replaceAll("^/+|/+$", "");
		return normalized;
	}

	private static String parentPath(String path) {
		String normalized = normalizeRelativePath(path);
		int slash = normalized.lastIndexOf('/');
		return slash < 0 ? "" : normalized.substring(0, slash);
	}

	private static String leafName(String path) {
		String normalized = normalizeRelativePath(path);
		int slash = normalized.lastIndexOf('/');
		return slash < 0 ? normalized : normalized.substring(slash + 1);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (!(o instanceof P4StreamProjectKey)) { return false; }
		P4StreamProjectKey that = (P4StreamProjectKey) o;
		return mappingName.equals(that.mappingName) && depotPath.equals(that.depotPath) && folderPath.equals(that.folderPath)
				&& jobName.equals(that.jobName) && streamName.equals(that.streamName) && itemPath.equals(that.itemPath)
				&& streamIncludePath.equals(that.streamIncludePath) && variables.equals(that.variables);
	}

	@Override
	public int hashCode() { return Objects.hash(mappingName, depotPath, folderPath, jobName, streamName, itemPath, streamIncludePath, variables); }

	@Override
	public String toString() {
		return "P4StreamProjectKey{" + "mappingName='" + mappingName + '\'' + ", depotPath='" + depotPath + '\''
				+ ", itemPath='" + itemPath + '\'' + ", streamName='" + streamName + '\'' + '}';
	}
}
