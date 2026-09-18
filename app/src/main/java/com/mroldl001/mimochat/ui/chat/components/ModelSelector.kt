
package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.domain.model.AIModel

private val modelSuffixPattern = Regex(
    """(?:^|[\s_-])v?\d+(?:\.\d+)*[\s_-]+(\p{L}[\p{L}\p{N} ._+-]*)$""",
    RegexOption.IGNORE_CASE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    currentModel: AIModel?,
    models: List<AIModel>,
    onModelSelected: (AIModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusManager.clearFocus()
                showBottomSheet = true
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.animateContentSize(
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing
                )
            ),
            contentAlignment = Alignment.CenterStart
        ) {
            Crossfade(
                targetState = currentModel?.name ?: "选择模型",
                animationSpec = tween(
                    durationMillis = 180,
                    easing = FastOutSlowInEasing
                ),
                label = "selected_model_transition"
            ) { modelName ->
                val suffixColor = MaterialTheme.colorScheme.primary
                val styledName = remember(modelName, suffixColor) {
                    buildAnnotatedString {
                        append(modelName)
                        modelSuffixPattern.find(modelName)?.groups?.get(1)?.let { suffix ->
                            addStyle(
                                SpanStyle(color = suffixColor, fontWeight = FontWeight.ExtraBold),
                                start = suffix.range.first,
                                end = suffix.range.last + 1
                            )
                        }
                    }
                }
                Text(
                    text = styledName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        val rotation by animateFloatAsState(
            targetValue = if (showBottomSheet) 180f else 0f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "arrow_rotation"
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "选择模型",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp)
                .rotate(rotation)
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "选择模型",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                val selectedIndex = models.indexOfFirst { it.id == currentModel?.id }
                val selectionOffset by animateDpAsState(
                    targetValue = (selectedIndex.coerceAtLeast(0) * 64).dp,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    label = "model_selection_offset"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedIndex >= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = selectionOffset)
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .height(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.shapes.medium
                                )
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        models.forEach { model ->
                            ModelItem(
                                model = model,
                                isSelected = model.id == currentModel?.id,
                                onClick = {
                                    onModelSelected(model)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelItem(
    model: AIModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "text_color"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
