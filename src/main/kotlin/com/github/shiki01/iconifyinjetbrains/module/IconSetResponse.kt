package com.github.shiki01.iconifyinjetbrains.module

data class IconSetResponse(
    val result: List<String>,
    val uncategories: MutableList<IconCategory>? = null,
    val categories: List<IconCategory>? = null,
    val aliases: MutableList<List<String>>? = null,
    val suffices: MutableList<List<String>>? = null
)