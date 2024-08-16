package com.github.shiki01.iconifyinjetbrains.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

object DisposableManager {
    lateinit var parentDisposable: Disposable

    fun initialize(toolWindowDisposable: Disposable) {
        parentDisposable = Disposer.newDisposable("parentDisposable")
        Disposer.register(toolWindowDisposable, parentDisposable)
    }
}