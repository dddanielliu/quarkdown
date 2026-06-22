package com.quarkdown.rendering.html

import com.quarkdown.rendering.html.pdf.HtmlPdfExportOptions
import com.quarkdown.rendering.html.pdf.HtmlPdfExporter
import com.quarkdown.server.LocalFileWebServer
import com.quarkdown.server.withScanner
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for HTML-to-PDF generation.
 */
class HtmlToPdfTest {
    private val directory: File =
        createTempDirectory()
            .toFile()

    private val options =
        HtmlPdfExportOptions(
            noSandbox = true,
        )

    @BeforeTest
    fun setup() {
        directory.deleteRecursively()
        directory.mkdirs()
    }

    @Test
    fun `bare script on simple html`() {
        val html = File(directory, "index.html")
        html.writeText(
            """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test</title>
            </head>
            <body>
                <h1>Hello, Quarkdown!</h1>
                <script>
                function isReady() { return true; }
                window.isReady = isReady;
                </script>
            </body>
            </html>
            """.trimIndent(),
        )

        println(directory)
        println(directory.list().contentToString())

        val out = File(directory, "out.pdf")
        HtmlPdfExporter(options).export(directory, out)

        assertTrue(out.exists())

        Loader.loadPDF(out).use {
            val text = PDFTextStripper().getText(it).trim()
            assertEquals(1, it.numberOfPages)
            assertEquals("Hello, Quarkdown!", text)
        }
    }
}
