package com.azokle.search.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleTheme

data class OssLicenseItem(
    val name: String,
    val licenseType: String,
    val websiteUrl: String,
    val notice: String
)

private val OSS_COMPONENTS = listOf(
    OssLicenseItem(
        name = "Mozilla GeckoView",
        licenseType = "MPL 2.0",
        websiteUrl = "https://geckoview.dev",
        notice = "Mozilla Public License Version 2.0\n\nThis Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/."
    ),
    OssLicenseItem(
        name = "Tor / Onionmasq",
        licenseType = "BSD-3-Clause",
        websiteUrl = "https://www.torproject.org",
        notice = "Copyright (c) The Tor Project, Inc.\nCopyright (c) The Guardian Project\n\nRedistribution and use in source and binary forms, with or without modification, are permitted provided that the copyright notice and disclaimer are retained."
    ),
    OssLicenseItem(
        name = "Android Jetpack & Compose",
        licenseType = "Apache 2.0",
        websiteUrl = "https://developer.android.com/jetpack/compose",
        notice = "Copyright (C) The Android Open Source Project\n\nLicensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "Material Components",
        licenseType = "Apache 2.0",
        websiteUrl = "https://m3.material.io",
        notice = "Copyright (C) Google LLC\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "Kotlin & Coroutines",
        licenseType = "Apache 2.0",
        websiteUrl = "https://kotlinlang.org",
        notice = "Copyright 2010-2025 JetBrains s.r.o. and Kotlin contributors\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "OkHttp",
        licenseType = "Apache 2.0",
        websiteUrl = "https://square.github.io/okhttp",
        notice = "Copyright 2019 Square, Inc.\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "Coil",
        licenseType = "Apache 2.0",
        websiteUrl = "https://coil-kt.github.io/coil",
        notice = "Copyright 2024 Coil Contributors\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "CameraX",
        licenseType = "Apache 2.0",
        websiteUrl = "https://developer.android.com/training/camerax",
        notice = "Copyright (C) 2019 The Android Open Source Project\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "ZXing",
        licenseType = "Apache 2.0",
        websiteUrl = "https://github.com/zxing/zxing",
        notice = "Copyright (C) 2008 ZXing authors\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "AndroidX Security Crypto",
        licenseType = "Apache 2.0",
        websiteUrl = "https://developer.android.com/topic/security/data",
        notice = "Copyright (C) 2019 The Android Open Source Project\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "AndroidX Credentials",
        licenseType = "Apache 2.0",
        websiteUrl = "https://developer.android.com/training/sign-in/credential-manager",
        notice = "Copyright (C) 2022 The Android Open Source Project\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "AndroidX Media",
        licenseType = "Apache 2.0",
        websiteUrl = "https://developer.android.com/guide/topics/media",
        notice = "Copyright (C) 2018 The Android Open Source Project\n\nLicensed under the Apache License, Version 2.0.\nhttp://www.apache.org/licenses/LICENSE-2.0"
    ),
    OssLicenseItem(
        name = "uBlock Origin & EasyList",
        licenseType = "GPL 3.0",
        websiteUrl = "https://github.com/uBlockOrigin/uBlock-issues",
        notice = "GNU General Public License v3.0\n\nCopyright (C) Raymond Hill (gorhill) and EasyList contributors.\nThis program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 3."
    ),
    OssLicenseItem(
        name = "Tokio",
        licenseType = "MIT",
        websiteUrl = "https://tokio.rs",
        notice = "Copyright (c) Tokio Contributors\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction under the terms of the MIT License."
    ),
    OssLicenseItem(
        name = "RustCrypto",
        licenseType = "Apache 2.0 / MIT",
        websiteUrl = "https://github.com/RustCrypto",
        notice = "Copyright (c) RustCrypto Project Developers\n\nLicensed under either of Apache License, Version 2.0 or MIT license at your option."
    ),
    OssLicenseItem(
        name = "Serde",
        licenseType = "MIT / Apache 2.0",
        websiteUrl = "https://serde.rs",
        notice = "Copyright (c) Erick Tryzelaar and David Tolnay\n\nLicensed under either of Apache License, Version 2.0 or MIT license at your option."
    ),
    OssLicenseItem(
        name = "Aho-Corasick & Regex",
        licenseType = "MIT",
        websiteUrl = "https://github.com/BurntSushi/aho-corasick",
        notice = "Copyright (c) Andrew Gallant (burntsushi)\n\nDedicated to the public domain or licensed under the MIT License."
    ),
    OssLicenseItem(
        name = "Azokle Core",
        licenseType = "MIT / Apache 2.0",
        websiteUrl = "https://azokle.com",
        notice = "Copyright (c) 2024-2026 The Azokle Authors\n\nLicensed under either of Apache License, Version 2.0 or MIT license at your option."
    )
)

/**
 * Clean, lightweight, and minimalist Open Source Licenses Screen.
 * Adheres to the unified Brave/Azokle card aesthetic.
 */
@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    var searchQuery by remember { mutableStateOf("") }

    val filteredComponents = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            OSS_COMPONENTS
        } else {
            val q = searchQuery.trim().lowercase()
            OSS_COMPONENTS.filter {
                it.name.lowercase().contains(q) ||
                it.licenseType.lowercase().contains(q)
            }
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Open source licenses",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sleek Minimalist Search Bar
            val searchShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(searchShape)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.border.copy(alpha = 0.5f), searchShape)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textMuted,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search licenses...",
                            fontSize = 14.sp,
                            color = colors.textMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        ),
                        cursorBrush = SolidColor(colors.statusAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Unified Grouped Card Container
            val cardShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(cardShape)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.border.copy(alpha = 0.6f), cardShape)
            ) {
                if (filteredComponents.isEmpty()) {
                    Text(
                        text = "No licenses found",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                } else {
                    filteredComponents.forEachIndexed { index, item ->
                        OssLicenseRow(
                            item = item,
                            onClose = onClose,
                            onOpenUrl = onOpenUrl
                        )

                        if (index < filteredComponents.lastIndex) {
                            HorizontalDivider(
                                color = colors.border.copy(alpha = 0.45f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OssLicenseRow(
    item: OssLicenseItem,
    onClose: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.12f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                }
            )
            .semantics {
                role = Role.Button
                contentDescription = "${item.name}, ${item.licenseType}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.licenseType,
                fontSize = 13.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isExpanded) 180f else 0f)
            )
        }

        // Clean Inline License Notice & Website Link
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(
                    text = item.notice,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusAccent.copy(alpha = 0.15f)),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClose()
                                onOpenUrl(item.websiteUrl)
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visit project website",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.statusAccent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open ${item.name} website",
                        tint = colors.statusAccent,
                        modifier = Modifier.size(13.5.dp)
                    )
                }
            }
        }
    }
}
