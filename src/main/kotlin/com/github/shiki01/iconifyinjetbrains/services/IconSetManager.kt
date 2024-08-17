package com.github.shiki01.iconifyinjetbrains.services

import com.github.shiki01.iconifyinjetbrains.module.IconCategory
import com.github.shiki01.iconifyinjetbrains.module.IconSet

class IconSetManager {
    private val iconSets: MutableList<IconSet> = mutableListOf()
    private val iconCategories: MutableList<IconCategory> = mutableListOf()

    fun addIconSet(iconSet: IconSet) {
        iconSets.add(iconSet)
    }

    fun addIconCategory(iconCategory: IconCategory) {
        iconCategories.add(iconCategory)
    }

    fun getIconSets(): List<IconSet> {
        return iconSets
    }

    fun getIconCategories(): List<IconCategory> {
        return iconCategories
    }

    fun findIconSetByName(name: String): IconSet? {
        return iconSets.find { it.name.name == name }
    }

    fun findIconCategoryByUrl(url: String): IconCategory? {
        return iconCategories.find { it.url == url }
    }
}