package io.github.autocoupons.core.designsystem.components

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Bottom-sheet drag handle that is also a 48 dp touch target. Material's default handle is 32 dp
 * wide, which the Accessibility Test Framework flags because the handle is clickable
 * (expand / collapse / dismiss).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcSheetDragHandle() = BottomSheetDefaults.DragHandle(width = 48.dp)
