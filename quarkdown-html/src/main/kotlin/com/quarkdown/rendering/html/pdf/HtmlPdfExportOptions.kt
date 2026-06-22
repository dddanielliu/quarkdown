package com.quarkdown.rendering.html.pdf

import java.io.File

/**
 * Options for exporting PDF files from HTML via Playwright.
 * @param outputDirectory the output directory for the PDF file
 * @param noSandbox whether to disable Chrome sandbox for PDF export from HTML. Potentially unsafe
 */
data class HtmlPdfExportOptions(
    val outputDirectory: File,
    val noSandbox: Boolean = false,
)
