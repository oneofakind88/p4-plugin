package org.jenkinsci.plugins.p4.organization;

import java.util.Objects;

/**
 * Stable identity for a generated organization/computed-folder project.
 */
public final class P4StreamProjectKey {

	private final String depotPath;
	private final String displayPath;
	private final String itemName;

	public P4StreamProjectKey(String depotPath, String displayPath, String itemName) {
		this.depotPath = requireText(depotPath, "depotPath");
		this.displayPath = requireText(displayPath, "displayPath");
		this.itemName = requireText(itemName, "itemName");
	}

	public String getDepotPath() {
		return depotPath;
	}

	public String getDisplayPath() {
		return displayPath;
	}

	public String getItemName() {
		return itemName;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value.trim();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof P4StreamProjectKey)) {
			return false;
		}
		P4StreamProjectKey that = (P4StreamProjectKey) o;
		return depotPath.equals(that.depotPath) && displayPath.equals(that.displayPath) && itemName.equals(that.itemName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(depotPath, displayPath, itemName);
	}

	@Override
	public String toString() {
		return "P4StreamProjectKey{" +
				"depotPath='" + depotPath + '\'' +
				", displayPath='" + displayPath + '\'' +
				", itemName='" + itemName + '\'' +
				'}';
	}
}
