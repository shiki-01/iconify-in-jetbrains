package com.github.shiki01.iconifyinjetbrains.actions

class Action(private val action: () -> Unit) {
    fun execute() {
        action()
    }
}