package com.github.shiki01.iconifyinjetbrains.toolWindow

import com.github.shiki01.iconifyinjetbrains.module.Author
import com.github.shiki01.iconifyinjetbrains.module.IconSet
import com.github.shiki01.iconifyinjetbrains.module.License
import com.github.shiki01.iconifyinjetbrains.module.Name
import com.github.shiki01.iconifyinjetbrains.services.DisposableManager.parentDisposable
import com.github.shiki01.iconifyinjetbrains.utils.IIJConstants.DEFAULT_BORDER
import com.github.shiki01.iconifyinjetbrains.utils.IIJFetchUtils.fetchIconData
import com.github.shiki01.iconifyinjetbrains.utils.IIJFetchUtils.fetchIconSet
import com.github.shiki01.iconifyinjetbrains.utils.IIJFetchUtils.fetchIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ItemEvent
import javax.swing.*

object IIJToolWindow {

    private val mainPanel = JBPanel<JBPanel<*>>()

    fun getContent() = mainPanel.apply {
        layout = BorderLayout()
        minimumSize = Dimension(400, 300)
        showMainPage()
    }

    private suspend fun updateComboBox(
        iconPanel: JPanel,
        categoryComboBox: JComboBox<String>? = null,
        paletteComboBox: JComboBox<String>? = null,
        licenseComboBox: JComboBox<String>? = null
    ) {
        val icons = fetchIconData(
            "https://api.iconify.design/collections",
            categoryComboBox?.selectedItem as String? ?: "All",
            licenseComboBox?.selectedItem as String? ?: "All",
            paletteComboBox?.selectedItem as String? ?: "All",
            licenseComboBox = licenseComboBox,
            categoryComboBox = categoryComboBox
        )
        withContext(Dispatchers.Main) {
            IconSetLayout.layout(iconPanel, icons)
        }
    }

    private fun showMainPage() {
        mainPanel.removeAll()
        val iconPanel = JPanel()
        iconPanel.layout = BoxLayout(iconPanel, BoxLayout.Y_AXIS)
        val scrollPane = JBScrollPane(iconPanel)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBar.unitIncrement = 16

        val filterPanel = JPanel(GridLayout(4, 1)).apply { border = JBUI.Borders.empty(10) }
        val filterPanelS = JPanel(BorderLayout())
        val filterPanelL = JPanel(FlowLayout(FlowLayout.LEFT))
        val filterPanelP = JPanel(FlowLayout(FlowLayout.LEFT))
        val filterPanelC = JPanel(FlowLayout(FlowLayout.LEFT))

        val searchField = JTextField()
        val licenseComboBox = ComboBox(arrayOf("All"))
        val paletteComboBox = ComboBox(arrayOf("All", "Single color", "Multiple colors"))
        val categoryComboBox = ComboBox(arrayOf("All"))

        filterPanelS.add(JLabel("Search:"), BorderLayout.WEST)
        filterPanelS.add(searchField, BorderLayout.CENTER)
        filterPanelL.add(JLabel("License:"))
        filterPanelL.add(licenseComboBox)
        filterPanelP.add(JLabel("Palette:"))
        filterPanelP.add(paletteComboBox)
        filterPanelC.add(JLabel("Category:"))
        filterPanelC.add(categoryComboBox)

        filterPanel.add(filterPanelS)
        filterPanel.add(filterPanelL)
        filterPanel.add(filterPanelP)
        filterPanel.add(filterPanelC)

        mainPanel.add(filterPanel, BorderLayout.NORTH)

        val filterActionListener = ActionListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (searchField.text.isEmpty()) {
                        updateComboBox(iconPanel, categoryComboBox, paletteComboBox, licenseComboBox)
                    } else {
                        fetchIcons("https://api.iconify.design/search")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.invokeOnCompletion { Disposer.dispose(parentDisposable) }
        }

        searchField.addActionListener(filterActionListener)
        licenseComboBox.addActionListener(filterActionListener)
        categoryComboBox.addActionListener(filterActionListener)
        paletteComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED || it.stateChange == ItemEvent.DESELECTED) {
                filterActionListener.actionPerformed(null)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateComboBox(iconPanel, categoryComboBox, paletteComboBox, licenseComboBox)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.invokeOnCompletion { Disposer.dispose(parentDisposable) }

        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                iconPanel.revalidate()
                iconPanel.repaint()
            }
        })

        Disposer.register(parentDisposable) {
            mainPanel.removeComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    iconPanel.revalidate()
                    iconPanel.repaint()
                }
            })
        }

        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.revalidate()
        mainPanel.repaint()
    }

    fun showSetPage(iconSet: IconSet) {
        mainPanel.removeAll()
        val iconPanel = JPanel()
        iconPanel.layout = BoxLayout(iconPanel, BoxLayout.Y_AXIS)
        val scrollPane = JBScrollPane(iconPanel)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBar.unitIncrement = 16

        val backButton = JButton("Back").apply {
            addActionListener {
                showMainPage()
            }
        }
        mainPanel.add(backButton, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        val titlePanel = JPanel(BorderLayout())
        val title = JLabel(iconSet.name.name).apply {
            font = font.deriveFont(16f)
            border = DEFAULT_BORDER
        }
        val creater = JLabel("by ${iconSet.author.name}").apply {
            font = font.deriveFont(12f)
            border = DEFAULT_BORDER
        }
        val licenseLabel = JLabel(iconSet.license.title).apply {
            font = font.deriveFont(12f)
            border = DEFAULT_BORDER
        }
        titlePanel.add(title, BorderLayout.WEST)
        titlePanel.add(creater, BorderLayout.CENTER)
        titlePanel.add(licenseLabel, BorderLayout.EAST)
        titlePanel.border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
        iconPanel.add(titlePanel)

        CoroutineScope(Dispatchers.IO).launch {
            val icons = fetchIconSet(iconSet.name.key).result
            withContext(Dispatchers.Main) {
                icons.forEach { icon ->
                    val iconLabel = JLabel(icon)
                    iconPanel.add(iconLabel)
                }
                iconPanel.revalidate()
                iconPanel.repaint()
            }
        }.invokeOnCompletion {
            Disposer.dispose(parentDisposable)
        }
    }

    fun showIconDetail(iconPanel: JPanel, collectionName: String, icon: String) {
        iconPanel.removeAll()
        val backButton = JButton("Back").apply {
            addActionListener {
                showSetPage(
                    IconSet(
                        Name(collectionName, collectionName),
                        listOf(icon),
                        License("", ""),
                        Author("", ""),
                        0,
                        "",
                        false
                    )
                )
            }
        }
        iconPanel.add(backButton)
        val detailLabel = JLabel("Details for $icon from $collectionName").apply {
            font = font.deriveFont(16f)
            border = DEFAULT_BORDER
        }
        iconPanel.add(detailLabel)
        iconPanel.revalidate()
        iconPanel.repaint()
    }
}