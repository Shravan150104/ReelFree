package com.reelfree.detection

import com.reelfree.detection.model.NodeSnapshot
import com.reelfree.detection.model.ScreenSnapshot

/**
 * Synthetic screens for tests. They model the *kind* of structure the rules look for;
 * they are not recordings of real Instagram screens.
 */
object TestScreens {
    const val IG = "com.instagram.android"

    private fun tab(label: String, selected: Boolean = false) = NodeSnapshot(
        className = "FrameLayout", label = label, labelLength = label.length,
        selected = selected, clickable = true, widthFraction = 0.2f, heightFraction = 0.06f, topFraction = 0.93f, depth = 3,
    )

    private val root = NodeSnapshot(className = "FrameLayout", widthFraction = 1f, heightFraction = 1f, depth = 0)

    fun homeFeed() = ScreenSnapshot(
        IG,
        listOf(
            root,
            NodeSnapshot(viewId = "list", className = "RecyclerView", widthFraction = 1f, heightFraction = 0.85f, topFraction = 0.08f, depth = 1),
            // An embedded Reel preview inside the feed: has "clips" in its ID but is not full screen.
            NodeSnapshot(viewId = "clips_media_preview", className = "FrameLayout", widthFraction = 1f, heightFraction = 0.45f, topFraction = 0.3f, depth = 2),
            NodeSnapshot(className = "TextView", labelLength = 42, depth = 3),
            tab("home", selected = true), tab("reels"),
        ),
    )

    fun reelsTab() = ScreenSnapshot(
        IG,
        listOf(
            root,
            NodeSnapshot(viewId = "clips_viewer_view_pager", className = "ViewPager", widthFraction = 1f, heightFraction = 0.92f, depth = 1),
            tab("home"), tab("reels", selected = true),
        ),
    )

    /** Reel opened from a profile / DM / link: full-screen viewer, no tab bar. */
    fun reelViewerWithoutTabs() = ScreenSnapshot(
        IG,
        listOf(
            root,
            NodeSnapshot(viewId = "clips_viewer_view_pager", className = "ViewPager", widthFraction = 1f, heightFraction = 1f, depth = 1),
        ),
    )

    fun storyViewer() = ScreenSnapshot(
        IG,
        listOf(
            root,
            NodeSnapshot(viewId = "reel_viewer_root", className = "FrameLayout", widthFraction = 1f, heightFraction = 1f, depth = 1),
            // A story that is a re-share of a Reel may carry clips views as well.
            NodeSnapshot(viewId = "clips_viewer_media", className = "FrameLayout", widthFraction = 1f, heightFraction = 0.9f, depth = 2),
        ),
    )

    fun dmThread() = ScreenSnapshot(
        IG,
        listOf(
            root,
            NodeSnapshot(viewId = "message_list", className = "RecyclerView", widthFraction = 1f, heightFraction = 0.8f, depth = 1),
            NodeSnapshot(viewId = "row_thread_composer_edittext", className = "EditText", widthFraction = 0.7f, heightFraction = 0.05f, topFraction = 0.9f, depth = 2),
        ),
    )
}
