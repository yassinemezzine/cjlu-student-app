package com.cjlu.studentapp.data

import kotlinx.serialization.json.Json

internal val academicJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
