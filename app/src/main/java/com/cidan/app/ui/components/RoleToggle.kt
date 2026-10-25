package com.cidan.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cidan.app.ui.theme.AppColors
import com.cidan.app.ui.theme.Poppins

enum class UserRole(val label: String) {
    BANK_USER("Bank User"),
    ADMIN_STAFF("Admin/Staff")
}

@Composable
fun RoleToggle(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(AppColors.ToggleBackground)
            .padding(4.dp)
    ) {
        UserRole.values().forEach { role ->
            val isSelected = role == selectedRole
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AppColors.Primary else Color.Transparent,
                animationSpec = tween(220),
                label = "role_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else AppColors.TextSecondary,
                animationSpec = tween(220),
                label = "role_text"
            )

            val pillModifier = if (isSelected) {
                Modifier.shadow(6.dp, RoundedCornerShape(24.dp), spotColor = AppColors.Primary.copy(alpha = 0.3f))
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(pillModifier)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRoleSelected(role) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (role == UserRole.BANK_USER) Icons.Rounded.Person else Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = role.label,
                        fontFamily = Poppins,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
