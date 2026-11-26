package dev.harness.app.support

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dev.harness.app.MainActivity
import java.util.Locale

/**
 * The steps a scenario test is made of, with the harness's waits: each one waits up to five seconds. Elements are
 * found by test tag (`Modifier.testTag(id)`).
 *
 * The rules go in this order, so the region and the restored data are in place before the activity starts:
 *
 *     private val compose = createAndroidComposeRule<MainActivity>()
 *
 *     @get:Rule
 *     val rules: RuleChain = RuleChain.outerRule(regionRule("en-US"))
 *         .around(restoreRule("S01", 2)) // only in a restart's second class
 *         .around(compose)
 *
 * A restart is two test classes (`S01a`, `S01b`), each in its own JVM: the first ends with
 * `Carry.save("S01", 1, compose.activity)`. A step fails when the default locale is not the scenario's region,
 * so a test without `regionRule` cannot pass by accident.
 */
class Scenario(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
    val region: String
) {
    private fun node(id: String): SemanticsNodeInteraction =
        rule.onNodeWithTag(id, useUnmergedTree = false)

    private fun present(id: String) =
        rule.onAllNodesWithTag(id, useUnmergedTree = false).fetchSemanticsNodes().isNotEmpty()

    private fun checkRegion() {
        val tag = Locale.getDefault().toLanguageTag()
        if (tag != region.replace('_', '-')) {
            throw AssertionError("region is $region but the default locale is $tag: add regionRule")
        }
    }

    private fun until(what: () -> String, condition: () -> Boolean) {
        checkRegion()
        runCatching { rule.waitUntil(WAIT_MS, condition) }
            .getOrElse { throw AssertionError("${what()} within 5 s", it) }
    }

    /** Waits for the element and brings it into view when it sits in a scrollable. */
    private fun ready(id: String): SemanticsNodeInteraction {
        until({ "expected $id to exist" }) { present(id) }
        // performScrollTo fails on an element that has no scrollable parent: it is on screen already then.
        runCatching { node(id).performScrollTo() }
        return node(id)
    }

    fun tap(id: String) {
        ready(id).performClick()
    }

    fun type(id: String, text: String) {
        ready(id).performTextInput(text)
    }

    fun clear(id: String) {
        ready(id).performTextClearance()
    }

    fun expectShows(id: String, text: String) {
        until({ "expected $id to exist" }) { present(id) }
        val want = normalised(text)
        var seen = ""
        until({ "expected $id to show \"$want\" but it shows \"$seen\"" }) {
            seen = normalised(shown(node(id).fetchSemanticsNode()))
            seen == want
        }
    }

    fun expectExists(id: String) = until({ "expected $id to exist" }) { present(id) }

    fun expectGone(id: String) = until({ "expected $id to be gone" }) { !present(id) }

    /** What the element shows: a text field's content, else its text. */
    private fun shown(n: SemanticsNode): String {
        val config = n.config
        return config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            ?: ""
    }

    /** No-break and narrow no-break spaces (a locale's currency format uses them) read as an ordinary space. */
    private fun normalised(text: String) = text.replace('\u00A0', ' ').replace('\u202F', ' ')

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
