# WWW 1.0.1: user-approved periodic work implementation (2026-10-17)

Authority: `forGPT/XPlayServer/OriginalPlugins/共通基盤/WWC・WWW_必要時更新・相互軽量化計画.md`, section "最新ユーザー判断". This document describes the exact code behavior rather than claiming measured TPS/MSPT improvements.

| # | Final implementation / decision | Validation boundary |
| --- | --- | --- |
| ① passive affection | PR #56 already merged: due UUID queue and dirty state | A/B branches remain available for live CPU comparison |
| ② weapon audit | Remove unconditional 600tick all-Wolf re-equipping, plus post-attack forced re-equip. `wolf.attack(target)` uses its currently equipped main-hand item; explicit WWW GUI/config/restore writes still project WWW saved weapon once | External equipment is no longer silently overwritten at periodic audits. Validate GUI updates, third-party changes and item damage/attributes live |
| ③ combat | Preserve active-combat-only every-tick attack/range check and Paper `Wolf.getPathfinder()` navigation; `NavigationRepathPolicy` only replans when path/target/movement changes. Vanilla melee goal stays removed to prevent duplicate independent damage | The existing normal-Mob-style pathfinder and controlled attack cooldowns are already the chosen architecture; do not add an unconditional vanilla melee goal alongside WWW attacks. Validate aggression, hit count, attack speed, rider and target loss |
| ④ ability idle | Replace global every-tick scheduler with a single scheduled wakeup on mutation, due time or capped biological clock checkpoint | At most 16 dirty projections per execution; backlog wakes again in 1tick; max200tick checkpoint for clock rollback |
| ⑤ juvenile/compatibility | Keep indexed juvenile 200tick detection; compatibility review at 6000tick for unsignaled external Attribute changes; emit dirty on changed individual | Do not delete all external-attribute audit guarantees: Paper offers no universal attribute-change notification |
| ⑥ behavior | Keep existing Paper MobGoals preparation, active-mode only 5tick reconciliation, reactive WANDER exceptions, `Wolf.getPathfinder()`; do not stack vanilla FollowOwner/MeleeAttack on top of WWW's own target and combat controller | Compare against conventional vanilla pathfinder behavior, path interruption, current target, owner movement, and same-UUID entity swap in live test |
| ⑦ trait scheduler | Schedule 5tick loop only while `tickingTraits` UUID membership nonempty or HolyPulse is in flight. Registry notifies on empty/nonempty transitions | No trait source means no Bukkit timer. Per-source STATUS/WATCH/SCAVENGE selectors are unchanged |
| ⑧ Aura / STATUS | Reuse ⑦ active-source timer and existing local, indexed effect search | Existing strong potion effects retain priority |
| ⑨ Watchman | Reuse ⑦ and existing indexed nearby search, purge stale watch-state on final source disappearance | Never install all-Mob movement listeners; first detection cadence remains 5tick |
| ⑩ Scavenger | **Rejected alternative**: retain existing indexed local periodic search | Detect items moved only by water; do not switch to ItemSpawn-only events |
| ⑪ HolyPulse | Reuse ⑦ shared active timer even when no periodic trait sources remain; end effect after recorded expiry; preserve 20tick interval | Instant effect tick/range/stacking rules unchanged |
| ⑫ WolfTrap clock | **Unchanged**, natural +1 world time polling | Sleep, setTime and rollback must not introduce artificial rolls |
| ⑬ WolfTrap armed/rider | **Unchanged**, armed/active entity only tick | Preserve target and passenger AI handling |
| ⑭ PendingFeed | Each feed schedules only the earliest 2tick cleanup; consumption still requires same/next server tick. Pending newer feeds reschedule after previous deadline fires | Callback via Bukkit main-thread scheduler; cancellation by plugin disable; no ongoing empty 5tick cleanup |
| ⑮ Founder | Main-thread bounded queue drained on accepted enqueue and worker-completion callback. No global 1tick idle polling | Executor queue 16 and backlog max128 unchanged; rejected item returns to front, main-thread Bukkit commit unchanged |

## Automated regression and limits

- TDD scenario classes: `PendingFeedWakeupTest`, `FounderDispatchQueueTest`, `AbilityWakeupPolicyTest`, `TraitTaskActivityPolicyTest`.
- Existing full Java25 Gradle tests and JAR metadata checks run in GitHub Actions; failures or incomplete CI block develop integration.
- This work does not require any WGL/WWC source change or Minecraft config change; `version=1.0.1` stays fixed.
- **Live tests remain** for JAR deployment, Geyser/ClickMobs/Multiverse, external equip edits, player/target motion, effects, clock rollback, WorldTrap and comparing total server CPU / MSPT p95/p99. No CPU benefit percentage is asserted.
- Full live A/B/C comparison can use the original pre-PR #56 A/B pair only for that isolated passive-affection experiment; do not compare it with this integrated revision and attribute results to a single subsystem.

## Review notes

- Founder main-thread mutation only, bounded work, completion callback always drains in a `finally` after the commit.
- Deadline scheduler holds UUIDs and numerical times, never an Entity that could survive unload.
- Per-source Bukkit timers are bounded to one ability wakeup and one 5tick trait loop, and both cancel on plugin disable.
- Do not automatically merge WWW into `main` or the production release PR before live verification.
