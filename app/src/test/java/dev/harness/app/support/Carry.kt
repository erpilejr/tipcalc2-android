package dev.harness.app.support

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.Locale
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule

/**
 * A restart on Android is two test classes, run one after the other, each in a fresh JVM: phase 1 saves the app's
 * data folder here, phase 2 puts it back before the app starts. Memory does not come across: it is a new process.
 * Phase 2 restores in an outer rule (`restoreRule`), before the compose rule starts the activity: see `Scenario`.
 */
object Carry {
    private val root = File("/tmp/harness-carry")

    private fun marker(scenario: String, phase: Int) = File(root, "$scenario.phase$phase")

    private fun clearMarkers(scenario: String) {
        root.listFiles { f -> f.name.startsWith("$scenario.phase") }?.forEach { it.delete() }
    }

    fun save(scenario: String, phase: Int, ctx: Context) {
        root.mkdirs()
        if (phase == 1) clearMarkers(scenario)
        val saved = File(root, scenario)
        saved.deleteRecursively()
        ctx.dataDir.copyRecursively(saved, overwrite = true)
        marker(scenario, phase).writeText("done")
    }

    fun restore(scenario: String, phase: Int, ctx: Context) {
        check(marker(scenario, phase - 1).exists()) {
            "phase ${phase - 1} of $scenario did not run first"
        }
        File(root, scenario).copyRecursively(ctx.dataDir, overwrite = true)
    }
}

/** Sets the default locale to the scenario's region (`en-US`, `de-DE`) for one test, then puts the old one back. */
fun regionRule(region: String): TestRule = object : ExternalResource() {
    private val before: Locale = Locale.getDefault()

    override fun before() = Locale.setDefault(Locale.forLanguageTag(region.replace('_', '-')))

    override fun after() = Locale.setDefault(before)
}

/** Puts phase `phase - 1`'s saved data back; use it as a rule before the one that starts the activity. */
fun restoreRule(scenario: String, phase: Int): TestRule = object : ExternalResource() {
    override fun before() {
        Carry.restore(scenario, phase, ApplicationProvider.getApplicationContext())
    }
}
