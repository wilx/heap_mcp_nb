# Implementation Gaps

This file records gaps found while comparing the documented MCP tools with their
current implementations. The BM25 pagination cache-key issue is not listed
because it was fixed in commit `9389f7e`.

## 1. Generated MCP schemas omit useful parameter constraints

`ToolsFactory` currently emits only the JSON type and required status for each
parameter. Defaults from `@Default` are used during invocation but are absent
from the published schema. The schemas also omit descriptions, minimum values,
and allowed values such as the `get_duplicate_strings.sort_by` choices.

Completion criteria:

- Publish default values in each input schema.
- Support parameter descriptions.
- Publish numeric minimums for offsets, limits, and lengths.
- Publish enum values where the accepted set is closed.
- Add schema-level tests for required parameters, defaults, ranges, and enums.

## 2. Regexp class-search cache is never populated

`HeapDumpService.getJavaClassesByRegExpPaginated` reads from `classesByRegexp`
but does not put newly fetched results into it. Every cache miss therefore
remains a miss.

Completion criteria:

- Store the full regexp result list after fetching it from the heap.
- Keep the cache cleared when a new heap is loaded.
- Add a test proving repeated pages for the same expression query the heap only
  once.

## 3. GC-root tools are duplicate APIs

`get_gc_roots` and `get_gc_roots_paginated` have the same parameters, delegate
to the same service method, and use the same printer. Their descriptions differ,
but their behavior does not.

Completion criteria:

- Choose one canonical tool name.
- Either remove the duplicate in the next breaking release or retain it as a
  clearly documented compatibility alias.
- Ensure the README and compatibility tests reflect that decision.

## 4. Smaller output and metadata defects

### BM25 rank restarts on every page

`Bm25ResultListPrinter` always starts rank numbering at 1 and receives no page
offset. Later pages therefore display incorrect global ranks.

Completion criteria: carry the offset into the printable result or return a
page object that includes it, then test a non-zero `from`.

### Server version differs from the artifact version

`McpServerLauncher` advertises version `0.0.1`, while `pom.xml` and documented
artifact names use `0.0.3`.

Completion criteria: derive server metadata from the build version, or update
both values through one release process so they cannot drift.

### Coverage is weak around failure-prone heap operations

Some integration and diagnostic tests are disabled, including the biggest
objects integration test. Reflection schema tests currently verify only basic
types and required status.

Completion criteria:

- Replace avoidable disabled tests with deterministic unit tests using Mockito.
- Keep fixture-dependent integration tests separate and state why each disabled
  test cannot run in the normal suite.
- Add MCP-level tests for error flags and output fields, not only tool
  registration.
