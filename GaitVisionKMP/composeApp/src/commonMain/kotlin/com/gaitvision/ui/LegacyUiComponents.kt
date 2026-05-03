package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mirrors [layout_common_header.xml]: back control, centered title (22sp bold white),
 * optional subtitle (18sp [AppColors.TableHeaderText], visibility toggled when null).
 */
@Composable
fun CommonScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: () -> Unit,
    endContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = AppColors.TextWhite
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = if (endContent != null) 0.dp else 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = AppColors.TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = AppColors.TableHeaderText,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (endContent != null) {
            Box(
                modifier = Modifier.padding(start = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                endContent()
            }
        }
    }
}

enum class DashboardNavTab {
    Home, Help, Info, Settings
}

/**
 * Mirrors [layout_bottom_nav.xml]: dark footer bar, four vertical icon+label items.
 */
@Composable
fun LegacyDashboardBottomNav(
    selectedTab: DashboardNavTab,
    onHome: () -> Unit,
    onHelp: () -> Unit,
    onInfo: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.CardSurfaceDark)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = selectedTab == DashboardNavTab.Home,
            onClick = onHome,
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            icon = Icons.Default.HelpOutline,
            label = "Help",
            selected = selectedTab == DashboardNavTab.Help,
            onClick = onHelp,
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            icon = Icons.Default.Info,
            label = "Info",
            selected = selectedTab == DashboardNavTab.Info,
            onClick = onInfo,
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = selectedTab == DashboardNavTab.Settings,
            onClick = onSettings,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) AppColors.PrimaryBlue else AppColors.TextTertiary
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = tint, fontSize = 10.sp)
    }
}

/**
 * Mirrors [item_recent_patient.xml]: elevated dark card, name + detail line, chevron.
 */
@Composable
fun RecentPatientRowCard(
    patientName: String,
    detailLine: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AppColors.CardSurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextWhite
                )
                Text(
                    text = detailLine,
                    fontSize = 14.sp,
                    color = AppColors.TextMutedGray
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Compact filter chip matching Android chip_selected_background / chip_background appearance. */
@Composable
fun LegacyFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) AppColors.PrimaryBlue else AppColors.CardSurfaceDark
    val fg = if (selected) AppColors.TextWhite else AppColors.TextChipInactive
    Text(
        text = text,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}
