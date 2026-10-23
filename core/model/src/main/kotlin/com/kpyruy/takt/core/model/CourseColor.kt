package com.kpyruy.takt.core.model

/** Stable subject palette keys; null means the automatic theme color. */
enum class CourseColor(val argb: Long) {
    BLUE(0xFF3D78C8), TEAL(0xFF148B85), GREEN(0xFF4C8A39), LIME(0xFF859B29),
    AMBER(0xFFBC870C), ORANGE(0xFFD17532), RED(0xFFC84B4B), ROSE(0xFFC65382),
    PURPLE(0xFF9960BB), INDIGO(0xFF6964BB), CYAN(0xFF268DA7), BROWN(0xFF9A7553);

    companion object {
        fun fromKey(key: String?): CourseColor? = entries.firstOrNull { it.name == key }
    }
}
