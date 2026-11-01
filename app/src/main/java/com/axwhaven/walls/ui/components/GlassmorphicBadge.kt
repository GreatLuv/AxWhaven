package com.axwhaven.walls.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.GlassText
import com.axwhaven.walls.ui.theme.LocalAxWhavenColors

@Composable
fun GlassmorphicBadge(
    text: String,
    modifier: Modifier = Modifier,
    colors: AxWhavenColors = LocalAxWhavenColors.current,
    textColor: Color = GlassText
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glassFill)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Shared Material 3 Expressive Header Count Badge.
 * Styled with secondaryContainer background, 6.dp primary circle dot, and onSecondaryContainer text.
 * Preserves frosted acrylic and mint text in Frosted Glass mode.
 */
@Composable
fun ArtsCountHeaderBadge(
    text: String,
    modifier: Modifier = Modifier,
    colors: AxWhavenColors = LocalAxWhavenColors.current
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (colors.isGlassmorphism) colors.mintPillBg
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(1.dp, colors.mintPillBorder, RoundedCornerShape(20.dp))
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (colors.isGlassmorphism) colors.mintText
                        else MaterialTheme.colorScheme.primary
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (colors.isGlassmorphism) colors.mintText else MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
