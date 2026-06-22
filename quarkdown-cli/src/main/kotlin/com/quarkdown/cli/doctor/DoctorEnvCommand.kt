package com.quarkdown.cli.doctor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextStyles.bold

/**
 * Reports the status of external runtimes used by Quarkdown.
 */
class DoctorEnvCommand : CliktCommand("env") {
    override fun run() {
        val checks: List<EnvironmentCheck> =
            listOf(
                JvmCheck,
                PlaywrightCheck,
            )
        echo(render(checks))
    }
}

/**
 * Detects a single runtime in the environment.
 */
private interface EnvironmentCheck {
    /**
     * Name of the runtime being checked, e.g. "JVM".
     */
    val name: String

    /**
     * Performs the detection; must never throw.
     */
    fun check(): Result

    data class Result(
        val found: Boolean,
        val path: String? = null,
        val version: String? = null,
    )
}

/**
 * Base for checks whose [detect] may throw when the runtime is missing;
 * maps any exception to `found = false`.
 */
private abstract class GuardedEnvironmentCheck(
    final override val name: String,
) : EnvironmentCheck {
    final override fun check(): EnvironmentCheck.Result =
        runCatching { detect() }.getOrElse {
            EnvironmentCheck.Result(found = false)
        }

    protected abstract fun detect(): EnvironmentCheck.Result
}

/**
 * JVM the CLI is currently running on; always present.
 */
private object JvmCheck : EnvironmentCheck {
    override val name = "JVM"

    override fun check() =
        EnvironmentCheck.Result(
            found = true,
            path = System.getProperty("java.home"),
            version = Runtime.version().toString(),
        )
}

/**
 * Detects the Playwright Java API by attempting to load the Playwright class.
 */
private object PlaywrightCheck : GuardedEnvironmentCheck("Playwright") {
    override fun detect(): EnvironmentCheck.Result {
        val playwrightClass = Class.forName("com.microsoft.playwright.Playwright")
        val pkg = playwrightClass.`package`
        return EnvironmentCheck.Result(
            found = true,
            path = playwrightClass.protectionDomain.codeSource.location.toExternalForm(),
            version = pkg?.implementationVersion ?: pkg?.specificationVersion ?: "unknown",
        )
    }
}

private const val FOUND_BADGE = "✓ found"
private const val MISSING_BADGE = "✗ missing"
private const val MISSING_VALUE_PLACEHOLDER = "—"
private const val PATH_LABEL = "path:"
private const val VERSION_LABEL = "version:"

private fun render(checks: List<EnvironmentCheck>): String =
    buildString {
        checks.forEachIndexed { index, check ->
            if (index > 0) appendLine()
            val result = check.check()
            val statusBadge = if (result.found) green(FOUND_BADGE) else red(MISSING_BADGE)
            appendLine("${bold(check.name)}  $statusBadge")
            appendLine("  ${gray(PATH_LABEL)}    ${result.path ?: MISSING_VALUE_PLACEHOLDER}")
            appendLine("  ${gray(VERSION_LABEL)} ${result.version ?: MISSING_VALUE_PLACEHOLDER}")
        }
    }
