package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Extend the material past the viewport; its bottom stroke is clipped, not painted over. */
object TaktSheetMaterialShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp).createOutline(
            Size(size.width, size.height + with(density) { 2.dp.toPx() }), layoutDirection, density)
}

/** Opens at the expanded anchor so actions are visible without an extra drag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaktFullSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = .40f),
        // The material includes the navigation area; only content receives its bottom inset.
        contentWindowInsets = { BottomSheetDefaults.windowInsets.only(WindowInsetsSides.Top) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
    ) {
        // Keep the glass clip inside the sheet's draggable/offset wrapper.
        // Sample the themed wallpaper through the entire dialog, including window-edge pixels.
        Column(Modifier.fillMaxWidth().clipToBounds()
            .glassSurface(TaktSheetMaterialShape, extendBackdrop = true).navigationBarsPadding()) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally).semantics {
                    dismiss {
                        onDismissRequest()
                        true
                    }
                },
            )
            content()
        }
    }
}
