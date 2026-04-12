package com.undersky.androidim.feature.chat.adapters

import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.chat.ChatListItem
import com.undersky.im.core.api.ChatMessage

/**
 * 左滑自己消息 / 右滑对方消息 → 快速进入引用回复（微信需多步，主流 IM 常见手势）。
 */
class ChatSwipeReplyItemTouchHelper(
    private val adapter: ChatMessageAdapter,
    private val onSwipeReply: (ChatMessage) -> Unit
) : ItemTouchHelper.SimpleCallback(0, 0) {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val swipe = when (viewHolder) {
            is ChatMessageAdapter.MineVh -> ItemTouchHelper.LEFT
            is ChatMessageAdapter.OtherVh -> ItemTouchHelper.RIGHT
            else -> 0
        }
        return makeMovementFlags(0, swipe)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.32f

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 6f

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return
        val row = adapter.currentList.getOrNull(pos) as? ChatListItem.MessageRow ?: return
        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        onSwipeReply(row.message)
        adapter.notifyItemChanged(pos)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val w = viewHolder.itemView.width.toFloat().coerceAtLeast(1f)
            val clamped = when (viewHolder) {
                is ChatMessageAdapter.MineVh -> dX.coerceIn(-w * 0.45f, 0f)
                is ChatMessageAdapter.OtherVh -> dX.coerceIn(0f, w * 0.45f)
                else -> dX
            }
            super.onChildDraw(c, recyclerView, viewHolder, clamped, dY, actionState, isCurrentlyActive)
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}
