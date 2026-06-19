package org.jenkinsci.plugins.p4.organization;

/**
 * Maps depot stream paths to stable organization/computed-folder keys.
 */
public class P4DepotPathMapping {

	public P4StreamProjectKey toProjectKey(String depotPath, String organizationRoot) {
		String normalizedDepotPath = normalizeDepotPath(depotPath);
		String displayPath = toDisplayPath(normalizedDepotPath, organizationRoot);
		String itemName = toItemName(displayPath);
		return new P4StreamProjectKey(normalizedDepotPath, displayPath, itemName);
	}

	public String toDisplayPath(String depotPath, String organizationRoot) {
		String normalizedDepotPath = normalizeDepotPath(depotPath);
		String normalizedRoot = organizationRoot == null ? "" : normalizeDepotPath(organizationRoot);
		if (!normalizedRoot.isEmpty() && normalizedDepotPath.startsWith(normalizedRoot + "/")) {
			return normalizedDepotPath.substring(normalizedRoot.length() + 1);
		}
		return normalizedDepotPath.replaceFirst("^//", "");
	}

	public String toItemName(String displayPath) {
		String itemName = displayPath == null ? "" : displayPath.trim();
		itemName = itemName.replaceAll("[/\\\\]+", "-");
		itemName = itemName.replaceAll("[^A-Za-z0-9._-]", "_");
		itemName = itemName.replaceAll("^-+|-+$", "");
		if (itemName.isEmpty()) {
			throw new IllegalArgumentException("displayPath must produce a non-empty Jenkins item name");
		}
		return itemName;
	}

	private String normalizeDepotPath(String depotPath) {
		if (depotPath == null || depotPath.trim().isEmpty()) {
			throw new IllegalArgumentException("depotPath must not be blank");
		}
		String value = depotPath.trim().replaceAll("/+$", "");
		if (!value.startsWith("//")) {
			throw new IllegalArgumentException("depotPath must start with //");
		}
		return value;
	}
}
