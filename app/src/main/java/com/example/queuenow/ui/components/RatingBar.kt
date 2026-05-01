package com.example.queuenow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val StarActive   = Color(0xFFF59E0B)
private val StarInactive = Color(0xFFD1D5DB)

@Composable
fun RatingBar(
    rating: Double,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 20.dp
) {
    Row(modifier = modifier) {
        repeat(maxStars) { i ->
            val idx = i + 1
            Icon(
                imageVector = when {
                    idx <= rating        -> Icons.Filled.Star
                    idx - 0.5 <= rating -> Icons.Filled.StarHalf
                    else                -> Icons.Filled.StarOutline
                },
                contentDescription = null,
                tint = if (idx - 0.5 <= rating) StarActive else StarInactive,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun InteractiveRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 40.dp
) {
    Row(modifier = modifier) {
        repeat(maxStars) { i ->
            val idx = i + 1
            Icon(
                imageVector = if (idx <= rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = "Sao $idx",
                tint = if (idx <= rating) StarActive else StarInactive,
                modifier = Modifier
                    .size(starSize)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onRatingChange(idx) }
            )
        }
    }
}