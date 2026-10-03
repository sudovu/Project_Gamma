package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@Composable
fun GammaSectionHeader(
    category: String,
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = GammaPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = GammaTextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (secondaryActionText != null && onSecondaryActionClick != null) {
                if (secondaryActionText.equals("Play", ignoreCase = true)) {
                    Surface(
                        onClick = onSecondaryActionClick,
                        shape = RoundedCornerShape(16.dp),
                        color = GammaPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = GammaBackground,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Play",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GammaBackground
                            )
                        }
                    }
                } else {
                    Text(
                        text = secondaryActionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = GammaPrimary,
                        modifier = Modifier
                            .clickable(onClick = onSecondaryActionClick)
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }
            }

            if (actionText != null && onActionClick != null) {
                if (actionText.equals("Shuffle", ignoreCase = true)) {
                    Surface(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(16.dp),
                        color = GammaPrimary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, GammaPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = GammaPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GammaPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = GammaPrimary,
                        modifier = Modifier
                            .clickable(onClick = onActionClick)
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }
            }
        }
    }
}
