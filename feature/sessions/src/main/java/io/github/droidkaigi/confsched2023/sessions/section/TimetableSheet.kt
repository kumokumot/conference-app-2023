package io.github.droidkaigi.confsched2023.sessions.section

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.droidkaigi.confsched2023.model.TimetableItem
import io.github.droidkaigi.confsched2023.sessions.component.TimetableScreenScrollState
import io.github.droidkaigi.confsched2023.sessions.component.TimetableTab
import io.github.droidkaigi.confsched2023.sessions.component.TimetableTabRow
import io.github.droidkaigi.confsched2023.sessions.component.TimetableTabState
import io.github.droidkaigi.confsched2023.sessions.component.rememberTimetableTabState
import io.github.droidkaigi.confsched2023.sessions.section.TimetableSheetUiState.Empty
import io.github.droidkaigi.confsched2023.sessions.section.TimetableSheetUiState.GridTimetable
import io.github.droidkaigi.confsched2023.sessions.section.TimetableSheetUiState.ListTimetable

sealed interface TimetableSheetUiState {
    object Empty : TimetableSheetUiState
    data class ListTimetable(
        val timetableListUiState: TimetableListUiState,
    ) : TimetableSheetUiState

    data class GridTimetable(
        val timetableGridUiState: TimetableGridUiState,
    ) : TimetableSheetUiState
}

@Composable
fun TimetableSheet(
    uiState: TimetableSheetUiState,
    timetableScreenScrollState: TimetableScreenScrollState,
    onTimetableItemClick: (TimetableItem) -> Unit,
    onFavoriteClick: (TimetableItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val corner by animateIntAsState(
        if (timetableScreenScrollState.isScreenLayoutCalculating || timetableScreenScrollState.isSheetExpandable) 40 else 0,
        label = "Timetable corner state",
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = corner.dp, topEnd = corner.dp),
    ) {
        val timetableSheetContentScrollState = rememberTimetableSheetContentScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(timetableSheetContentScrollState.nestedScrollConnection),
        ) {
            TimetableTabRow(
                tabState = timetableSheetContentScrollState.tabScrollState,
                selectedTabIndex = selectedTabIndex,
            ) {
                // TODO: Mapping tab data
                (0..2).forEach {
                    TimetableTab(
                        day = it,
                        selected = it == selectedTabIndex,
                        onClick = {
                            selectedTabIndex = it
                        },
                        scrollState = timetableSheetContentScrollState.tabScrollState,
                    )
                }
            }
            when (uiState) {
                is Empty -> {
                    Text(
                        text = "empty",
                        modifier = Modifier.testTag("empty"),
                    )
                }

                is ListTimetable -> {
                    TimetableList(
                        uiState = uiState.timetableListUiState,
                        onTimetableItemClick = onTimetableItemClick,
                        onBookmarkClick = onFavoriteClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                }

                is GridTimetable -> {
                    TimetableGrid(
                        uiState = uiState.timetableGridUiState,
                        onTimetableItemClick = onTimetableItemClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun rememberTimetableSheetContentScrollState(
    tabScrollState: TimetableTabState = rememberTimetableTabState(),
): TimetableSheetContentScrollState {
    return remember { TimetableSheetContentScrollState(tabScrollState) }
}

@Stable
class TimetableSheetContentScrollState(
    val tabScrollState: TimetableTabState,
) {
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            return onPreScrollSheetContent(available)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return onPostScrollSheetContent(available)
        }
    }

    /**
     * @return consumed offset
     */
    private fun onPreScrollSheetContent(availableScrollOffset: Offset): Offset {
        if (availableScrollOffset.y >= 0) return Offset.Zero
        // When scrolled upward
        return if (tabScrollState.isTabExpandable) {
            val prevHeightOffset: Float = tabScrollState.scrollOffset
            tabScrollState.onScroll(availableScrollOffset.y)
            availableScrollOffset.copy(x = 0f, y = tabScrollState.scrollOffset - prevHeightOffset)
        } else {
            Offset.Zero
        }
    }

    /**
     * @return consumed offset
     */
    private fun onPostScrollSheetContent(availableScrollOffset: Offset): Offset {
        if (availableScrollOffset.y < 0f) return Offset.Zero
        return if (tabScrollState.isTabCollapsing && availableScrollOffset.y > 0) {
            // When scrolling downward and overscroll
            val prevHeightOffset = tabScrollState.scrollOffset
            tabScrollState.onScroll(availableScrollOffset.y)
            availableScrollOffset.copy(x = 0f, y = tabScrollState.scrollOffset - prevHeightOffset)
        } else {
            Offset.Zero
        }
    }
}