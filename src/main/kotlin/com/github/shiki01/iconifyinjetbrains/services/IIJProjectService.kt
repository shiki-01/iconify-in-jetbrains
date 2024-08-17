package com.github.shiki01.iconifyinjetbrains.services

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class IIJProjectService {
    init {
        println("Hello from IIJProjectService!")
    }

    fun getRandomNumber(): Int {
        return (0..100).random()
    }
}
