---
cat_version: '0.2'
kind: work
id: www.trap.review
status: draft
process: www.trap
source_refs:
  - https://drive.google.com/file/d/1rjCFOOnkObvx-FNXDDkjhgbChCk451L6/view
  - user-chat:2026-11-14/trap-waiting-mob-request
refs:
  - www.trap
  - www.trap.pi.event
  - www.trap.tce.lifecycle
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: user-chat:2026-11-14/trap-waiting-mob-request
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: draft
---

# Work: www.trap.review

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/WhatAWonderfulWolf/commit/af3a8080992c0f26fce60ee76b2e12f91bbd41ba |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| www.trap | www.trap.tce.lifecycle | www.trap.pi.event |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| surumeneco/WhatAWonderfulWolf | ../../.. | develop | feature/* | src/main/java/**;src/test/java/**;docs/cat/** | src/main/java/** | src/test/java/** |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @work/Process.md | @work/PI.md | @work/TCE.md | — | — | — | @work/TestModel.json | @work/vectors.json | @work/TDD/Queue.json | — | @work/generated/current.test.java | @work/obligations.json | — | true |
