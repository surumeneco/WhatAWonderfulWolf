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
