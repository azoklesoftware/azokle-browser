/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.common.util

import com.azokle.search.common.util.LocaleHelper
import java.util.Locale

data class LanguageConfig(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flag: String,
    val isAuto: Boolean = false
)

private val RAW_LANGUAGES = listOf(
    "system" to "🌐",
    "en" to "🇬🇧",
    "fr" to "🇫🇷",
    "es" to "🇪🇸",
    "de" to "🇩🇪",
    "zh-rCN" to "🇨🇳",
    "zh-rTW" to "🇹🇼",
    "ja" to "🇯🇵",
    "ko" to "🇰🇷",
    "hi" to "🇮🇳",
    "ar" to "🇸🇦",
    "pt-rBR" to "🇧🇷",
    "pt-rPT" to "🇵🇹",
    "ru" to "🇷🇺",
    "it" to "🇮🇹",
    "nl" to "🇳🇱",
    "tr" to "🇹🇷",
    "pl" to "🇵🇱",
    "uk" to "🇺🇦",
    "id" to "🇮🇩",
    "vi" to "🇻🇳",
    "th" to "🇹🇭",
    "sv" to "🇸🇪",
    "da" to "🇩🇰",
    "fi" to "🇫🇮",
    "no" to "🇳🇴",
    "cs" to "🇨🇿",
    "el" to "🇬🇷",
    "he" to "🇮🇱",
    "ro" to "🇷🇴",
    "hu" to "🇭🇺",
    "bn" to "🇧🇩",
    "ta" to "🇮🇳",
    "te" to "🇮🇳",
    "mr" to "🇮🇳",
    "gu" to "🇮🇳",
    "kn" to "🇮🇳",
    "ml" to "🇮🇳",
    "pa" to "🇮🇳",
    "ur" to "🇵🇰",
    "fa" to "🇮🇷",
    "fil" to "🇵🇭",
    "ms" to "🇲🇾",
    "sw" to "🇹🇿",
    "am" to "🇪🇹",
    "yo" to "🇳🇬",
    "ig" to "🇳🇬",
    "ha" to "🇳🇬",
    "zu" to "🇿🇦",
    "bg" to "🇧🇬",
    "sk" to "🇸🇰",
    "hr" to "🇭🇷",
    "sr" to "🇷🇸",
    "sl" to "🇸🇮",
    "lt" to "🇱🇹",
    "lv" to "🇱🇻",
    "et" to "🇪🇪",
    "ca" to "🇪🇸",
    "eu" to "🇪🇸",
    "gl" to "🇪🇸",
    "is" to "🇮🇸",
    "ga" to "🇮🇪",
    "cy" to "🇬🇧",
    "af" to "🇿🇦",
    "sq" to "🇦🇱",
    "mk" to "🇲🇰",
    "bs" to "🇧🇦",
    "az" to "🇦🇿",
    "ka" to "🇬🇪",
    "hy" to "🇦🇲",
    "kk" to "🇰🇿",
    "uz" to "🇺🇿",
    "mn" to "🇲🇳",
    "my" to "🇲🇲",
    "km" to "🇰🇭",
    "lo" to "🇱🇦",
    "si" to "🇱🇰",
    "ne" to "🇳🇵",
    "so" to "🇸🇴",
    "rw" to "🇷🇼",
    "mg" to "🇲🇬",
    "sn" to "🇿🇼",
    "ny" to "🇲🇼",
    "st" to "🇱🇸",
    "tn" to "🇧🇼",
    "xh" to "🇿🇦",
    "wo" to "🇸🇳",
    "ff" to "🇸🇳",
    "ti" to "🇪🇷",
    "om" to "🇪🇹",
    "tg" to "🇹🇯",
    "ky" to "🇰🇬",
    "tk" to "🇹🇲",
    "ps" to "🇦🇫",
    "ku" to "🇮🇶",
    "sd" to "🇵🇰",
    "as" to "🇮🇳",
    "or" to "🇮🇳",
    "sa" to "🇮🇳",
    "bo" to "🇨🇳",
    "ug" to "🇨🇳",
    "dz" to "🇧🇹",
    "mt" to "🇲🇹",
    "lb" to "🇱🇺",
    "fo" to "🇫🇴",
    "fy" to "🇳🇱",
    "gd" to "🇬🇧",
    "br" to "🇫🇷",
    "co" to "🇫🇷",
    "oc" to "🇫🇷",
    "sc" to "🇮🇹",
    "rm" to "🇨🇭",
    "fur" to "🇮🇹",
    "yi" to "🇮🇱",
    "jv" to "🇮🇩",
    "su" to "🇮🇩",
    "min" to "🇮🇩",
    "ceb" to "🇵🇭",
    "ilo" to "🇵🇭",
    "hil" to "🇵🇭",
    "war" to "🇵🇭"
)

val SUPPORTED_LANGUAGES: List<LanguageConfig> by lazy {
    RAW_LANGUAGES.map { (code, flag) ->
        if (code == "system") {
            LanguageConfig(
                code = "system",
                nativeName = "Default (System)",
                englishName = "System Default",
                flag = flag,
                isAuto = true
            )
        } else {
            val locale = LocaleHelper.getLocale(code)
            val native = locale.getDisplayName(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            val english = locale.getDisplayName(Locale.ENGLISH).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            LanguageConfig(
                code = code,
                nativeName = if (native.isNotBlank()) native else code,
                englishName = if (english.isNotBlank()) english else code,
                flag = flag,
                isAuto = false
            )
        }
    }
}
