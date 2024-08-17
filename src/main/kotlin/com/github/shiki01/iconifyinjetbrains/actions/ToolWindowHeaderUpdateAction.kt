package com.github.shiki01.iconifyinjetbrains.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ToolWindowHeaderUpdateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // Your action logic here
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}