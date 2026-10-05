rootProject.name = "WhatAWonderfulWolf"

val wglDirectory = file("../WonderfulGenomeLib")
check(wglDirectory.isDirectory) {
    "WonderfulGenomeLib must be checked out next to WhatAWonderfulWolf: ${wglDirectory.absolutePath}"
}

includeBuild(wglDirectory)
