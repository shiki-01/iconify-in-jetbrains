package com.github.shiki01.iconifyinjetbrains

import com.kitfox.svg.SVGDiagram
import com.kitfox.svg.SVGUniverse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO
import javax.swing.JLabel
import javax.swing.SwingUtilities

class IIJURLtoSVG(svgContent: String) : JLabel() {

    private var svgDiagram: SVGDiagram? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val cleanedSvgContent = svgContent.trim()
            val svgUniverse = SVGUniverse()
            val svgUri = try {
                svgUniverse.loadSVG(cleanedSvgContent.byteInputStream(), "icon.svg")
            } catch (e: FileNotFoundException) {
                println("SVG file not found")
                null
            } catch (e: Exception) {
                println("Error loading SVG: ${e.message}")
                null
            }
            svgDiagram = svgUniverse.getDiagram(svgUri)
            SwingUtilities.invokeLater {
                println("SVG Diagram loaded: $svgDiagram")
                preferredSize = Dimension(100, 100)
                revalidate()
                repaint()
                saveAsImage("output.png")
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        svgDiagram?.let {
            it.setIgnoringClipHeuristic(true)
            it.render(g2d)
        }
    }

    private fun saveAsImage(filePath: String) {
        svgDiagram?.let {
            val image = BufferedImage(preferredSize.width, preferredSize.height, BufferedImage.TYPE_INT_ARGB)
            val g2d = image.createGraphics()
            it.render(g2d)
            g2d.dispose()
            val file = File(filePath)
            ImageIO.write(image, "png", file)
            println("Image saved to ${file.absolutePath}")
        } ?: run {
            println("SVG Diagram is null, cannot save image.")
        }
    }
}

val svg = """
    <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24">
        <path fill="currentColor" d="m12 21.35l-1.45-1.32C5.4 15.36 2 12.27 2 8.5C2 5.41 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.08C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.41 22 8.5c0 3.77-3.4 6.86-8.55 11.53z"/>
    </svg>
""".trimIndent()

val svgLabel = IIJURLtoSVG(svg)

fun main() {
    println("Hello from IconifyInJetBrains!")
}