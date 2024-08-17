package com.github.shiki01.iconifyinjetbrains.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.*
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class IIJURLtoSVG(url: String, parentDisposable: Disposable) : JLabel(), Disposable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        Disposer.register(parentDisposable, this)

        scope.launch {
            try {
                val svgCode = fetchSVGFromURL(url)
                val bufferedImage = convertSVGToBufferedImage(svgCode)
                val imageIcon = ImageIcon(bufferedImage)
                withContext(Dispatchers.Main) {
                    this@IIJURLtoSVG.icon = imageIcon
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun fetchSVGFromURL(svgUrl: String): String {
        SVGCache.get(svgUrl)?.let { return it }

        var attempts = 0
        val maxAttempts = 3
        var svgCode: String? = null

        while (attempts < maxAttempts && svgCode == null) {
            try {
                withContext(Dispatchers.IO) {
                    BufferedInputStream(URL(svgUrl).openStream()).use { inputStream ->
                        svgCode = String(inputStream.readAllBytes()).replace("currentColor", "#000000")
                        if (!svgCode!!.contains("xmlns:xlink")) {
                            svgCode = svgCode!!.replace("<svg ", "<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
                        }
                        if (svgCode!!.contains("<use ") && !svgCode!!.contains("xlink:href")) {
                            svgCode = svgCode!!.replace("<use ", "<use xlink:href=\"#icon\" ")
                        }
                        if (svgCode!!.contains("<use ") && !svgCode!!.contains("id=\"icon\"")) {
                            svgCode = svgCode!!.replace("</svg>", "<symbol id=\"icon\"><path d=\"\"/></symbol></svg>")
                        }
                        if (svgCode!!.contains("<stop ") && !svgCode!!.contains("offset=\"")) {
                            svgCode = svgCode!!.replace("<stop ", "<stop offset=\"0%\" ")
                        }
                        if (svgCode!!.contains("<path ") && !svgCode!!.contains("d=\"")) {
                            svgCode = svgCode!!.replace("<path ", "<path d=\"M0,0 L0,100 L100,100 Z\" ")
                        }

                        svgCode = validateSVGStopData(svgCode!!)
                        SVGCache.put(svgUrl, svgCode!!)
                    }
                }
            } catch (e: IOException) {
                attempts++
                if (attempts >= maxAttempts) {
                    throw e
                }
            }
        }
        return svgCode!!
    }

    private fun validateSVGStopData(svgCode: String): String {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document: Document = builder.parse(ByteArrayInputStream(svgCode.toByteArray()))
            val stopElements = document.getElementsByTagName("stop")
            for (i in 0 until stopElements.length) {
                val stopElement = stopElements.item(i) as Element
                if (!stopElement.hasAttribute("offset") || stopElement.getAttribute("offset").isEmpty()) {
                    stopElement.setAttribute("offset", "0%")
                }
            }
            val transformer = TransformerFactory.newInstance().newTransformer()
            val output = StringWriter()
            transformer.transform(DOMSource(document), StreamResult(output))
            return output.toString()
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
            throw e
        } catch (e: SAXException) {
            e.printStackTrace()
            throw e
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        } catch (e: TransformerException) {
            e.printStackTrace()
            throw e
        }
    }

    @Throws(TranscoderException::class, IOException::class)
    private fun convertSVGToBufferedImage(svgCode: String): BufferedImage {
        val transcoder = PNGTranscoder()
        val contextClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = SAXParserFactory::class.java.classLoader

        try {
            ByteArrayInputStream(svgCode.toByteArray()).use { inputStream ->
                ByteArrayOutputStream().use { outputStream ->
                    val input = TranscoderInput(inputStream)
                    val output = TranscoderOutput(outputStream)
                    transcoder.transcode(input, output)
                    val pngData = outputStream.toByteArray()
                    return ImageIO.read(ByteArrayInputStream(pngData))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw TranscoderException(e)
        } finally {
            Thread.currentThread().contextClassLoader = contextClassLoader
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}