package com.lovishraheja27.helparticles.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        markdown.lines().forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                line.trim().startsWith("- ") -> {
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = parseInlineFormatting(line.trim().removePrefix("- ")),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                line.trim().matches(Regex("\\d+\\. .*")) -> {
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        val number = line.trim().substringBefore(".")
                        Text("$number. ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = parseInlineFormatting(
                                line.trim().substringAfter(". ")
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                line.trim().startsWith("```") -> {
                    // Skip code fence lines
                }

                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                else -> {
                    Text(
                        text = parseInlineFormatting(line),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun parseInlineFormatting(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0

        // Simple bold parsing (** or __)
        val boldRegex = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__")
        boldRegex.findAll(text).forEach { match ->
            // Add text before match
            append(text.substring(currentIndex, match.range.first))

            // Add bold text
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1].ifEmpty { match.groupValues[2] })
            }

            currentIndex = match.range.last + 1
        }

        // Add remaining text
        append(text.substring(currentIndex))
    }
}