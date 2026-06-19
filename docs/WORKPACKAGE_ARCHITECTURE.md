# Workpackage Architecture

This document is the canonical workpackage for Perforce workpackage discovery in Jenkins. It keeps one copy of each numbered section, sections 1 through 26, followed by one set of link reference definitions.

## 1. Goal

Discover a Perforce workpackage hierarchy from depot metadata and present it in Jenkins as nested folders containing generated Pipeline jobs.

## 2. Repository anchors

* Existing multibranch source package: `src/main/java/org/jenkinsci/plugins/p4/scm/`
* Existing stream source class: `org.jenkinsci.plugins.p4.scm.StreamsScmSource`
* Existing dependency declarations: `pom.xml`
* Jenkins folder lifecycle API: [`ComputedFolder`][computed-folder]

## 3. Problem statement

The plugin already supports Perforce-backed multibranch behavior, but workpackages need a hierarchy-first item model. Users should be able to configure one Jenkins parent item and let indexing materialize visible Jenkins folders and Pipeline jobs that mirror the buildable workpackage structure.

## 4. Product requirements

* Provide one configured Jenkins parent item for workpackage discovery.
* Create nested Jenkins folders for visible workpackage levels.
* Create generated Pipeline jobs at buildable leaf workpackages.
* Reuse existing p4-plugin connection, credentials, filtering, and SCM metadata concepts.
* Keep existing multibranch configuration semantics unchanged.

## 5. Discovery scope

Discovery should use the same Perforce connection and filtering concepts already used by the multibranch source package. The initial MVP should prefer stream-backed discovery when workpackages map naturally to streams, with `StreamsScmSource` as the repository anchor for source probing behavior.

The depot can provide stream-level information cheaply as part of stream enumeration, so the MVP should ask the depot for that metadata and directly use it as the authoritative hierarchy input.

## 6. Architecture

Nested Jenkins folders are mandatory for the MVP. The primary target is therefore a new parent item type, `P4DepotComputedFolder`, implemented as `P4DepotComputedFolder extends ComputedFolder`.

`P4DepotComputedFolder` owns workpackage indexing at the depot/workpackage root and materializes the discovered hierarchy as Jenkins items under itself. The generated model is:

* `P4DepotComputedFolder` as the top-level Jenkins parent item configured by the user.
* Nested computed or managed folder items for each discovered Perforce workpackage folder level that must appear as a Jenkins folder.
* Generated Pipeline child jobs at leaf workpackage nodes, reusing the existing p4-plugin SCM source concepts from `src/main/java/org/jenkinsci/plugins/p4/scm/` and the stream discovery behavior represented by `org.jenkinsci.plugins.p4.scm.StreamsScmSource`.
* Stream-level metadata queried directly from the depot during discovery and used immediately to place each stream-backed workpackage at the correct Jenkins folder depth.

An `SCMNavigator` implementation is not the MVP target because navigators are best suited to discovering sibling multibranch projects under a flat organization folder model. It remains a possible future or alternate implementation if the product requirement changes to a flat repository/project listing rather than mandatory nested Jenkins folders.

## 7. Parent item type and child item model

All MVP sections should refer to `P4DepotComputedFolder extends ComputedFolder` as the configured parent item type. The parent folder is responsible for reconciling Jenkins children during indexing.

Generated children must use one consistent model:

1. Folder children represent nested workpackage path segments that are required to be visible as Jenkins folders.
2. Leaf children are generated Pipeline jobs for buildable workpackages.
3. Each generated Pipeline job is configured with the Perforce SCM metadata needed to check out and probe the same workpackage revision space as the existing multibranch stream source.
4. For stream-backed workpackages, the generated folder depth and parent-child placement come from the depot-provided stream level and parent metadata captured during discovery.

The MVP must not describe a separate `SCMNavigator` parent as the primary path. `SCMNavigator` is reserved for a future flat organization-folder variant.

## 8. Path mapping

Path mapping converts depot-provided workpackage identity into stable Jenkins item locations. The mapping should preserve three values for every discovered node:

* The original Perforce depot path or stream path.
* The display path shown to users.
* The Jenkins-safe item name used for folder and job identity.

Generated folder and job names should be deterministic, stable across re-indexes, and safe for Jenkins item names. Perforce depot paths should be retained in metadata so display names can remain readable while internal names stay stable.

## 9. Indexing lifecycle

The indexing lifecycle is the Jenkins `ComputedFolder` lifecycle owned by `P4DepotComputedFolder`:

1. A user creates or reconfigures a `P4DepotComputedFolder`.
2. Jenkins schedules the folder computation.
3. `P4DepotComputedFolder` connects to Perforce using the configured credentials and include paths.
4. The computation enumerates workpackage folders and buildable leaf workpackages, asking the depot for stream-level metadata when streams are the backing model.
5. The folder normalizes the depot-provided stream information into the workpackage tree, using stream level/depth and parent metadata directly to decide which Jenkins folder owns each generated child.
6. The folder reconciles its generated children: create missing nested folders and Pipeline jobs, update existing generated children whose metadata changed, and orphan or remove generated children no longer present according to the configured orphan strategy.
7. Generated Pipeline jobs then build through normal Pipeline scheduling and checkout behavior.

Event handling should schedule recomputation of the owning `P4DepotComputedFolder`. It should not require an `SCMNavigator` event path for the MVP.

## 10. Configuration model

The MVP configuration belongs to `P4DepotComputedFolder` and should include credentials, depot or stream include paths, optional filters, script path defaults, and naming rules for generated folders and jobs.

## 11. Required Jenkins plugin dependencies

The MVP dependency set is the dependency set required by `P4DepotComputedFolder extends ComputedFolder`, nested folder item generation, and generated Pipeline jobs:

* `cloudbees-folder` for `ComputedFolder` and nested Jenkins folder support.
* `workflow-multibranch` and existing Pipeline dependencies for generated Pipeline job behavior already used by the plugin.
* `scm-api` for continued reuse of the p4-plugin SCM source abstractions in `src/main/java/org/jenkinsci/plugins/p4/scm/`.
* Existing p4-plugin dependencies declared in `pom.xml`, including credentials and Pipeline SCM step dependencies.

If `cloudbees-folder` is currently only transitive through another dependency, the implementation work should add or verify an explicit dependency declaration in `pom.xml` before compiling the MVP. Do not add a new primary `SCMNavigator` dependency path for the MVP architecture; keep navigator-specific dependencies and descriptors limited to the future flat variant if that variant is implemented.

## 12. Security and credentials

The folder configuration should use Jenkins credentials APIs consistently with existing p4-plugin SCM sources. Credentials should be scoped and looked up from the folder context where possible.

## 13. UI behavior

The MVP UI should expose one new item type for `P4DepotComputedFolder`. Configuration should make it clear that indexing will create nested Jenkins folders and Pipeline jobs beneath that parent.

## 14. Backward compatibility

Existing multibranch sources, including `StreamsScmSource`, remain supported. The MVP adds a new folder-based parent item and should not change existing multibranch job configuration semantics.

## 15. Migration

No automatic migration is required for the MVP. Existing multibranch jobs can coexist with `P4DepotComputedFolder` items.

## 16. Observability

Folder indexing logs should report the Perforce include paths scanned, the stream-level metadata source used for hierarchy placement, the number of workpackage folders discovered, the number of generated Pipeline jobs reconciled, and orphan handling results.

## 17. Error handling

Indexing failures should fail the folder computation with actionable messages while preserving existing generated children until a later successful recomputation or orphan strategy action.

## 18. Testing strategy

Tests should cover folder computation, nested child reconciliation, generated Pipeline job metadata, credential lookup from folder context, orphan handling, path mapping, and event-triggered recomputation of `P4DepotComputedFolder`.

## 19. Performance

The indexer should avoid full depot scans when include paths narrow the search space. It should prefer batched stream enumeration that returns stream-level metadata in the same discovery pass, then cache or batch any additional Perforce queries where Jenkins lifecycle constraints allow.

## 20. Extensibility

The folder implementation should keep Perforce discovery logic separate from Jenkins item reconciliation so future work can add non-stream workpackage discovery without changing the parent item type.

## 21. Implementation phases

1. Add the `P4DepotComputedFolder` item type, descriptor, and minimal configuration persistence.
2. Add Perforce discovery services that return normalized workpackage nodes with depot identity, stream metadata, and path mapping fields.
3. Add child reconciliation for nested folders and generated Pipeline jobs.
4. Wire generated jobs to existing p4-plugin SCM metadata and script-path behavior.
5. Add event handling that schedules recomputation of the owning folder.
6. Add tests for discovery, mapping, reconciliation, credentials, and orphan handling.
7. Verify dependency declarations and user-facing documentation.

## 22. Non-goals

The MVP does not implement a flat organization-folder browser, automatic migration from existing multibranch jobs, or a separate `SCMNavigator` primary architecture.

## 23. Future or alternate flat implementation

A future or alternate implementation may use `SCMNavigator` when nested Jenkins folders are not mandatory. In that model, a navigator could discover workpackages as sibling projects under an organization folder or similar flat parent and could reuse source classes from `src/main/java/org/jenkinsci/plugins/p4/scm/`, including concepts from `StreamsScmSource`.

That flat implementation is explicitly not the MVP architecture. It should be documented and estimated separately because its parent item type, generated child model, event matching, and dependency surface differ from `P4DepotComputedFolder extends ComputedFolder`.

## 24. Decision summary

The architecture decision is to implement `P4DepotComputedFolder extends ComputedFolder` as the single primary parent item type. It generates nested Jenkins folder items for workpackage levels, uses depot-provided stream level and parent metadata when streams back the workpackages, and generates Pipeline jobs for buildable leaf workpackages.

## 25. Cross-reference checklist

The canonical section numbers are:

* Architecture: [section 6](#6-architecture)
* Path mapping: [section 8](#8-path-mapping)
* Implementation phases: [section 21](#21-implementation-phases)
* MVP cut line: [section 26](#26-mvp-cut-line)

## 26. MVP cut line

The MVP cut line includes the configured `P4DepotComputedFolder`, stream-backed workpackage discovery, deterministic path mapping, nested folder and generated Pipeline job reconciliation, folder-scoped credentials, orphan handling, indexing logs, and tests for the primary folder lifecycle.

The MVP cut line excludes the flat `SCMNavigator` architecture, automatic migration from existing multibranch jobs, non-stream discovery variants, and advanced UI workflows beyond the new folder configuration and generated child presentation.

[computed-folder]: https://javadoc.jenkins.io/plugin/cloudbees-folder/com/cloudbees/hudson/plugins/folder/computed/ComputedFolder.html
