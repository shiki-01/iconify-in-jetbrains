package com.github.shiki01.iconifyinjetbrains.toolWindow

import com.github.shiki01.iconifyinjetbrains.actions.Action
import com.github.shiki01.iconifyinjetbrains.actions.ActionManager.executeAction
import com.github.shiki01.iconifyinjetbrains.actions.JoinCallAction
import com.github.shiki01.iconifyinjetbrains.actions.ToolWindowHeaderUpdateAction
import com.github.shiki01.iconifyinjetbrains.module.Author
import com.github.shiki01.iconifyinjetbrains.module.IconSet
import com.github.shiki01.iconifyinjetbrains.module.License
import com.github.shiki01.iconifyinjetbrains.module.Name
import com.github.shiki01.iconifyinjetbrains.services.DisposableManager.parentDisposable
import com.github.shiki01.iconifyinjetbrains.utils.IIJConstants.DEFAULT_BORDER
import com.github.shiki01.iconifyinjetbrains.utils.IIJFetchUtils.fetchIconData
import com.github.shiki01.iconifyinjetbrains.utils.IIJFetchUtils.fetchIconSet
import com.github.shiki01.iconifyinjetbrains.utils.IIJFetchUtils.fetchIcons
import com.github.shiki01.iconifyinjetbrains.utils.IIJUtils
import com.github.shiki01.iconifyinjetbrains.utils.IIJUtils.getIconifyAPISorC
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ItemEvent
import javax.swing.*

class IIJToolWindow(parentDisposable: Disposable) : Disposable {

    val mainPanel = JPanel(BorderLayout())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        Disposer.register(parentDisposable, this)

        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                mainPanel.revalidate()
                mainPanel.repaint()
            }
        })

        Disposer.register(parentDisposable) {
            mainPanel.removeComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    mainPanel.revalidate()
                    mainPanel.repaint()
                }
            })
        }

        val dataContext: DataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> null
                PlatformDataKeys.CONTEXT_COMPONENT.name -> mainPanel
                else -> null
            }
        }

        val joinCallButton = JButton("Join Call").apply {
            addActionListener {
                JoinCallAction().actionPerformed(AnActionEvent.createFromDataContext("", null, dataContext))
            }
        }
        val updateHeaderButton = JButton("Update Header").apply {
            addActionListener {
                ToolWindowHeaderUpdateAction().actionPerformed(
                    AnActionEvent.createFromDataContext(
                        "",
                        null,
                        dataContext
                    )
                )
            }
        }

        mainPanel.add(joinCallButton, BorderLayout.SOUTH)
        mainPanel.add(updateHeaderButton, BorderLayout.SOUTH)

        showMainPage()

        mainPanel.revalidate()
        mainPanel.repaint()
    }

    private suspend fun updateComboBox(
        iconPanel: JPanel,
        categoryComboBox: JComboBox<String>? = null,
        paletteComboBox: JComboBox<String>? = null,
        licenseComboBox: JComboBox<String>? = null
    ) {
        val icons = fetchIconData(
            getIconifyAPISorC(IIJUtils.Select.Collections),
            categoryComboBox?.selectedItem as String? ?: "All",
            licenseComboBox?.selectedItem as String? ?: "All",
            paletteComboBox?.selectedItem as String? ?: "All",
            licenseComboBox = licenseComboBox,
            categoryComboBox = categoryComboBox
        )
        withContext(Dispatchers.Main) {
            IIJIconSetLayout.layout(iconPanel, icons)
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
                        fetchIcons(getIconifyAPISorC(IIJUtils.Select.Search(searchField.text)))
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
                executeAction(Action { showMainPage() })
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

        scope.launch {
            val icons = fetchIconSet(iconSet.name.key).result
            withContext(Dispatchers.Main) {
                icons.forEach { icon ->
                    val iconLabel = JLabel(icon)
                    iconPanel.add(iconLabel)
                }
                iconPanel.revalidate()
                iconPanel.repaint()
            }
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

    override fun dispose() {
        scope.cancel()
    }
}