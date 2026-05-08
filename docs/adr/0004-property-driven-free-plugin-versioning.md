# Property-driven free plugin versioning

## Status

Accepted

## Context

JirakJ JetBrains plugin repositories keep plugin metadata such as group, name, repository URL and version in `gradle.properties`. This makes release version bumps explicit and avoids hard-coded version strings inside build scripts.

Cucumber+ also needs to remain a free Marketplace plugin.

## Decision

Move the Cucumber+ plugin version to `pluginVersion` in `gradle.properties` and have the root Gradle build assign `project.version` from that property.

Keep the plugin descriptor free by not declaring a Marketplace `product-descriptor` and by documenting that policy in the build and plugin descriptor.

## Consequences

Future releases should bump `pluginVersion` in one place. The Gradle IntelliJ plugin will use `project.version` when patching plugin metadata.

The plugin remains free unless a future change intentionally adds Marketplace paid-product metadata.
