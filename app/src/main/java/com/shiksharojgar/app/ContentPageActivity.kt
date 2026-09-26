package com.shiksharojgar.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ContentPageActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var bodyBox: LinearLayout
    private lateinit var titleView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pageId = intent.getStringExtra("pageId") ?: "tools"
        val fallback = intent.getStringExtra("pageTitle") ?: "Shiksha Rojgar"
        setContentView(buildUi(fallback))
        load(pageId, fallback)
    }
    private fun buildUi(title: String): LinearLayout {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(248,250,252))}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(8,8,8,8);setBackgroundColor(Color.rgb(7,89,133))}
        bar.addView(Button(this).apply{text="←";setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},LinearLayout.LayoutParams(52,52))
        titleView=TextView(this).apply{text=title;textSize=18f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL}
        bar.addView(titleView,LinearLayout.LayoutParams(0,60,1f))
        bar.addView(Button(this).apply{text="⌂";setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},LinearLayout.LayoutParams(52,52))
        root.addView(bar)
        val scroll=ScrollView(this)
        bodyBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,14,14,28)}
        scroll.addView(bodyBox);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        return root
    }
    private fun load(pageId:String,fallback:String){
        db.collection("home_pages").document(pageId).get().addOnSuccessListener { d ->
            val title=d.getString("title").orEmpty().ifBlank{fallback}; val body=d.getString("body").orEmpty(); val image=d.getString("imageUrl").orEmpty()
            titleView.text=title
            bodyBox.removeAllViews()
            bodyBox.addView(TextView(this).apply{text=title;textSize=23f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));setPadding(0,0,0,12)})
            if(image.isNotBlank()) {
                val iv=ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_CROP;setBackgroundColor(Color.WHITE)}
                bodyBox.addView(iv,LinearLayout.LayoutParams(-1,220).apply{bottomMargin=12})
                ChannelRepository.loadImage(image){bmp,_->runOnUiThread{if(bmp!=null)iv.setImageBitmap(bmp)}}
            }
            bodyBox.addView(TextView(this).apply{text=body.ifBlank{"इस पेज की सामग्री अभी Admin Panel से जोड़ी नहीं गई है।"};textSize=16f;setTextColor(Color.rgb(30,41,59));setLineSpacing(4f,1.05f)})
        }.addOnFailureListener {
            bodyBox.addView(TextView(this).apply{text="सामग्री लोड नहीं हो सकी। कृपया बाद में फिर प्रयास करें।";textSize=16f})
        }
    }
}
