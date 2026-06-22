package com.quarkdown.rendering.html.pdf

import java.io.File

/**
 * Exports a PDF from a directory with an `index.html` root file.
 * This is done via the Playwright library, invoked through the Playwright Java API.
 * @param options options that affect the export process
 */
class HtmlPdfExporter(
    private val options: HtmlPdfExportOptions,
) {
    /**
     * Exports a PDF from the given source directory.
     * @param sourcesDirectory the directory containing the HTML source files
     * @param out the output file for the generated PDF
     */
    fun export(
        sourcesDirectory: File,
        out: File,
    ) {
        PlaywrightPdfGeneratorScript(
            sourcesDirectory,
            out,
            options.noSandbox,
        ).launch()
    }
}
