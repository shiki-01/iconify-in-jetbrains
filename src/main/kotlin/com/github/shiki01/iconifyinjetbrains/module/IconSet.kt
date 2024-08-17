package com.github.shiki01.iconifyinjetbrains.module

data class IconSet(
    val name: Name,
    val samples: List<String>,
    val license: License,
    val author: Author,
    val height: Int,
    val category: String,
    val palette: Boolean
)