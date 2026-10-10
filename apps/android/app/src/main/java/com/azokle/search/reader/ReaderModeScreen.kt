package com.azokle.search.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val context = LocalContext.current
    val tts = remember { ReaderTextToSpeech(context) }
    val ttsState by tts.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            tts.release()
        }
    }

    val currentTheme = AzokleTheme.colors
    var selectedReaderTheme by remember {
        mutableStateOf(if (currentTheme.isDark) ReaderTheme.DARK else ReaderTheme.LIGHT)
    }

    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var useSerif by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    // Dynamic Reading Progress
    val totalItems = remember(article) {
        val count = if (article.blocks.isNotEmpty()) article.blocks.size else article.paragraphs.size
        (count + 2).coerceAtLeast(1)
    }

    val readingProgress by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val normalized = (first.toFloat() + (offset / 500f)) / totalItems.toFloat()
            normalized.coerceIn(0f, 1f)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = readingProgress,
        label = "readingProgress"
    )

    BackHandler {
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(selectedReaderTheme.bg)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {} // Consume all touches to avoid click passthrough to underlying browser views
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Reading Progress Hairline
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = if (selectedReaderTheme == ReaderTheme.SEPIA) Color(0xFF8D6E63) else AzokleTheme.colors.actionPrimaryBg,
                trackColor = selectedReaderTheme.border.copy(alpha = 0.35f)
            )

            // 2. Reader Control Top Bar (Responsive & Overflow-Safe)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(selectedReaderTheme.cardBg)
                    .border(0.8.dp, selectedReaderTheme.border)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {} // Consume header area clicks so underlying searchbox/mic/qr are never triggered
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Close & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Reader",
                            tint = selectedReaderTheme.text,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "READER",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = selectedReaderTheme.text
                    )
                }

                // Right: Typography & Theme Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Read Aloud (Text-to-Speech) Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (ttsState != ReaderTextToSpeech.State.IDLE) AzokleTheme.colors.actionPrimaryBg.copy(alpha = 0.2f) else selectedReaderTheme.bg)
                            .border(0.8.dp, selectedReaderTheme.border, RoundedCornerShape(8.dp))
                            .clickable {
                                when (ttsState) {
                                    ReaderTextToSpeech.State.IDLE -> tts.startReading(article)
                                    ReaderTextToSpeech.State.PLAYING -> tts.pause()
                                    ReaderTextToSpeech.State.PAUSED -> tts.resume()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = when (ttsState) {
                                    ReaderTextToSpeech.State.PLAYING -> Icons.Default.Pause
                                    ReaderTextToSpeech.State.PAUSED -> Icons.Default.PlayArrow
                                    ReaderTextToSpeech.State.IDLE -> Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = "Read Aloud",
                                tint = if (ttsState != ReaderTextToSpeech.State.IDLE) AzokleTheme.colors.actionPrimaryBg else selectedReaderTheme.text,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = when (ttsState) {
                                    ReaderTextToSpeech.State.PLAYING -> "Pause"
                                    ReaderTextToSpeech.State.PAUSED -> "Resume"
                                    ReaderTextToSpeech.State.IDLE -> "Listen"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ttsState != ReaderTextToSpeech.State.IDLE) AzokleTheme.colors.actionPrimaryBg else selectedReaderTheme.text
                            )
                        }
                    }

                    // Serif / Sans Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedReaderTheme.bg)
                            .border(0.8.dp, selectedReaderTheme.border, RoundedCornerShape(8.dp))
                            .clickable { useSerif = !useSerif }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (useSerif) "Serif" else "Sans",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedReaderTheme.text
                        )
                    }

                    // A- Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(selectedReaderTheme.bg)
                            .border(0.8.dp, selectedReaderTheme.border, CircleShape)
                            .clickable { if (fontSizeSp > 13f) fontSizeSp -= 2f },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A-",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedReaderTheme.text
                        )
                    }

                    // A+ Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(selectedReaderTheme.bg)
                            .border(0.8.dp, selectedReaderTheme.border, CircleShape)
                            .clickable { if (fontSizeSp < 26f) fontSizeSp += 2f },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A+",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedReaderTheme.text
                        )
                    }

                    // Theme Cycler Circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when (selectedReaderTheme) {
                                    ReaderTheme.LIGHT -> Color(0xFFFBF0D9)
                                    ReaderTheme.SEPIA -> Color(0xFF0A0A0E)
                                    ReaderTheme.DARK -> Color(0xFFFFFFFF)
                                }
                            )
                            .border(1.2.dp, selectedReaderTheme.border, CircleShape)
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

            // 3. Article Content Body
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .adaptiveContentWidth(680.dp)
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.Start,
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(selectedReaderTheme.cardBg)
                                .border(0.8.dp, selectedReaderTheme.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 3.5.dp)
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
                            .height(0.8.dp)
                            .background(selectedReaderTheme.border.copy(alpha = 0.6f))
                    )
                }

                // Optional Lead Image
                if (!article.leadImageUrl.isNullOrBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(selectedReaderTheme.cardBg)
                                .border(0.8.dp, selectedReaderTheme.border, RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(article.leadImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = article.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Structured Blocks (Headings, Blockquotes, Paragraphs)
                if (article.blocks.isNotEmpty()) {
                    items(article.blocks) { block ->
                        when (block.type) {
                            BlockType.HEADING -> {
                                Text(
                                    text = block.text,
                                    fontSize = (fontSizeSp + 3f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = selectedReaderTheme.text,
                                    fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                                    lineHeight = (fontSizeSp + 8f).sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            BlockType.BLOCKQUOTE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(selectedReaderTheme.cardBg.copy(alpha = 0.6f))
                                        .border(
                                            BorderStroke(2.5.dp, selectedReaderTheme.border),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(start = 14.dp, top = 10.dp, end = 12.dp, bottom = 10.dp)
                                ) {
                                    Text(
                                        text = block.text,
                                        fontSize = (fontSizeSp - 0.5f).sp,
                                        fontStyle = FontStyle.Italic,
                                        color = selectedReaderTheme.text.copy(alpha = 0.9f),
                                        fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                                        lineHeight = (fontSizeSp * 1.55f).sp
                                    )
                                }
                            }
                            BlockType.PARAGRAPH -> {
                                Text(
                                    text = block.text,
                                    fontSize = fontSizeSp.sp,
                                    color = selectedReaderTheme.text,
                                    fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                                    lineHeight = (fontSizeSp * 1.65f).sp,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }
                } else {
                    // Fallback to plain paragraphs
                    items(article.paragraphs) { paragraph ->
                        Text(
                            text = paragraph,
                            fontSize = fontSizeSp.sp,
                            color = selectedReaderTheme.text,
                            fontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif,
                            lineHeight = (fontSizeSp * 1.65f).sp,
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}
