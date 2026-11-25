package com.voidlauncher.util

import kotlinx.serialization.json.Json

/** Mojang's JSON evolves constantly, so unknown keys must never break the app. */
val voidJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}
