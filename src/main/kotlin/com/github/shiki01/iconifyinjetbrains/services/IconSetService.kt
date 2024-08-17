package com.github.shiki01.iconifyinjetbrains.services

import com.github.shiki01.iconifyinjetbrains.module.IconCategory
import com.github.shiki01.iconifyinjetbrains.module.IconSet
import com.github.shiki01.iconifyinjetbrains.module.IconSetResponse
import com.github.shiki01.iconifyinjetbrains.module.Response

class IconSetService {
    private val iconSetManager = IconSetManager()

    fun loadIconSets(response: Response.Response) {
        response.result.forEach { iconSetManager.addIconSet(it) }
    }

    fun loadIconCategories(response: IconSetResponse) {
        response.categories?.forEach { iconSetManager.addIconCategory(it) }
    }

    fun getIconSets(): List<IconSet> {
        return iconSetManager.getIconSets()
    }

    fun getIconCategories(): List<IconCategory> {
        return iconSetManager.getIconCategories()
    }
}