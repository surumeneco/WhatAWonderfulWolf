---
kind: process
id: www.trap
cat_version: '0.3'
status: draft
process: www.trap
source_refs:
  - https://drive.google.com/file/d/1rjCFOOnkObvx-FNXDDkjhgbChCk451L6/view
  - user-chat:2026-11-14/trap-waiting-mob-request
refs: []
semantic_contract: markdown-v2
language: ja
---

# Process: www.trap

## 契約

閉世界: 真

待機Mobに対して非同期ゲノム準備を先行させ、準備完了前に起動・被ダメージを許可しない。

## 境界
| PI |
| --- |
| www.trap.pi.event |

## 仕様状態
| ID | Domain |
| --- | --- |
| state.prepared | boolean |

## 観測能力
| ID | Source | Domain |
| --- | --- | --- |
| observation.bait_valid | Minecraft world | boolean |

## 作用能力
| ID | Target | Domain |
| --- | --- | --- |
| action.cancel_damage | Bukkit damage event | boolean |
| action.totem_feedback | Minecraft sound/particles | boolean |
| action.activate_trap | World spawn/pair | boolean |
| action.prepare_genomes | asynchronous founder | boolean |

## 起点
| ID | Source |
| --- | --- |
| spawn | www.trap.pi.event/spawn |
| damage | www.trap.pi.event/damage |
| synthesis | www.trap.pi.event/synthesis |
| proximity | www.trap.pi.event/proximity |
