package com.github.shiki01.iconifyinjetbrains.services

object SVGCache {
    private val cache = mutableMapOf<String, String>()

    fun get(svgUrl: String): String? = cache[svgUrl]

    fun put(svgUrl: String, svgData: String) {
        cache[svgUrl] = svgData
    }
}