package bendev.addblock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import bendev.addblock.R

val Jakarta = FontFamily(
    Font(R.font.jakarta_400, FontWeight.Normal),
    Font(R.font.jakarta_500, FontWeight.Medium),
    Font(R.font.jakarta_600, FontWeight.SemiBold),
    Font(R.font.jakarta_700, FontWeight.Bold),
    Font(R.font.jakarta_800, FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.jakarta(weight: FontWeight? = null) =
    copy(fontFamily = Jakarta, fontWeight = weight ?: fontWeight)

val Typography = Typography(
    displayLarge = base.displayLarge.jakarta(FontWeight.ExtraBold),
    displayMedium = base.displayMedium.jakarta(FontWeight.ExtraBold),
    displaySmall = base.displaySmall.jakarta(FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.jakarta(FontWeight.Bold),
    headlineMedium = base.headlineMedium.jakarta(FontWeight.Bold),
    headlineSmall = base.headlineSmall.jakarta(FontWeight.Bold),
    titleLarge = base.titleLarge.jakarta(FontWeight.Bold),
    titleMedium = base.titleMedium.jakarta(FontWeight.SemiBold),
    titleSmall = base.titleSmall.jakarta(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.jakarta(),
    bodyMedium = base.bodyMedium.jakarta(),
    bodySmall = base.bodySmall.jakarta(),
    labelLarge = base.labelLarge.jakarta(FontWeight.SemiBold),
    labelMedium = base.labelMedium.jakarta(FontWeight.SemiBold),
    labelSmall = base.labelSmall.jakarta(FontWeight.Medium),
)
