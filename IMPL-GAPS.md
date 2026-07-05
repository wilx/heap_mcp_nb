# Implementation Gaps

This file records gaps found while comparing the documented MCP tools with their
current implementations. The BM25 pagination cache-key issue is not listed
because it was fixed in commit `9389f7e`.

## 1. GC-root tools are duplicate APIs

`get_gc_roots` and `get_gc_roots_paginated` have the same parameters, delegate
to the same service method, and use the same printer. Their descriptions differ,
but their behavior does not.

Completion criteria:

- Choose one canonical tool name.
- Either remove the duplicate in the next breaking release or retain it as a
  clearly documented compatibility alias.
- Ensure the README and compatibility tests reflect that decision.

## 2. Smaller output and metadata defects

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

## Follow-up: consider MCP Java SDK 2.x migration

This is not a correctness gap in the current implementation. MCP Java SDK 2.x
would make richer tool schemas more native because `Tool.inputSchema` is a
full JSON Schema `Map<String,Object>` and tool input validation is built into
the server by default.

Potential model-facing benefits:

- Publish richer schema keywords such as descriptions, defaults, minimums, and
  enums without squeezing them through the SDK 1.x `JsonSchema` record.
- Let the SDK validate incoming tool arguments against the published schema.
- Improve AI client argument selection and reduce invalid tool calls.

Migration notes:

- SDK 2.x has breaking API changes around tool schema construction, builders,
  validation, and server setup.
- The migration would not replace the need for good local tool and parameter
  metadata.
- Keep service-level validation even if MCP server-side validation is enabled,
  since CLI and direct service paths bypass MCP tool-call validation.
