# Avoid unused mandatory IntelliLang dependency

## Status

Accepted

## Context

The plugin descriptor declared a mandatory dependency on `org.intellij.intelliLang`. Some target IDE installations report that plugin ID as unknown, preventing Cucumber+ from loading.

The production code does not use IntelliLang-specific APIs or extension points. Requiring the plugin at runtime therefore narrows compatibility without providing behavior.

## Decision

Remove the mandatory IntelliLang dependency from the plugin descriptor and from Gradle IntelliJ sandbox plugin lists.

Keep only dependencies that are required by runtime extensions or compiled production code.

## Consequences

Cucumber+ can load in IDE distributions where IntelliLang is not available as a separately resolvable plugin.

If future work adds IntelliLang-specific integration, it must be declared explicitly, preferably as an optional dependency with a dedicated config file unless the feature is required for core plugin startup.
