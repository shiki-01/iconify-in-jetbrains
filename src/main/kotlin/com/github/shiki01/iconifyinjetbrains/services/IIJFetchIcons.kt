package com.github.shiki01.iconifyinjetbrains.services

import com.github.shiki01.iconifyinjetbrains.toolWindow.IIJToolWindowFactory.IIJToolWindow.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.swing.JComboBox

suspend fun fetchIcons(
    url: String,
): List<String> {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        try {
            val connection = connect(url)
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().use { it.readText() }

            val jsonElement = JsonParser.parseString(response)
            val jsonObject = if (jsonElement.isJsonObject) {
                jsonElement.asJsonObject
            } else {
                JsonObject()
            }
            val iconsArray = jsonObject.getAsJsonArray("icons")
            val iconMap = mutableMapOf<String, MutableList<String>>()

            iconsArray?.forEach { iconElement ->
                val icon = iconElement.asString
                val parts = icon.split(":")
                val setName = parts[0]
                val iconName = parts[1]
                iconMap.computeIfAbsent(setName) { mutableListOf() }.add(iconName)
            }

            iconMap.forEach { icons ->
                icons.value.forEach { icon ->
                    result.add("https://api.iconify.design/${icons.key}/$icon.svg")
                }
            }
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
        result
    }
}

suspend fun fetchIconSet(key: String): IconSetResponse {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        val uncategories = mutableListOf<IconCategory>()
        val categories = mutableListOf<IconCategory>()
        val aliasedIcons = mutableListOf<List<String>>()
        val suffixedIcons = mutableListOf<List<String>>()

        try {
            val url = "https://api.iconify.design/collections?prefix=$key"
            val connection = connect(url)
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().use { it.readText() }

            val jsonElement = JsonParser.parseString(response)
            val jsonObject = if (jsonElement.isJsonObject) {
                jsonElement.asJsonObject
            } else {
                JsonObject()
            }

            if (jsonObject.has("suffixes")) {
                val suffixes = jsonObject.getAsJsonObject("suffixes")
                for ((suffixes, value) in suffixes.entrySet()) {
                    val icon = value.asString
                    suffixedIcons.add(
                        listOf(
                            suffixes,
                            icon
                        )
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }

        result.addAll(uncategories.map { it.url })
        result.addAll(categories.map { it.url })

        IconSetResponse(result, uncategories, categories, aliasedIcons, suffixedIcons)
    }
}

suspend fun fetchIconData(
    url: String,
    category: String?,
    license: String?,
    palette: String?,
    licenseComboBox: JComboBox<String>? = null,
    categoryComboBox: JComboBox<String>? = null
): Response.Response {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<IconSet>()
        val licenses = mutableSetOf<String>()
        val categories = mutableSetOf<String>()

        try {
            val connection = connect(url)
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().use { it.readText() }

            val jsonElement = JsonParser.parseString(response)
            val jsonObject = if (jsonElement.isJsonObject) {
                jsonElement.asJsonObject
            } else {
                JsonObject()
            }

            jsonObject.keySet().forEach { key ->
                val collection = jsonObject.getAsJsonObject(key)
                val samples = collection.getAsJsonArray("samples")?.map { it.asString } ?: emptyList()
                val iconSet = createIconSet(collection, samples, key)
                if (filterIconSet(iconSet, category, license, palette)) {
                    result.add(iconSet)
                    licenses.add(iconSet.license.title)
                    categories.add(iconSet.category)
                }
            }
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
        Response.Response(result, licenses, categories, licenseComboBox, categoryComboBox)
    }
}

suspend fun fetchIconSearch(url: String): Response.SearchResponse {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()

        try {
            val connection = connect(url)
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().use { it.readText() }

            val jsonElement = JsonParser.parseString(response)
            val jsonObject = if (jsonElement.isJsonObject) {
                jsonElement.asJsonObject
            } else {
                JsonObject()
            }
            val iconsArray = jsonObject.getAsJsonArray("icons")
            val iconMap = mutableMapOf<String, MutableList<String>>()

            iconsArray?.forEach { iconElement ->
                val icon = iconElement.asString
                val parts = icon.split(":")
                val setName = parts[0]
                val iconName = parts[1]
                iconMap.computeIfAbsent(setName) { mutableListOf() }.add(iconName)
            }

            iconMap.forEach { icons ->
                icons.value.forEach { icon ->
                    result.add("https://api.iconify.design/${icons.key}/$icon.svg")
                }
            }
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }

        Response.SearchResponse(result)
    }
}

private fun filterIconSet(iconSet: IconSet, category: String?, license: String?, palette: String?): Boolean {
    if (category != null && category != "All" && iconSet.category != category) return false
    if (license != null && license != "All" && iconSet.license.title != license) return false
    val isPalette = palette == "Multiple colors"
    return !(palette != null && iconSet.palette != isPalette)
}

private fun createIconSet(collection: JsonObject, samples: List<String>, key: String): IconSet {
    val name = Name(
        collection.get("name")?.asString ?: "Unknown",
        key
    )
    val license = collection.getAsJsonObject("license")?.let {
        License(
            it.get("title")?.asString ?: "Unknown",
            it.get("url")?.asString ?: "#"
        )
    } ?: License("Unknown", "#")
    val author = collection.getAsJsonObject("author")?.let {
        Author(
            it.get("name")?.asString ?: "Unknown",
            it.get("url")?.asString ?: "#"
        )
    } ?: Author("Unknown", "#")
    val height = collection.get("height")?.asInt ?: 24
    val category = collection.get("category")?.asString ?: "General"
    val palette = collection.get("palette")?.asBoolean ?: false

    return IconSet(
        name,
        samples,
        license,
        author,
        height,
        category,
        palette
    )
}

private fun connect(url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    connection.requestMethod = "GET"
    return connection
}
