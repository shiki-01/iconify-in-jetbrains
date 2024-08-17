package com.github.shiki01.iconifyinjetbrains.utils

import com.github.shiki01.iconifyinjetbrains.utils.IIJConstants.ICONIFY_API

object IIJUtils {
    fun getIconifyAPIIcon(prefix: String, icon: String): String {
        return "${ICONIFY_API}/${prefix}/${icon}.svg"
    }
}