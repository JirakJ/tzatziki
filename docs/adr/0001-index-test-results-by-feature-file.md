# Index test results by feature file

## Status

Accepted

## Context

`TzTestsResultsAnnotator` runs from the IntelliJ highlighting daemon for open Gherkin `.feature` files. Once any test results existed in the project, the annotator needed to decide whether each visited PSI element belonged to a file with results.

Scanning all stored test results for every highlighted PSI element makes unrelated open feature files pay work proportional to the total project result set.

## Decision

Maintain a derived `filesWithResults` set in `TzTestRegistry`. The set is rebuilt when active test results are refreshed or cleaned, and `hasResults(file)` becomes an O(1) membership check used as an early annotator guard.

## Consequences

Open `.feature` files without test results return from `TzTestsResultsAnnotator` before step/row-specific work. Files with results keep the same visible annotations and quick fixes.

The registry now has one additional derived index that must stay synchronized whenever `activeResults.tests` changes.
