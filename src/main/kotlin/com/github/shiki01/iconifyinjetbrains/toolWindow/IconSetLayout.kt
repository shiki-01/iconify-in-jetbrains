package com.github.shiki01.iconifyinjetbrains.toolWindow

import com.github.shiki01.iconifyinjetbrains.module.Response
import com.github.shiki01.iconifyinjetbrains.services.DisposableManager
import com.github.shiki01.iconifyinjetbrains.services.IIJURLtoSVG
import com.github.shiki01.iconifyinjetbrains.toolWindow.IIJToolWindow.showIconDetail
import com.github.shiki01.iconifyinjetbrains.toolWindow.IIJToolWindow.showSetPage
import com.github.shiki01.iconifyinjetbrains.utils.IIJConstants.DEFAULT_BORDER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

object IconSetLayout {
    suspend fun layout(iconPanel: JPanel, icons: Response.Response) {
        val response = icons.result
        icons.licenseComboBox?.let { updateComboBox(it, icons.licenses) }
        icons.categoryComboBox?.let { updateComboBox(it, icons.categories) }
        withContext(Dispatchers.Main) {
            iconPanel.removeAll()
            response.forEachIndexed { index, iconSet ->
                val titlePanel = JPanel(BorderLayout())
                val title = JLabel("<html><a href='#'>${iconSet.name.name}</a></html>").apply {
                    font = font.deriveFont(16f)
                    border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            showSetPage(iconSet)
                        }
                    })
                }
                val licenseLabel =
                    JLabel("<html><a href='${iconSet.license.url}'>${iconSet.license.title}</a></html>").apply {
                        font = font.deriveFont(12f)
                        border = BorderFactory.createEmptyBorder(10, 10, 10, 0)
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    }
                titlePanel.add(title, BorderLayout.WEST)
                titlePanel.add(licenseLabel, BorderLayout.EAST)
                titlePanel.border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
                iconPanel.add(titlePanel)

                val iconRow = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
                iconSet.samples.forEach { icon ->
                    val iconLabel = JLabel(icon)
                    iconRow.add(iconLabel)
                }
                iconRow.border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
                iconPanel.add(iconRow)

                if (index < response.size - 1) {
                    val separator = JSeparator(SwingConstants.HORIZONTAL)
                    val separatorPanel = JPanel(BorderLayout())
                    separatorPanel.add(separator, BorderLayout.CENTER)
                    separatorPanel.border = DEFAULT_BORDER
                    iconPanel.add(separatorPanel)
                }
            }
            val bottomSpace = JPanel()
            bottomSpace.border = DEFAULT_BORDER
            iconPanel.add(bottomSpace)

            iconPanel.revalidate()
            iconPanel.repaint()
        }
    }

    suspend fun searchLayout(iconPanel: JPanel, icons: Response.SearchResponse) {
        val response = icons.result
        withContext(Dispatchers.Main) {
            iconPanel.removeAll()
            response.forEachIndexed { _, icon ->
                val iconLabel = IIJURLtoSVG(icon, DisposableManager.parentDisposable).apply {
                    toolTipText = icon
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            showIconDetail(iconPanel, "search", icon)
                        }
                    })
                }
                val iconRow = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
                iconRow.add(iconLabel)
                iconPanel.add(iconRow)
            }
            iconPanel.revalidate()
            iconPanel.repaint()
        }
    }

    private fun updateComboBox(comboBox: JComboBox<String>?, items: Set<String>) {
        comboBox?.let {
            it.removeAllItems()
            it.addItem("All")
            items.forEach { item -> it.addItem(item) }
        }
    }
}