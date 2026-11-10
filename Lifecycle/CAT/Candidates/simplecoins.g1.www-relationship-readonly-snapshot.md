---
kind: cat-code-to-spec-candidate
status: candidate
flow: code-to-spec
stage: reverse
work_id: task007.www.g1.reverse
task_id: TASK-20261108-007
owner: queue-d
source_repository: surumeneco/WhatAWonderfulWolf
source_ref: 442ae2cdece202dcbaeb78c82c9df26800951925
source_file: src/main/java/co/surumene/www/persistence/WonderfulWolfStorageReader.java
source_git_blob: 37e35aff978686183056bbc090b54e9b44e20ac9
related_api_repository: surumeneco/WonderfulCoreLibrary
normative_authority: unconfirmed
process_closure: not-run
model_conformance: not-run
---

# WWW G1 — source-derived relationship and storage snapshot contract

This is a **CAT code-to-spec Candidate**, not an approved Process, PI, TCE, or claim that G1 works end-to-end. Its evidence is the exact WWW `develop` source revision above, independently read from GitHub on 2026-11-10. It addresses the **WWW consumer/supplier boundary**, distinct from the WWC-only provenance Candidate in PR #99. Changing the behavior requires the appropriate normative decision and CAT Gates.

## Observation (code-backed; not a policy decision)

| ID | Source entry point | Directly observed behavior |
| --- | --- | --- |
| WWW-G1-O01 | `providerId` | Returns `www`. `readLoaded` and `readStored` return a `StorageReadResult`, not a monetary amount. |
| WWW-G1-O02 | `readLoaded(UUID)` | Rejects null input; disabled plugin produces `Failure(DISABLED)`; absent loaded Wolf produces `NotFound`; Wolf present without `WonderfulWolfIndividual` produces `Failure(MISSING_DATA)`. A present pair supplies the world's UUID, entity UUID, finite position, optional owner/commander, weapon and indexed inventory to `snapshot`. |
| WWW-G1-O03 | `readStored(UUID,UUID,Optional<Position>,PersistentDataContainer)` | Does not load a chunk in this method. Non-WWW type marker produces `NotFound`; missing schema or missing/empty runtime bytes produces `Failure(MISSING_DATA)`; unknown schema produces `Failure(UNSUPPORTED_SCHEMA)`. For external schema, absent `genome_ref` is `MISSING_DATA`, and its UUID is parsed before decoding runtime. |
| WWW-G1-O04 | `readStored` decoding | The runtime codec decodes owner, commander, weapon and inventory. Exceptions raised inside the stored PDC/codec/snapshot try block become `Failure(CORRUPT_DATA)`, including malformed external genome UUID or serialized item errors. |
| WWW-G1-O05 | `snapshot` | Adds optional UUID relation keys **`owner`** and **`commander`** independently; it does **not** choose between them. Weapon slot is `weapon`; inventory entries are sorted by numeric key then exposed as `cargo:<slot>`. Returns `Success(StorageSnapshot)` only after all `StoredItem` objects are built. |
| WWW-G1-O06 | `addItem` | Reads serialized bytes, invokes `ItemStack.deserializeBytes`, rejects null/empty deserialization and then constructs `StoredItem`. Corrupt items do not silently become an empty successful inventory. |

### WCL API observations (source-verified separately)

- `StorageReadResult` has **three** variants: `Success(StorageSnapshot)`, `NotFound`, and `Failure(code, detail)`. Its type comment explicitly warns that a missing individual is not an empty inventory.
- `StorageSnapshot` copies relationships with `Map.copyOf` and stored item list with `List.copyOf`, rejects duplicate slot IDs and rejects non-finite positions. Its comment requires callers to traverse nested containers rather than the provider flattening their contents.
- `StoredItem` defensively clones the serialized bytes in its constructor and accessor; it does not parse or value nested wallets. These statements follow WCL API source, **not** an executed integration test.

## Candidate obligations for SimpleCoins G1 (requires decision and tests)

1. A consuming adapter should distinguish a valid empty `Success` from `NotFound` and each `Failure` code; an absent/unreadable G1 provider must never be interpreted as **0 currency** or an unconditionally free asset.
2. Evaluate `commander` and `owner` **without erasing either relation**. Whether `commander` takes precedence over `owner` in P/F/D classification must be mapped to the authoritative SimpleCoins 02A user decision and Confirmed CAT rule, not inferred from a Map iteration order. **No normative priority is asserted here.**
3. De-duplicate entity UUIDs when loaded and stored snapshots describe the same individual; the reader itself exposes a UUID but does not perform cross-source aggregation.
4. Let SimpleCoins's separate inventory traversal count nested wallet/coin payloads exactly once, respecting stored slot labels and serialized PDC. The provider exposes serialized inventory bytes, not a financial total or nested traversal guarantee.
5. Verify loaded/stored parity, malformed UUID, missing/unsupported schema, duplicate slot, missing runtime, corrupt ItemStack bytes, disabled provider and unknown source without value-loss or false success. These are **test candidates**, not passing assertions.

## Evidence provenance

| Object | Immutable source or blob |
| --- | --- |
| WWW `develop` | `442ae2cdece202dcbaeb78c82c9df26800951925` |
| `WonderfulWolfStorageReader.java` | `37e35aff978686183056bbc090b54e9b44e20ac9` |
| WCL `StorageReadResult.java` | `409d4654cc1cd0b64945863a1daa214c62faceb5` |
| WCL `StorageSnapshot.java` | `94b2baaecef32c62828a9cfa934582ffb8beeab2` |
| WCL `StoredItem.java` | `2ec6b42ab8ccbde46fe5eb4b44e62abfa9fd4a58` |
| Work | `TASK-20261108-007`, original Drive file `1Jwy1tnOrs92uWTHP6MD9QhMKOVGigdur` |

## Unresolved / explicitly not verified

- Authority for the SimpleCoins 02A commander→owner→free policy as a **Confirmed CAT** Process/PI/TCE is not evidenced by this reverse-specification.
- This source reading does **not** prove the loaded-individual registry's completeness, offline PDC migration, actual WCL ServicesManager registration, WWC integration, nested wallet valuation, Paper 26.2 runtime behavior, JDK25 JUnit results or live deployment.
- `readLoaded` invokes the loaded entity/location access **before** its try/catch; an exception in that pre-try code is not normalized to `CORRUPT_DATA` by this method. A consumer error boundary needs verification, rather than an unsupported blanket claim that all exceptions are mapped.
- The WCL API's defensive snapshot semantics do not, by themselves, prove concurrent PDC updates cannot race with enumeration or that every serialized type is supported.

No user approval, Confirmed promotion, Process Closure or Model Conformance is implied by publishing this Candidate.