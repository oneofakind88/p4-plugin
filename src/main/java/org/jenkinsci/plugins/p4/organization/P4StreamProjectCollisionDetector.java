package org.jenkinsci.plugins.p4.organization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups discovered projects by generated Jenkins item name and reports collisions.
 */
public class P4StreamProjectCollisionDetector {

	public Map<String, List<P4StreamProjectKey>> groupByItemName(Collection<P4StreamProjectKey> keys) {
		Map<String, List<P4StreamProjectKey>> grouped = new LinkedHashMap<>();
		if (keys == null) {
			return grouped;
		}
		for (P4StreamProjectKey key : keys) {
			if (key == null) {
				continue;
			}
			grouped.computeIfAbsent(key.getItemName(), ignored -> new ArrayList<>()).add(key);
		}
		return grouped;
	}

	public Map<String, List<P4StreamProjectKey>> findCollisions(Collection<P4StreamProjectKey> keys) {
		Map<String, List<P4StreamProjectKey>> collisions = new LinkedHashMap<>();
		for (Map.Entry<String, List<P4StreamProjectKey>> entry : groupByItemName(keys).entrySet()) {
			if (entry.getValue().size() > 1) {
				collisions.put(entry.getKey(), entry.getValue());
			}
		}
		return collisions;
	}
}
