package com.lingualink.update

object VersionUtils {

    fun isNewerVersion(current: String, latest: String): Boolean {
        val c = current.split(".", "-").mapNotNull { it.toIntOrNull() }
        val l = latest.split(".", "-").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(c.size, l.size)) {
            val cv = c.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
