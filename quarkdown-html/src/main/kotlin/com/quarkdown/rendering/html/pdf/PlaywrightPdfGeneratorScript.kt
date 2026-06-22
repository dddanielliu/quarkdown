package com.quarkdown.rendering.html.pdf

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.quarkdown.core.log.Log
import com.quarkdown.server.LocalFileWebServer
import com.quarkdown.server.withScanner
import java.io.File
import java.nio.file.Paths

/**
 * The starting port to attempt to start the server on.
 * It is incremented until a free port is found.
 */
private const val STARTING_SERVER_PORT = 8096

/**
 * Generates a PDF from HTML through Playwright Java API.
 * @param sourcesDirectory directory containing the `index.html` file
 * @param out output PDF file to be written
 * @param noSandbox whether to disable Chrome sandbox for PDF export
 */
class PlaywrightPdfGeneratorScript(
    private val sourcesDirectory: File,
    private val out: File,
    private val noSandbox: Boolean = false,
) {
    private var port: Int? = null

    /**
     * Launches Playwright to convert the webpage from [sourcesDirectory] into a PDF saved at [out].
     * Blocking call.
     */
    fun launch() {
        launchServer()
    }

    private fun launchServer() {
        LocalFileWebServer(sourcesDirectory)
            .withScanner()
            .attemptStartUntilPortAvailable(STARTING_SERVER_PORT) { server, port ->
                this.port = port
                Log.info("PDF server is ready on port $port. Please wait...")
                try {
                    generatePdf()
                    Log.info("PDF generated successfully.")
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    Log.error("Failed to export PDF: ${e.message}")
                    Log.debug(e)
                } finally {
                    server.stop()
                }
            }
    }

    private fun generatePdf() {
        requireNotNull(port) { "PDF server port is not set" }

        val url = "http://localhost:$port/?print-pdf"

        Playwright.create().use { playwright ->
            val launchOptions =
                BrowserType
                    .LaunchOptions()
                    .setHeadless(true)
                    .setArgs(
                        mutableListOf("--disable-gpu").apply {
                            if (noSandbox) add("--no-sandbox")
                        },
                    )

            // The Java SDK does not read PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH automatically, unlike the Node.js CLI.
            System
                .getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH")
                ?.let { launchOptions.setExecutablePath(Paths.get(it)) }

            val browser: Browser = playwright.chromium().launch(launchOptions)
            try {
                val page: Page = browser.newPage()
                page.setDefaultNavigationTimeout(0.0)
                page.setDefaultTimeout(0.0)

                Log.info("Connecting to $url")
                page.navigate(url)

                Log.info("Connected. Waiting for page content.")
                page.content()

                Log.info("Connected. Waiting for page to be ready.")
                page.waitForFunction("window.isReady()")

                val body: Locator = page.locator("body")

                // Plain documents render as a single-page PDF.
                val isSinglePage = body.evaluate("el => el.classList.contains('quarkdown-plain')", null) as Boolean
                val singlePageHeightPadding = 100
                val singlePageHeightMultiplier = 1.03

                val pdfOptions =
                    Page
                        .PdfOptions()
                        .setPath(out.toPath())
                        .setPrintBackground(true)
                        .setPreferCSSPageSize(true)

                if (isSinglePage) {
                    val clientWidth = body.evaluate("el => el.clientWidth", null) as Number
                    val clientHeight = body.evaluate("el => el.clientHeight", null) as Number
                    val height =
                        clientHeight.toDouble() * singlePageHeightMultiplier + singlePageHeightPadding
                    pdfOptions.setWidth("${clientWidth}px").setHeight("${height}px")
                }

                page.pdf(pdfOptions)
            } finally {
                browser.close()
            }
        }
    }
}
