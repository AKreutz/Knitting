package com.akreutz.knitting.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.akreutz.knitting.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// Headings use Fraunces, downloaded on demand; the system font shows while it loads or if it fails.
private val Fraunces = FontFamily(
    Font(GoogleFont("Fraunces"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Fraunces"), fontProvider, FontWeight.Medium),
    Font(GoogleFont("Fraunces"), fontProvider, FontWeight.SemiBold),
)

private val BaseTypography = androidx.compose.material3.Typography()

val Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = Fraunces),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = Fraunces),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = Fraunces),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = Fraunces),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = Fraunces),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = Fraunces),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = Fraunces),
    titleMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
)
