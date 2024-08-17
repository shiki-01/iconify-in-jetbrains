package com.github.shiki01.iconifyinjetbrains.utils

import com.github.shiki01.iconifyinjetbrains.utils.IIJConstants.ICONIFY_API

object IIJUtils {
    sealed class Select {
        data class Search(val search: String) : Select()
        data object Collections : Select()
        data class Collection(val collection: String) : Select()
    }

    fun getIconifyAPIIcon(prefix: String, icon: String): String {
        return "${ICONIFY_API}/${prefix}/${icon}.svg"
    }

    fun getIconifyAPISorC(select: Select): String {
        return when (select) {
            is Select.Search -> "${ICONIFY_API}/search?query=${select.search.replace(" ", ",")}"
            is Select.Collections -> "${ICONIFY_API}/collections"
            is Select.Collection -> "${ICONIFY_API}/collection${select.collection.let { "?prefix=$it" }}"
        }
    }
}