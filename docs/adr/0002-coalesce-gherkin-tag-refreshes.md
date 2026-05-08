# Coalesce Gherkin tag refreshes

## Status

Accepted

## Context

Open Gherkin `.feature` files trigger PSI tree change events while the user types. The tag service refresh path scans project feature files to rebuild tag metadata for the Cucumber+ tool window and tag completion.

Scheduling that refresh immediately for every PSI change can queue repeated project-wide tag refreshes during normal editing bursts.

## Decision

Use IntelliJ's `MergingUpdateQueue` inside `PsiChangeListener` to merge Gherkin tag refresh requests over a short window before running the existing smart-mode and committed-document refresh flow.

The queue is parented to the project disposal service so pending work is cancelled with the project lifecycle.

## Consequences

Typing in open `.feature` files no longer schedules one project tag refresh per PSI event. Tag-dependent UI and completion still refresh after documents are committed and indices are ready.

The refresh remains asynchronous and eventually consistent, which matches the previous deferred `smartInvokeLater` / `performLaterWhenAllCommitted` behavior.
