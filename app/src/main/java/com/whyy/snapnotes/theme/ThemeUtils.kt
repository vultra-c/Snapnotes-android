package com.whyy.snapnotes.theme

import androidx.compose.runtime.Composable
import com.nevoit.glasense.theme.LocalDarkTheme

@Composable
fun isAppInDarkTheme(): Boolean = LocalDarkTheme.current
