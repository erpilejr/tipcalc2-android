package dev.harness.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val TIP_LOW = 10
private const val TIP_DEFAULT = 15
private const val TIP_HIGH = 20
private const val PERCENT = 100
private const val CENTS = 2
private val TIPS = listOf(TIP_LOW, TIP_DEFAULT, TIP_HIGH)

/** Plain model of the calculator's amounts. */
data class Amounts(val tip: BigDecimal, val total: BigDecimal, val each: BigDecimal)

fun calculate(billText: String, tipPercent: Int, people: Int = 1): Amounts {
    val parsed = billText.trim().toBigDecimalOrNull()
    val bill = parsed?.takeIf { it.signum() >= 0 } ?: BigDecimal.ZERO
    val tip = bill.multiply(BigDecimal(tipPercent))
        .divide(BigDecimal(PERCENT), CENTS, RoundingMode.HALF_UP)
    val total = bill.setScale(CENTS, RoundingMode.HALF_UP).add(tip)
    val each = total.divide(BigDecimal(people), CENTS, RoundingMode.HALF_UP)
    return Amounts(tip, total, each)
}

fun formatNzd(amount: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-NZ"))
    format.currency = java.util.Currency.getInstance("NZD")
    format.minimumFractionDigits = CENTS
    format.maximumFractionDigits = CENTS
    return format.format(amount)
}

@Composable
fun AppRoot() {
    var bill by remember { mutableStateOf("") }
    var tipPercent by remember { mutableStateOf(TIP_DEFAULT) }
    BillAndTipScreen(
        bill = bill,
        tipPercent = tipPercent,
        onBillChange = { bill = it },
        onTipChange = { tipPercent = it }
    )
}

@Composable
fun BillAndTipScreen(
    bill: String,
    tipPercent: Int,
    onBillChange: (String) -> Unit,
    onTipChange: (Int) -> Unit
) {
    val amounts = calculate(bill, tipPercent)
    Column(Modifier.padding(16.dp)) {
        TextField(
            value = bill,
            onValueChange = onBillChange,
            modifier = Modifier.testTag("bill-input")
        )
        Row {
            TIPS.forEach { pct ->
                Button(onClick = { onTipChange(pct) }, modifier = Modifier.testTag("tip-$pct")) {
                    Text("$pct%")
                }
            }
        }
        Text("Tip: $tipPercent%", Modifier.testTag("tip-label"))
        Text(formatNzd(amounts.tip), Modifier.testTag("tip-amount"))
        Text(formatNzd(amounts.total), Modifier.testTag("total-amount"))
        Text(formatNzd(amounts.each), Modifier.testTag("each-amount"))
    }
}
