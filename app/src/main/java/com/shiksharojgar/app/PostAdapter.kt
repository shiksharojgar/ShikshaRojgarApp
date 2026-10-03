package com.shiksharojgar.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class PostAdapter(
    private var posts: List<Post>,
    private val onClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {

        val image: ImageView =
            v.findViewById(R.id.postImage)

        val source: TextView =
            v.findViewById(R.id.source)

        val title: TextView =
            v.findViewById(R.id.title)

        val description: TextView =
            v.findViewById(R.id.description)

        val date: TextView =
            v.findViewById(R.id.date)

        val share: TextView =
            v.findViewById(R.id.sharePost)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        return VH(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_post,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        val p = posts[position]

        holder.source.text =
            "${p.source}  •  Latest"

        holder.title.text =
            p.title

        holder.description.text =
            p.description

        holder.description.visibility =
            if (p.description.isBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        holder.date.text =
            p.date

        // ====================================================
        // POST IMAGE
        // ====================================================

        holder.image.setImageDrawable(null)

        if (p.imageUrl.isBlank()) {

            holder.image.visibility =
                View.GONE

        } else {

            holder.image.visibility =
                View.VISIBLE

            loadPostImage(
                holder.image,
                p.imageUrl
            )
        }

        // ====================================================
        // OPEN POST
        // ====================================================

        holder.itemView.setOnClickListener {

            AnalyticsTracker.uniqueFeedPostView(
                holder.itemView.context,
                p.url
            )

            onClick(p)
        }

        // ====================================================
        // SHARE POST
        // ====================================================

        holder.share.setOnClickListener {

            AnalyticsTracker.uniqueFeedPostShare(
                holder.itemView.context,
                p.url
            )

            holder.itemView.context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {

                        type = "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            "${p.title}\n${p.url}"
                        )
                    },
                    "Share Post"
                )
            )
        }
    }

    override fun getItemCount(): Int =
        posts.size

    fun submit(
        newPosts: List<Post>
    ) {

        posts = newPosts

        notifyDataSetChanged()
    }

    // ========================================================
    // SIMPLE IMAGE LOADER
    // ========================================================

    private fun loadPostImage(
        imageView: ImageView,
        imageUrl: String
    ) {

        val handler =
            Handler(Looper.getMainLooper())

        thread {

            var connection: HttpURLConnection? = null

            try {

                connection =
                    URL(imageUrl)
                        .openConnection()
                            as HttpURLConnection

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    15000

                connection.instanceFollowRedirects =
                    true

                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
                )

                connection.connect()

                if (
                    connection.responseCode
                    in 200..299
                ) {

                    val bitmap =
                        connection.inputStream
                            .use {
                                BitmapFactory.decodeStream(it)
                            }

                    if (bitmap != null) {

                        handler.post {

                            if (
                                imageView.tag ==
                                imageUrl ||
                                imageView.tag == null
                            ) {

                                imageView.setImageBitmap(
                                    bitmap
                                )
                            }
                        }
                    }
                }

            } catch (_: Exception) {

                handler.post {

                    imageView.visibility =
                        View.GONE
                }

            } finally {

                connection?.disconnect()
            }
        }

        imageView.tag =
            imageUrl
    }
}
