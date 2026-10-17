# What a Wonderful Wolf

## 1.0.1 patch

- For runtime-only changes, reuse the previously published immutable Genome/Snapshot payload reference when verified in-process identity matches. Changed Genome or Phenotype still follows the full copy-on-write path.
- Requires WGL 1.0.1 at runtime. Server-side TPS/MSPT improvement has not yet been measured.

What a Wonderful Wolf (WWW) 1.0.0 is a Paper plugin for the XPlayServer project.

The functional specification is maintained in Google Drive under `forGPT/XPlayServer/Minecraft/What a Wonderful Wolf.md` and `forGPT/XPlayServer/Minecraft/WWW/`.

## Development

WWW targets Java 25 and Paper 26.2. For the WGL 1.0.1 release, clone the repositories as siblings:

```text
workspace/
├─ WonderfulGenomeLib/
└─ WhatAWonderfulWolf/
```

WWW uses Gradle Composite Build to resolve `co.surumene:wgl-plugin:1.0.1` from the sibling `WonderfulGenomeLib` repository.

Build with:

```bash
gradle clean build --no-daemon
```

At runtime, Wonderful Genome Lib 1.0.1 remains a separate Paper plugin and must be installed alongside WWW.

## Config changes in 1.0.1

All ten WWW abilities now expose `stats.<ability>.min/max/display` in the bundled `config.yml`. The initial canonical ranges are identical to those from 1.0.0; the `min/max` values describe normalized Genome ability scores 0 and 1, with linear extrapolation above 1. The `display` values `none`, `value`, `rank`, and `both` apply to the player GUI/detail only. Administrative `/www info` retains complete diagnostic output.

New settings are validated as finite ordered ranges and approved display strings. Existing configuration without `stats` defaults to the original ranges. Reload reprojects effective abilities and never rewrites immutable Genome or birth Phenotype Snapshots. Inventory retains the hard 45-slot cap. Founder/Genome Profile settings and legacy conversion semantics are unchanged.

## Administrative commands (WWC 2.0-compatible)

```text
/www summon [x y z] [<www-data>] [<vanilla-nbt>]
/www info [<entity selector>]
/www genome get <entity selector>
/www summon genome <text|bits|hex|dna> <haplotypeA> [haplotypeB]
/www summon offspring parent <sourceA> parent <sourceB>
/www modify <entity selector> <set|add> <field> <value>
/www config get <path>
/www config set <path> <value>
/www config list [path]
/www config reset <path>
/www config reset numeric [path]
/www config reset other [path]
/www reload
```

The long alias `/whatawonderfulwolf` is also registered. Commands use independent
`www.command.*` permission nodes; config commands accept either
`www.command.config` or `www.command.config.<get|set|list|reset>`.

`summon` defaults to the execution location and a natural Founder.
Relative `~` and local `^` coordinates are supported.
Its WWW-specific compound supports `stats:{max_health:40,movement_speed:8}`,
`equipment:{weapon:"iron_sword"}`, `behavior:{mode:"follow"}`, and `baby:true`.
A single compound without WWW keys is passed as vanilla NBT, while two
compounds represent WWW data followed by vanilla NBT. A commanded
mode needs a player executor; the spawned wolf is tamed and
its Owner and command state are stored together.

`modify` accepts all ten WWW canonical abilities:
`max-health`, `size`, `movement-speed`, `jump`,
`step-height`, `attack-damage`, `attack-speed`, `defense`,
`patience`, `inventory`, and `mode` (`set` only).
Values are canonical units (not normalized Genome scores).
Genome-derived Phenotype Snapshot scores remain bounded to normalized `0..1.5`.
Administrator changes are stored separately as canonical ability overrides
in runtime/PDC state, without changing inherited Genome or the original
Phenotype Snapshot. Valid debug values outside both the normal and natural
Founder ranges are permitted, subject to finite/positive inputs and
Minecraft/Paper attribute applicability. Rankings saturate at the existing
lowest/highest grade. The fixed 45-slot inventory UI cannot be enlarged.

`genome get` provides click-to-copy A/B chromosome bitstrings and
two offspring parent tokens: `wolf_<UUID>` for a loaded individual or
`wglp_<Base64URL>` for an encoded WGLP parent source.
The latter carries an explicit diploid-parent or gamete source type.
Legacy unprefixed Base64 WGLP input remains accepted by
`summon offspring` for backward compatibility.

All `config set/reset` operations check the **whole WWW config**
before committing, and attempt to restore the previous configuration if
runtime reload fails. As with WWC, numeric/other category resets are separate.
