# WWW 1.0.1 — 残存する定期処理の比較・採択記録（2026-10-17）

基点: `develop` `1aff6bbc1a70fb6421a2a0ced03974f83f57deca`。設計正本: Google Drive `OriginalPlugins/共通基盤/WWC・WWW_必要時更新・相互軽量化計画.md`（2026-10-17改訂）。

## 判定規則

- A = 現行周期処理、B = 変更通知／イベント＋単発期限、C = 活動中の個体・現象のみ周期確認。
- **機能同等性が必須**。外部プラグインの無通知操作を捕捉できない場合は、イベントが存在しても従来監査を無断削除しない。
- 優劣は Paper/Java/設定/World/Entity/プレイヤー条件を揃えて、CPU time、処理回数、イベント配送、allocations、MSPT p95/p99、最大遅延を比較して判断する。
- 本記録の「対象検査数」はアルゴリズム上の演算件数であり **CPU時間、TPS改善率、Paperイベント配送コストの実測値ではない**。

## 残存経路の棚卸し・方式採否

| 定期処理 | 必要な目的 | 代替・比較 | 現段階の採択 |
| --- | --- | --- | --- |
| Relationship: 5tick ごとの有資格狼一覧と受動好感度抽選 | FOLLOW/GUARD＋commander時、所定間隔で抽選。モード変更・アンロードで停止 | A/C: 全有資格個体を毎5tick検査。B: 登録・モード・commander変更時dirty＋個体ごと次回抽選期限 | **B を実装**。5tickの基準時計は維持し、期限到来IDだけを調べる。抽選回数・初回開始時刻を維持 |
| Relationship: PendingFeed 5tick cleanup | 1tick以内の回復イベントと右クリックを対応付ける | B: 消費時時刻チェック＋単発削除。ただし個別タスク生成・キャンセル費用が増える | **A維持**。既存 `consume` に経過tick検証あり。小さなMapの掃除だけを単独タスク化する改善根拠なし |
| Weapon: 毎tickの戦闘追尾／攻撃 | 標的移動、Path消失、射程到達、攻撃可能時刻の変化を反映 | B: 攻撃期限予約、EntityTarget通知。C: 現行active combatのみ毎tick | **C維持**。経路・相手位置変化に無通知経路があり、1tickの反映遅延維持が必要 |
| Weapon: 600tick装備監査 | 他plugin等が保存/GUIを経由せず装備変更した場合の修復 | B: `EntityEquipmentChangedEvent`＋内部変更通知。監査撤去には他plugin API書込・復元経路の通知網羅性実証が必要 | **Aの低頻度監査維持**。未実証の外部変更互換を削除しない |
| Ability: 毎tick dirty/生物時刻期限dispatcher | 変更がある場合のみ最大16件／tickで能力反映、生物時刻の巻戻しも捕捉 | B: 個別タスク予約。C: 現行の空時即return／due最小値チェック | **現行維持**。多数の個別タスク＋取消より軽いとのCPU証拠なし |
| Ability: 幼体200tick / 6000tick全登録監査 | `isAdult`変化と外部Attribute編集の再反映 | B: 成長・装備・Attributeイベント、成年期限。C: 幼体だけ周期＋全登録低頻度互換監査 | **C＋低頻度監査維持**。外部時刻操作・成長変更・無通知Attribute編集の通知網羅性未検証 |
| Behavior: 5tick activeモード／戦闘・退避個体 | vanilla Goalの中断、標的・owner移動、追従・帰還・回避の再評価 | B: イベント/MobGoal。C: 既存active集合＋例外的reactive状態 | **C維持**。MobGoal内部でもtickが生じ、挙動優先順位とCPU比較が未実証 |
| Trait: 5tick active trait（Potion/Aura/Watchman/Scavenger） | 範囲内Player移動、敵侵入、移動Item回収、継続効果を捕捉 | B: Move/Potion/Item各イベントと期限。C: 既存trait集合のみ局所検索 | **C維持**。PlayerMove・LivingEntity移動イベント配送とItem無通知移動の比較／漏れ検証が必要 |
| Triggered effect: 5tick `holyPulses` Map | 発動中の対象に20tickごとの効果、死・期限で解除 | B: due単発予約。C: 発動中Mapだけ5tick走査 | **C維持**。Map空時の低費用。高同時発動数とタスク生成費の比較が必要 |
| WolfTrap: 毎tickワールドfullTime監視 | `reachedAdditionalCheckTime` の「自然な +1」のみ抽選。睡眠・時間飛越は抽選しない | B: ClockTimeSkip/TimeSkipと単発期限。A: 現行 per-world 1tick確認 | **A維持**。あらゆる`setFullTime`と昼夜停止・巻戻しをイベントで観測できる保証なし |
| WolfTrap: armed/rider個体のみ毎tick | 発動距離・grace経過・乗客/狼ペア・標的変化 | B: プレイヤーMove/Teleport/Target/Mountイベント。C: 現行armed/rider集合のみ | **C維持**。騎乗AIの無通知ターゲット移動／水流等の互換検証未了 |
| Founder: 毎tickの小さな待機キュー | 同期スレッドで有限workerへ仕事を流し、満杯時再試行 | B: enqueue/worker完了でwake-up。A: 空キュー即return | **A維持**。wake-up生成・キャンセル／worker競合込みのCPU比較が未実施 |

本表の維持は「永遠に周期方式しかない」の意味ではない。**同等性かCPU優位を確認できない限り置換しない**という現在の採否である。未検証の項目を「低負荷確定」と呼ばない。

## 好感度: A/C と B の比較可能な数量

想定: モード／commander不変、受動増加間隔1200tick、観測tick 5,10,...,2400（5tick間隔で計480回）、全員有資格。

| 有資格狼数 | A/C の有資格狼判定数 | B の初回＋期限到来の狼判定数 |
| ---: | ---: | ---: |
| 0 | 0 | 0 |
| 50 | 24,000 | 100 |
| 200 | 96,000 | 400 |
| 1000 | 480,000 | 2,000 |

Bは **5tick毎の空キュー先頭確認480回** と、加入／変更ごとの索引管理コストを追加で負う。表はその費用もCPU時間も含まない。動作同等性では初回tickに状態を記録し、1200tick経過時だけ1回乱数を引き、次回期限へ送る。抽選失敗時も期限更新し、二重抽選を禁止する。設定reloadはアクティブ個体だけ再評価し、変更済みの周期でも過去の抽選時刻を保持する。UUID同一別Entityは期限を初期化し、古いEntity参照を保持しない。

**採択**: B。不要な全件反復を構造的に取り除け、旧アルゴリズムの抽選時刻・確率・条件を同じ単位テストで拘束できるため。ただし **PaperでのCPU／MSPT優位は未確認**。個別キューのメモリ・割当・イベント発火費が高いケースが実測された場合は再評価する。既存5tickタイマーを維持することで新規のSchedulerジョブ生成を抑える。

## 2026-10-17 owner-approved follow-through (WWW 1.0.1)

The 15 owner decisions in Google Drive supersede the historical candidate matrix above. This implementation intentionally retains WWC-style monitoring where a generic Paper event cannot guarantee equivalent behavior.

| Decision numbers | Implemented behavior / decision |
| --- | --- |
| ① | Passive-affection deadline queue from PR #56 remains selected; no change here |
| ② | Remove global 600-tick equipment force-restore and post-attack restore. Read the live wolf weapon via the normal Paper attack path. Explicit WWW saved-weapon/GUI mutations still synchronize equipment |
| ③, ⑥ | Existing Wolf MobGoals adapter, Pathfinder navigation and active combat/mode-only reconciliation retained. Repath only for changed target, significant movement or lost path; maintain custom modes and target precedence instead of blindly restoring vanilla goals |
| ④ | Replace empty 1-tick Ability ticker with one bounded delayed wakeup based on dirty work, biological-time deadlines, juvenile 200-tick check and compatibility 6000-tick audit. Dirty mutation can rearm an earlier wakeup |
| ⑤ | Preserve incrementally indexed juveniles and low-frequency full-registered external Attribute compatibility check; no event-only invalidation of unknown external writes |
| ⑦, ⑧, ⑨, ⑪ | Run periodic trait/Aura/watch/scavenge dispatcher only if a periodic trait source exists or HolyPulse has in-flight work. No global entity-move listeners; maintain active-only local range checks and external-effect priority. Clear inactive watch state |
| ⑩ | Explicitly reject event-only Scavenger replacement; preserve existing localized periodic scan |
| ⑫, ⑬ | Retain current per-world clock tick and armed/rider-only active trap update, matching WWC's strict natural clock progression and active physical interaction contract |
| ⑭ | On recording a feed, arm a single earliest-expiration wakeup. Consume retains same/next-tick validation; no unrelated relationship 5-tick expiration scan |
| ⑮ | Dispatch bounded Founder submissions on enqueue and worker-result callbacks; remove idle global 1-tick task. Retain capacity and main-thread commit contract |

### Review and acceptance

Automated Java 25 checks verify the code and regression suite but do not establish real-server CPU or behavior equivalence. Required integration checks include equipment changed externally before attacks, 0/1/1000 wolves for ability/trait load, biological-time rollback, late Attribute changes, same-UUID entity replacement, feed event ordering, HolyPulse without any periodic source, multiple concurrent Founder jobs, queue saturation, plugin shutdown, and Geyser/ClickMobs/Multiverse behavior. Use matched CPU/MSPT p95/p99 measurements before claiming production optimization. Keep `main` and WGL/WWC unchanged.

## 検証・未完了

- 自動テスト: `PassiveAffectionDeadlineQueueTest`, `PassiveAffectionScheduleTest`, `WonderfulWolfLoadedIndividualsTest`。空候補、期限直前／期限到来、重複／変更による予定取消し、1000件の候補限定、commander変更、UUID同一別Entity・アンロードを確認。
- 同等性の未完了: 実Paper/外部プラグインでのイベント配送、Geyser、ClickMobs、Multiverse、sleep/`/time`操作、装備・Path・Aura、実PDC復元（全シナリオ）。
- 性能の未完了: プロセスCPU time、同一ワールドのA/B比較、0/50/200/1000個体・active比率・プレイヤー移動別のイベント入場数／allocations／MSPT p50/p95/p99。
- CIが成功しても実機機能・実負荷の保証とはしない。`develop`への反映を`main`へのリリースと混同しない。WWC・WGLは対象外。

