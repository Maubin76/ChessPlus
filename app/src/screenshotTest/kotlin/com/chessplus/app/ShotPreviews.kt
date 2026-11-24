package com.chessplus.app

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Appareil de référence des captures : Galaxy S24 (1080 × 2340 px, 384 × 832 dp). */
const val kDevice = "spec:width=384dp,height=832dp,dpi=450"

/**
 * Capture un écran en clair et en sombre sur l'appareil de référence.
 */
@Preview(name = "Clair", device = kDevice, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "Sombre", device = kDevice, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class DesignPreviews
