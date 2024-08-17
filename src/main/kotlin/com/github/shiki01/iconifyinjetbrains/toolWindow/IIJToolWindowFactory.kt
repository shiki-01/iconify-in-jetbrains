package com.github.shiki01.iconifyinjetbrains.toolWindow

import com.github.shiki01.iconifyinjetbrains.services.DisposableManager
import com.github.shiki01.iconifyinjetbrains.toolWindow.IIJToolWindow.getContent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class IIJToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        DisposableManager.initialize(toolWindow.disposable)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(getContent(), null, false))
    }

    override fun shouldBeAvailable(project: Project) = true
}