package com.balance.monthly.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

private val ItemHeight = 44.dp
private const val VISIBLE_ITEMS = 5

/**
 * 1~31일을 고르는 세로 다이얼.
 * 굴리는 동안 가운데 값을 [onSelecting]으로, 멈추면 [onSelected]로 알린다.
 */
@Composable
fun DayWheel(
    selected: Int,
    onSelecting: (Int) -> Unit,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected - 1)
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.let { it.index + 1 } ?: selected
        }
    }
    val currentOnSelecting by rememberUpdatedState(onSelecting)
    val currentOnSelected by rememberUpdatedState(onSelected)

    LaunchedEffect(state) {
        snapshotFlow { centered }.distinctUntilChanged().drop(1).collect { currentOnSelecting(it) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.drop(1).filter { !it }.collect { currentOnSelected(centered) }
    }

    Box(modifier.height(ItemHeight * VISIBLE_ITEMS).semantics { contentDescription = "달 시작일 ${centered}일" }) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ItemHeight)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
        )
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
            contentPadding = PaddingValues(vertical = ItemHeight * (VISIBLE_ITEMS / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(31) { index ->
                val day = index + 1
                Box(Modifier.height(ItemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "${day}일",
                        style = if (day == centered) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        color = if (day == centered) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
