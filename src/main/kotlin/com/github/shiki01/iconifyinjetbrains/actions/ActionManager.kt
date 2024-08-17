package com.github.shiki01.iconifyinjetbrains.actions

object ActionManager {
    fun executeAction(action: Action) {
        action.execute()
    }
}