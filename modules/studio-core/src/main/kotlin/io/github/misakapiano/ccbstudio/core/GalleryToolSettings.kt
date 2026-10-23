package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable

/** Management preferences belong to the serialization-enabled core, not the desktop renderer. */
@Serializable data class GalleryToolSettings(
    val version:Int=1,
    val order:List<String> = emptyList(),
    val hidden:Set<String> = emptySet(),
    val tiles:Boolean=true,
)
