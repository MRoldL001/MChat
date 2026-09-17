package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.ui.chat.viewmodel.SkillType

@Composable
fun SkillToggleBar(
    isThinkingMode: Boolean,
    isWebSearchEnabled: Boolean,
    activeSkill: SkillType?,
    isGenerating: Boolean,
    onThinkingModeToggle: (Boolean) -> Unit,
    onWebSearchToggle: (Boolean) -> Unit,
    onSkillToggle: (SkillType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isThinkingActive = activeSkill == null && isThinkingMode
    val isPoetActive = activeSkill == SkillType.POET
    val isLearningActive = activeSkill == SkillType.LEARNING

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkillToggleChip(
            icon = Icons.Default.Public,
            label = "联网",
            isActive = isWebSearchEnabled,
            isDisabled = isGenerating,
            onClick = { onWebSearchToggle(!isWebSearchEnabled) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        SkillToggleChip(
            icon = Icons.Default.Psychology,
            label = "思考",
            isActive = isThinkingActive,
            isDisabled = isGenerating,
            onClick = {
                if (isThinkingMode) {
                    onThinkingModeToggle(false)
                } else {
                    onThinkingModeToggle(true)
                    onSkillToggle(null)
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        SkillToggleChip(
            icon = Icons.Default.Edit,
            label = "诗人",
            isActive = isPoetActive,
            isDisabled = isGenerating,
            onClick = {
                if (isPoetActive) {
                    onSkillToggle(null)
                } else {
                    onThinkingModeToggle(false)
                    onSkillToggle(SkillType.POET)
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        SkillToggleChip(
            icon = Icons.Default.School,
            label = "学习",
            isActive = isLearningActive,
            isDisabled = isGenerating,
            onClick = {
                if (isLearningActive) {
                    onSkillToggle(null)
                } else {
                    onThinkingModeToggle(false)
                    onSkillToggle(SkillType.LEARNING)
                }
            }
        )
    }
}

@Composable
private fun SkillToggleChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val borderColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "skill_border_color"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
            else if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "skill_icon_tint"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
            else if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "skill_text_color"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(width = 2.dp, color = borderColor),
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                enabled = !isDisabled,
                onClick = {
                    focusManager.clearFocus()
                    onClick()
                }
            )
    ) {
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}
