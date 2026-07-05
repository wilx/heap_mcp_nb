# Implementation Gaps

This file records gaps found while comparing the documented MCP tools with their
current implementations. The BM25 pagination cache-key issue is not listed
because it was fixed in commit `9389f7e`.

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
