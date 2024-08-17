package com.github.shiki01.iconifyinjetbrains.toolWindow

import com.github.shiki01.iconifyinjetbrains.actions.Action
import com.github.shiki01.iconifyinjetbrains.actions.ActionManager.executeAction
import com.github.shiki01.iconifyinjetbrains.module.Response
import com.github.shiki01.iconifyinjetbrains.services.DisposableManager
import com.github.shiki01.iconifyinjetbrains.services.IIJURLtoSVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border

object IIJIconSetLayout {

    private val toolWindow = IIJToolWindow(DisposableManager.parentDisposable)

    private fun createLabel(
        htmlText: String,
        fontSize: Float,
        border: Border,
        cursor: Cursor,
        onClick: () -> Unit
    ): JLabel {
        return JLabel(htmlText).apply {
            font = font.deriveFont(fontSize)
            this.border = border
            this.cursor = cursor
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    onClick()
                }
            })
        }
    }

    suspend fun layout(iconPanel: JPanel, icons: Response.Response) {
        val response = icons.result
        icons.licenseComboBox?.let { updateComboBox(it, icons.licenses) }
        icons.categoryComboBox?.let { updateComboBox(it, icons.categories) }
        withContext(Dispatchers.Main) {
            iconPanel.removeAll()
            val defaultBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            response.forEachIndexed { index, iconSet ->
                val titlePanel = JPanel(BorderLayout())
                val title = createLabel(
                    "<html><a href='#'>${iconSet.name.name}</a></html>",
                    16f,
                    BorderFactory.createEmptyBorder(10, 0, 10, 0),
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                ) { executeAction(Action { toolWindow.showSetPage(iconSet) }) }
                val licenseLabel = createLabel(
                    "<html><a href='${iconSet.license.url}'>${iconSet.license.title}</a></html>",
                    12f,
                    BorderFactory.createEmptyBorder(10, 10, 10, 0),
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                ) { }
                titlePanel.add(title, BorderLayout.WEST)
                titlePanel.add(licenseLabel, BorderLayout.EAST)
                titlePanel.border = defaultBorder
                iconPanel.add(titlePanel)

                val iconRow = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
                iconSet.samples.forEach { icon ->
                    val iconLabel = JLabel(icon)
                    iconRow.add(iconLabel)
                }
                iconRow.border = defaultBorder
                iconPanel.add(iconRow)

                if (index < response.size - 1) {
                    val separator = JSeparator(SwingConstants.HORIZONTAL)
                    val separatorPanel = JPanel(BorderLayout())
                    separatorPanel.add(separator, BorderLayout.CENTER)
                    separatorPanel.border = defaultBorder
                    iconPanel.add(separatorPanel)
                }
            }
            val bottomSpace = JPanel()
            bottomSpace.border = defaultBorder
            iconPanel.add(bottomSpace)

            iconPanel.revalidate()
            iconPanel.repaint()
        }
    }

    suspend fun searchLayout(iconPanel: JPanel, icons: Response.SearchResponse) {
        val response = icons.result
        withContext(Dispatchers.Main) {
            iconPanel.removeAll()
            val defaultBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            response.forEachIndexed { _, icon ->
                val iconLabel = IIJURLtoSVG(icon, DisposableManager.parentDisposable).apply {
                    toolTipText = icon
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            executeAction(Action { toolWindow.showIconDetail(iconPanel, "search", icon) })
                        }
                    })
                }
                val iconRow = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
                iconRow.add(iconLabel)
                iconRow.border = defaultBorder
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