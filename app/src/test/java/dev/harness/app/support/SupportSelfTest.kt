package dev.harness.app.support

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The harness's own check of its support code. It does not start the app, so it passes whatever the builder wrote. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SupportSelfTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun carryPutsSavedDataBackAndRefusesAPhaseThatDidNotRunFirst() {
        val name = "self-test-carry"
        File(ctx.filesDir, "note.txt").writeText("milk")
        ctx.getSharedPreferences("p", Context.MODE_PRIVATE).edit().putString("k", "saved").commit()
        Carry.save(name, 1, ctx)
        File(ctx.filesDir, "note.txt").delete()
        ctx.getSharedPreferences("p", Context.MODE_PRIVATE).edit().clear().commit()
        assertFalse(File(ctx.filesDir, "note.txt").exists())
        Carry.restore(name, 2, ctx)
        assertEquals("milk", File(ctx.filesDir, "note.txt").readText())
        assertTrue(File(ctx.dataDir, "shared_prefs/p.xml").readText().contains("saved"))
        val error = assertThrows(IllegalStateException::class.java) { Carry.restore(name, 3, ctx) }
        assertEquals("phase 2 of $name did not run first", error.message)
        Carry.save(name, 1, ctx)
        assertThrows(IllegalStateException::class.java) { Carry.restore(name, 3, ctx) }
    }

    @Test
    fun regionRuleSetsTheLocaleForTheTestAndPutsTheOldOneBack() {
        val before = Locale.getDefault()
        val rule = regionRule("en-NZ")
        val statement = object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                assertEquals("$12.50", NumberFormat.getCurrencyInstance().format(12.5))
                assertEquals("NZ", Locale.getDefault().country)
            }
        }
        rule.apply(statement, org.junit.runner.Description.EMPTY).evaluate()
        assertEquals(before, Locale.getDefault())
    }
}
