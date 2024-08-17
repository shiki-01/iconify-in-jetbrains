package com.github.shiki01.iconifyinjetbrains.utils

import com.github.shiki01.iconifyinjetbrains.module.*
import com.github.shiki01.iconifyinjetbrains.utils.IIJConstants.result
import com.github.shiki01.iconifyinjetbrains.utils.IIJUtils.getIconifyAPIIcon
import com.github.shiki01.iconifyinjetbrains.utils.IIJUtils.getIconifyAPISorC
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.swing.JComboBox

object IIJFetchUtils {
    suspend fun fetchIcons(
        url: String,
    ): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                result.clear()
                val connection = connect(url)
                try {
                    val response = connection.inputStream.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }

                    val jsonElement = JsonParser.parseString(response)
                    if (!jsonElement.isJsonObject) throw JsonSyntaxException("Expected JsonObject")
                    val jsonObject = jsonElement.asJsonObject
                    val iconsArray =
                        jsonObject.getAsJsonArray("icons") ?: throw JsonSyntaxException("Expected JsonArray 'icons'")
                    val iconMap = mutableMapOf<String, MutableList<String>>()

                    iconsArray.forEach { iconElement ->
                        val icon = iconElement?.asString ?: return@forEach
                        val parts = icon.split(":")
                        val setName = parts[0]
                        val iconName = parts[1]
                        iconMap.computeIfAbsent(setName) { mutableListOf() }.add(iconName)
                    }

                    iconMap.forEach { icons ->
                        icons.value.forEach { icon ->
                            result.add(getIconifyAPIIcon(icons.key, icon))
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            result
        }
    }

    suspend fun fetchIconSet(key: String): IconSetResponse {
        return withContext(Dispatchers.IO) {
            val uncategories = mutableListOf<IconCategory>()
            val categories = mutableListOf<IconCategory>()
            val aliasedIcons = mutableListOf<List<String>>()
            val suffixedIcons = mutableListOf<List<String>>()

            try {
                result.clear()
                val url = getIconifyAPISorC(IIJUtils.Select.Search(key))
                val connection = connect(url)
                try {
                    val response = connection.inputStream.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }

                    val jsonObject = jsonObject(response) ?: throw JsonSyntaxException("Expected JsonObject")

                    if (jsonObject.has("suffixes")) {
                        val suffixes = jsonObject.getAsJsonObject("suffixes")
                        for ((suffixesKey, value) in suffixes.entrySet()) {
                            val icon = value.asString
                            suffixedIcons.add(
                                listOf(
                                    suffixesKey,
                                    icon
                                )
                            )
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
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
                try {
                    val response = connection.inputStream.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }

                    val jsonObject = jsonObject(response) ?: throw JsonSyntaxException("Expected JsonObject")

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
                } finally {
                    connection.disconnect()
                }
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Response.Response(result, licenses, categories, licenseComboBox, categoryComboBox)
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

    private fun jsonObject(response: String): JsonObject? {
        val jsonElement = JsonParser.parseString(response)
        if (!jsonElement.isJsonObject) throw JsonSyntaxException("Expected JsonObject")
        return jsonElement.asJsonObject
    }
}