package com.whyy.snapnotes.util

import android.os.Build
import java.util.Locale

object DeviceInfo {
    private val mtkModelRegex = Regex("""\bmt\d{4}[a-z]*\b""")

    val isMediaTekDevice: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val text = buildString {
            append(Build.SOC_MANUFACTURER).append(' ')
            append(Build.SOC_MODEL).append(' ')
            append(Build.HARDWARE).append(' ')
            append(Build.BOARD)
        }.lowercase(Locale.US)

        "mediatek" in text ||
                "mtk" in text ||
                mtkModelRegex.containsMatchIn(text)
    }
}