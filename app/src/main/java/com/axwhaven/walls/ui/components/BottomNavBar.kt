package com.axwhaven.walls.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.axwhaven.walls.model.PrimaryAppTab
import com.axwhaven.walls.ui.theme.AxWhavenColors

@Composable
fun AxWhavenBottomNavBar(
    currentTab: PrimaryAppTab,
    colors: AxWhavenColors,
    onTabSelect: (PrimaryAppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val navBackgroundBrush = if (colors.isGlassmorphism) {
            Brush.verticalGradient(
                listOf(
                    Color(0x38FFFFFF), // Top specular reflection on frosted acrylic
                    Color(0x1C182238)  // Translucent dark glass body
                )
            )
        } else {
            Brush.linearGradient(listOf(colors.bottomNavBg, colors.bottomNavBg))
        }

        val navBorderBrush = if (colors.isGlassmorphism) {
            Brush.verticalGradient(
                listOf(
                    Color(0x8CFFFFFF), // Crisp top light highlight on glass rim (55% white)
                    Color(0x28FFFFFF)  // Subtle bottom glass rim (16% white)
                )
            )
        } else {
            Brush.linearGradient(listOf(colors.bottomNavBorder, colors.bottomNavBorder))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (colors.isGlassmorphism) 16.dp else 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = if (colors.isGlassmorphism) colors.primaryAccent.copy(alpha = 0.35f) else colors.primaryAccent.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(navBackgroundBrush)
                .border(1.dp, navBorderBrush, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalTabs = 3
            val tabWidth = maxWidth / totalTabs
            val activeTabIndex = currentTab.ordinal

            val animatedTabIndex by animateFloatAsState(
                targetValue = activeTabIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "NavSlidingPillOffset"
            )

            val activePillColor = if (colors.isGlassmorphism) {
                colors.primaryAccent.copy(alpha = 0.22f)
            } else {
                colors.primaryAccent.copy(alpha = 0.14f)
            }

            // Smooth horizontal sliding indicator pill across the 3 bottom tabs
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(tabWidth)
                    .offset {
                        IntOffset(
                            x = (animatedTabIndex * tabWidth.toPx()).roundToInt(),
                            y = 0
                        )
                    }
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(activePillColor)
                    .then(
                        if (colors.isGlassmorphism) {
                            Modifier.border(
                                0.8.dp,
                                Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x20FFFFFF))),
                                RoundedCornerShape(26.dp)
                            )
                        } else {
                            Modifier.border(
                                0.8.dp,
                                colors.primaryAccent.copy(alpha = 0.3f),
                                RoundedCornerShape(26.dp)
                            )
                        }
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                NavBarItem(
                    tab = PrimaryAppTab.HOME,
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentTab == PrimaryAppTab.HOME,
                    colors = colors,
                    onClick = { onTabSelect(PrimaryAppTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                NavBarItem(
                    tab = PrimaryAppTab.HAVEN_SURF,
                    icon = Icons.Default.Explore,
                    label = "AxW Surf",
                    isSelected = currentTab == PrimaryAppTab.HAVEN_SURF,
                    colors = colors,
                    onClick = { onTabSelect(PrimaryAppTab.HAVEN_SURF) },
                    modifier = Modifier.weight(1f)
                )

                NavBarItem(
                    tab = PrimaryAppTab.SETTINGS,
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isSelected = currentTab == PrimaryAppTab.SETTINGS,
                    colors = colors,
                    onClick = { onTabSelect(PrimaryAppTab.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NavBarItem(
    tab: PrimaryAppTab,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    colors: AxWhavenColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryAccent else colors.textMuted,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "NavIconColor"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryAccent else colors.textMuted,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "NavTextColor"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "NavIconScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.primaryAccent),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedIconColor,
                modifier = Modifier
                    .size(21.dp)
                    .scale(iconScale)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = animatedTextColor,
                maxLines = 1
            )
        }
    }
}
