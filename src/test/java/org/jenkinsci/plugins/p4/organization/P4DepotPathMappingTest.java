package org.jenkinsci.plugins.p4.organization;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P4DepotPathMappingTest {

	@Test
	void mapsNamedPathLevelsToFolderJobAndStream() {
		P4DepotPathMapping mapping = new P4DepotPathMapping("QGATE Streams", "//streams/{domain}/{project}/{stream}",
				"{domain}", "{project}", "{stream}", null, null, "^(main|dev|release-.*)$", "release-old.*", false, true);

		Optional<P4StreamProjectKey> key = mapping.match("//streams/qgate/api/main");

		assertTrue(key.isPresent());
		assertEquals("qgate", key.get().getFolderPath());
		assertEquals("api", key.get().getJobName());
		assertEquals("main", key.get().getStreamName());
		assertEquals("qgate/api", key.get().getItemPath());
		assertEquals("//streams/qgate/api/...", key.get().getStreamIncludePath());
		assertEquals("qgate", key.get().getVariables().get("domain"));
	}

	@Test
	void filtersStreamsAndProjectsByRegex() {
		P4DepotPathMapping mapping = new P4DepotPathMapping("QGATE Streams", "//streams/qgate/{project}/{stream}",
				"qgate", "{project}", "{stream}", "^(api|service)$", null, "^(main|dev|release-.*)$", "release-old.*", false, true);

		assertTrue(mapping.match("//streams/qgate/api/dev").isPresent());
		assertFalse(mapping.match("//streams/qgate/cli/dev").isPresent());
		assertFalse(mapping.match("//streams/qgate/api/experimental").isPresent());
		assertFalse(mapping.match("//streams/qgate/api/release-old-2019").isPresent());
	}

	@Test
	void validatesUnknownTemplateVariables() {
		assertThrows(IllegalArgumentException.class, () -> new P4DepotPathMapping("bad", "//streams/{project}/{stream}",
				"{team}", "{project}", "{stream}", null, null, null, null, false, true));
	}

	@Test
	void detectsGeneratedItemCollisions() {
		P4StreamProjectCollisionDetector detector = new P4StreamProjectCollisionDetector();
		List<P4StreamProjectKey> keys = Arrays.asList(
				new P4StreamProjectKey("one", "//streams/qgate/api/main", "qgate", "api", "main", "//streams/qgate/api/...", Map.of()),
				new P4StreamProjectKey("two", "//legacy/qgate/api/main", "qgate", "api", "main", "//legacy/qgate/api/...", Map.of()));

		assertTrue(detector.findCollisions(keys).containsKey("qgate/api"));
	}
}
