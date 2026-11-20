package com.kotha.app.data.chat

object MediaBatch {

    private val pattern = Regex("^([A-Za-z0-9]{20})_(\\d{1,3})$")

    fun idOf(key: String, index: Int): String = "${key}_$index"

    fun keyOf(id: String): String? = pattern.matchEntire(id)?.groupValues?.get(1)

    fun indexOf(id: String): Int = pattern.matchEntire(id)?.groupValues?.get(2)?.toIntOrNull() ?: Int.MAX_VALUE
}
