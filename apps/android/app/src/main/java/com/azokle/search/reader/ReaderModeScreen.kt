package com.azokle.search.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

enum class ReaderTheme(
    val bg: Color,
    val text: Color,
    val cardBg: Color,
    val border: Color
) {
    LIGHT(Color(0xFFFFFFFF), Color(0xFF1F2328), Color(0xFFF6F8FA), Color(0xFFD0D7DE)),
    SEPIA(Color(0xFFFBF0D9), Color(0xFF433422), Color(0xFFF4E4C1), Color(0xFFDECBA4)),
    DARK(Color(0xFF0A0A0E), Color(0xFFEDEDED), Color(0xFF14141A), Color(0xFF27272A))
}

@Composable
fun ReaderModeScreen(
    article: ArticleContent,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = AzokleTheme.colors
    var selectedReaderTheme by remember {
        mutableStateOf(if (currentTheme.isDark) ReaderTheme.DARK else ReaderTheme.LIGHT)
    }

    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var useSerif by remember { mutableStateOf(true) }

    BackHandler {
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(selectedReaderTheme.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Reader Control Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(selectedReaderTheme.cardBg)
                    .border(1.dp, selectedReaderTheme.border)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Reader",
                            tint = selectedReaderTheme.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "READER MODE",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = selectedReaderTheme.text
                    )
                }

                // Controls: Font Size, Serif/Sans, Color Theme
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Serif / Sans Toggle
                    Box(
                        modifier = Modifier
                            .sizeIn(minHeight = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedReaderTheme.bg)
                            .border(1.dp, selectedReaderTheme.border, RoundedCornerShape(8.dp))
                            .clickable { useSerif = !useSerif }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (useSerif) "Serif" else "Sans",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedReaderTheme.text
                        )
                    }

                    // Smaller Font (36dp Touch Target)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(selectedReaderTheme.bg)
                            .border(1.dp, selectedReaderTheme.border, CircleShape)
                            .clickable { if (fontSizeSp > 13f) fontSizeSp -= 2f },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedReaderTheme.text
                        )
                    }

                    // Larger Font (36dp Touch Target)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(selectedReaderTheme.bg)
                            .border(1.dp, selectedReaderTheme.border, CircleShape)
                            .clickable { if (fontSizeSp < 26f) fontSizeSp += 2f },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A+",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedReaderTheme.text
                        )
                    }

                    // Theme Cycler (36dp Touch Target)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when (selectedReaderTheme) {
                                    ReaderTheme.LIGHT -> Color(0xFFFBF0D9)
                                    ReaderTheme.SEPIA -> Color(0xFF0A0A0E)
                                    ReaderTheme.DARK -> Color(0xFFFFFFFF)
                                }
                            )
                            .border(1.dp, selectedReaderTheme.border, CircleShape)
                            .clickable {
                                selectedReaderTheme = when (selectedReaderTheme) {
                                    ReaderTheme.LIGHT -> ReaderTheme.SEPIA
                                    ReaderTheme.SEPIA -> ReaderTheme.DARK
                                    ReaderTheme.DARK -> ReaderTheme.LIGHT
                                }
                            }
                    )
                }
            }

            // 2. Article Content Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .adaptiveContentWidth(700.dp)
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Headline
                item {
                    Text(
                        text = article.title,
                        fontSize = (fontSizeSp + 7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = selectedReaderTheme.text,
                        fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                        lineHeight = (fontSizeSp + 13f).sp
                    )
                }

                // Byline & Read Time Pill
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (article.byline.isNotBlank()) {
                            Text(
                                text = article.byline,
                                fontSize = 12.sp,
                                color = selectedReaderTheme.text.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedReaderTheme.cardBg)
                                .border(1.dp, selectedReaderTheme.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⏱️ ${article.estimatedMinutes} min read",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = selectedReaderTheme.text.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(selectedReaderTheme.border)
                    )
                }

                // Paragraphs
                items(article.paragraphs) { paragraph ->
                    Text(
                        text = paragraph,
                        fontSize = fontSizeSp.sp,
                        color = selectedReaderTheme.text,
                        fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                        lineHeight = (fontSizeSp * 1.6f).sp,
                        letterSpacing = 0.2.sp
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}
