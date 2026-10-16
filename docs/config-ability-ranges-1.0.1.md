# WWW 1.0.1 — 能力Config設定

- `stats.<ability>.min/max/display` を全10能力に追加した。
- 対象能力: `max-health`, `defense`, `patience`, `size`, `inventory`, `ground-speed`, `jump-strength`, `step-height`, `attack-damage`, `attack-speed`。
- 正規化値0→min、1→max、1超は線形外挿。体力・積載量の丸め、最終積載45枠制限は維持。
- `display` は `none/value/rank/both`。プレイヤー向けGUIと詳細の能力行のみ制御し、管理者用 `/www info` はデバッグ情報を保持する。
- 旧1.0.0 configに`stats`がなくても従来レンジを採用する。キー追加後は数値型・有限性・min/max順序・displayモードを検証。
- `reload`時は既存の能力投影dirty化機構で実効値へ再反映する。Genome・出生時Phenotypeは不変。
- Founder Targetの採用範囲は現行固定（自然0〜0.5、トラップ0.5〜1.5）、`stats.range-change-policy` はWWWには導入しない。
- `inventory.max` は正規化1の実値であり、45枠上限そのものは変更しない。
- 未検証: 実Paperでの再投影、GUI、装備・移動AI、旧設定ファイル更新の実地確認。
