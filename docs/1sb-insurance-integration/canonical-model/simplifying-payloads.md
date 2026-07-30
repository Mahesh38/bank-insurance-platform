# Simplifying 1SB requests/responses into contexts

1SB payloads are large because they support many insurers and LOBs in one schema. For the bank platform, **split by context** and keep each API small.

## Context → payload slices

| Context | Include in bank API | Leave to adapter |
|---------|--------------------|------------------|
| Distribution | rmId, channel=B2B | distributorID, agentID, agentType, salesChannel, varFields |
| Party | CIF-based member profile | 1SB memberType enums, sequence numbers |
| Quote intent | mode, category, amount, frequency prefs | typeOfQuote strings, includeBI, outOfBoundConfig, alternateFreq |
| Catalog pin | optional insurer/product filters | insuranceAndProducts[] packing |
| Proposal | schemaId + values map | Full dynamic tree POST |
| Payment | amount, redirect route, journeyId | Payment URL envelope |
| Status | journeyId | insurer codes + applicationNo lookup mix |

## Response simplification

| 1SB | Bank |
|-----|------|
| Poll wrapper + nested quote products | `QuoteJob { status, offers[], failures[] }` |
| Dynamic proposal schema | `FormSchema` already normalized by adapter |
| Status manufacturer[] product[] | `JourneyStatus { stage, substatus, policy?, premium? }` |
| errors[] polymorphic | `Problem { code, message, retryable, insurer? }` |

## Why this helps replacement

When the bank middleware arrives, it only needs to satisfy the same small contexts. Insurer-specific verbosity stays behind adapters—whether the adapter talks to 1SB or to HDFC/ICICI directly.
