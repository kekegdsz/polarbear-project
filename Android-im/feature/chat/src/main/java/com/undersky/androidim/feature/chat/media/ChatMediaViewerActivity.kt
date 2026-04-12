package com.undersky.androidim.feature.chat.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.undersky.androidim.feature.chat.R
import com.undersky.androidim.feature.chat.databinding.ActivityChatMediaViewerBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatMediaPageImageBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatMediaPageVideoBinding
import java.io.File

private const val VIEW_IMAGE = 0
private const val VIEW_VIDEO = 1

private class ChatVisualMediaPagerAdapter(
    private val items: List<ChatVisualMediaPage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ImageHolder(val binding: ItemChatMediaPageImageBinding) : RecyclerView.ViewHolder(binding.root)
    class VideoHolder(val binding: ItemChatMediaPageVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        if (items[position].isVideo) VIEW_VIDEO else VIEW_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_IMAGE) {
            ImageHolder(ItemChatMediaPageImageBinding.inflate(inf, parent, false))
        } else {
            VideoHolder(ItemChatMediaPageVideoBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val page = items[position]
        when (holder) {
            is ImageHolder -> {
                val pathOrUrl = page.displayUriString().trim()
                if (pathOrUrl.isEmpty()) return
                val data: Any = when {
                    pathOrUrl.startsWith("http", ignoreCase = true) -> pathOrUrl
                    else -> File(pathOrUrl)
                }
                holder.binding.imageFull.load(data) {
                    allowHardware(false)
                }
            }
            is VideoHolder -> {
                val vv = holder.binding.videoView
                vv.stopPlayback()
                vv.setOnPreparedListener(null)
                val s = page.displayUriString().trim()
                if (s.isEmpty()) return
                if (s.startsWith("http", ignoreCase = true)) {
                    vv.setVideoURI(Uri.parse(s))
                } else {
                    vv.setVideoPath(s)
                }
                vv.setOnPreparedListener { mp ->
                    mp.isLooping = true
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}

/**
 * 全屏左右滑动浏览当前会话内的图片与视频。
 */
class ChatMediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatMediaViewerBinding
    private var pages: ArrayList<ChatVisualMediaPage> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_PAGES, ChatVisualMediaPage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_PAGES)
        } ?: arrayListOf()

        binding.buttonClose.setOnClickListener { finish() }

        if (pages.isEmpty()) {
            finish()
            return
        }

        val start = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, pages.lastIndex)

        binding.pager.adapter = ChatVisualMediaPagerAdapter(pages)
        binding.pager.setCurrentItem(start, false)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pauseAllVideosExcept(position)
                startVideoIfAt(position)
            }
        })
        binding.pager.post {
            pauseAllVideosExcept(start)
            startVideoIfAt(start)
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAllVideos()
    }

    private fun recycler(): RecyclerView? {
        for (i in 0 until binding.pager.childCount) {
            val c = binding.pager.getChildAt(i)
            if (c is RecyclerView) return c
        }
        return null
    }

    private fun pauseAllVideos() {
        val rv = recycler() ?: return
        for (i in 0 until rv.childCount) {
            val v = rv.getChildAt(i)
            v.findViewById<VideoView?>(R.id.video_view)?.pause()
        }
    }

    private fun pauseAllVideosExcept(keepPosition: Int) {
        val rv = recycler() ?: return
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            val vh = try {
                rv.getChildViewHolder(child)
            } catch (_: IllegalArgumentException) {
                continue
            }
            if (vh.bindingAdapterPosition != keepPosition) {
                child.findViewById<VideoView?>(R.id.video_view)?.pause()
            }
        }
    }

    private fun startVideoIfAt(position: Int) {
        val rv = recycler() ?: return
        val vh = rv.findViewHolderForAdapterPosition(position) ?: return
        if (position !in pages.indices || !pages[position].isVideo) return
        val vv = vh.itemView.findViewById<VideoView?>(R.id.video_view) ?: return
        try {
            vv.post { if (!isFinishing) try { vv.start() } catch (_: Exception) { } }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val EXTRA_PAGES = "pages"
        private const val EXTRA_INDEX = "index"

        fun start(context: Context, pages: List<ChatVisualMediaPage>, initialIndex: Int) {
            if (pages.isEmpty()) return
            val i = Intent(context, ChatMediaViewerActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_PAGES, ArrayList(pages))
                putExtra(EXTRA_INDEX, initialIndex.coerceIn(0, pages.lastIndex))
            }
            context.startActivity(i)
        }
    }
}
