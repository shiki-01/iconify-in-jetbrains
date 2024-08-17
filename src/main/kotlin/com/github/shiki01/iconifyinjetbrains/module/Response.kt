package com.github.shiki01.iconifyinjetbrains.module

import javax.swing.JComboBox

sealed class Response {
    data class Response(
        val result: List<IconSet>,
        val licenses: Set<String>,
        val categories: Set<String>,
        val licenseComboBox: JComboBox<String>? = null,
        val categoryComboBox: JComboBox<String>? = null
    )

    data class SearchResponse(
        val result: MutableList<String>,
    )
}