package com.shiksharojgar.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(private var posts: List<Post>, private val onClick: (Post) -> Unit) : RecyclerView.Adapter<PostAdapter.VH>() {
    class VH(v: View): RecyclerView.ViewHolder(v) {
        val source: TextView = v.findViewById(R.id.source)
        val title: TextView = v.findViewById(R.id.title)
        val date: TextView = v.findViewById(R.id.date)
        val share: TextView = v.findViewById(R.id.sharePost)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = posts[position]
        holder.source.text = "${p.source}  •  Latest"
        holder.title.text = p.title
        holder.date.text = p.date
        holder.itemView.setOnClickListener {
            AnalyticsTracker.uniqueFeedPostView(holder.itemView.context, p.url)
            onClick(p)
        }
        holder.share.setOnClickListener {
            AnalyticsTracker.uniqueFeedPostShare(holder.itemView.context, p.url)
            holder.itemView.context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${p.title}\n${p.url}")
            }, "Share Post"))
        }
    }
    override fun getItemCount() = posts.size
    fun submit(newPosts: List<Post>) { posts = newPosts; notifyDataSetChanged() }
}
