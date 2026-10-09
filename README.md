# What a Wonderful Wolf

What a Wonderful Wolf (WWW) is a Paper plugin for the XPlayServer project.

The functional specification is maintained in Google Drive under `forGPT/XPlayServer/Minecraft/What a Wonderful Wolf.md` and `forGPT/XPlayServer/Minecraft/WWW/`.

## Development

WWW targets Java 25 and Paper 26.2. During the WGL SNAPSHOT phase, clone the repositories as siblings:

```text
workspace/
├─ WonderfulGenomeLib/
└─ WhatAWonderfulWolf/
```

WWW uses Gradle Composite Build to resolve `co.surumene:wgl-plugin:0.1.0-SNAPSHOT` from the sibling `WonderfulGenomeLib` repository.

Build with:

```bash
gradle clean build --no-daemon
```

At runtime, Wonderful Genome Lib remains a separate Paper plugin and must be installed alongside WWW.

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
The current PDC phenotype storage constrains normal ability scores to
normalized `0..1.5`; command modifications respect this bound.
Changes to the phenotype are **not written back into the inherited Genome**.

`genome get` provides click-to-copy A/B chromosome bitstrings and
two offspring parent tokens: `wolf_<UUID>` for a loaded individual or
`wglp_<Base64URL>` for an encoded WGLP parent source.
The latter carries an explicit diploid-parent or gamete source type.
Legacy unprefixed Base64 WGLP input remains accepted by
`summon offspring` for backward compatibility.

All `config set/reset` operations check the **whole WWW config**
before committing, and attempt to restore the previous configuration if
runtime reload fails. As with WWC, numeric/other category resets are separate.
