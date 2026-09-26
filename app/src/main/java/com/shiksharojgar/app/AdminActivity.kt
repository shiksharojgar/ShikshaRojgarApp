package com.shiksharojgar.app

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {
    private val auth=FirebaseAuth.getInstance(); private val db=FirebaseFirestore.getInstance()
    private var imageUri:Uri?=null; private var fileUri:Uri?=null
    private var pageImageUri:Uri?=null
    private var pageImageField:EditText?=null
    private val selectedImageUris = mutableListOf<Uri>()
    private var imagePreviewContainer: LinearLayout? = null
    private lateinit var panel:LinearLayout; private lateinit var globalSwitch:Switch; private lateinit var postsContainer:LinearLayout; private lateinit var postScroll:ScrollView; private var globalCommentsEnabled=true
    private val selectedPostIds = mutableSetOf<String>()
    private val pickImage=registerForActivityResult(ActivityResultContracts.GetMultipleContents()){ uris ->
        selectedImageUris.clear()
        selectedImageUris.addAll(uris.take(5))
        imageUri = selectedImageUris.firstOrNull()
        updateImagePreview()
        toast(if(uris.isNotEmpty()) "${uris.size} फोटो चुनी गईं" else "कोई फोटो नहीं चुनी गई")
    }
    private val pickFile=registerForActivityResult(ActivityResultContracts.OpenDocument()){ fileUri=it; toast(if(it!=null) "PDF/Document चुना गया" else "") }
    private val pickPageImage=registerForActivityResult(ActivityResultContracts.GetContent()){ uri -> pageImageUri=uri; pageImageField?.setText(if(uri!=null) "नई फोटो चुनी गई — SAVE दबाएँ" else ""); toast(if(uri!=null) "Page फोटो चुनी गई" else "") }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState); auth.useAppLanguage(); window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (auth.currentUser != null) checkAdmin(autoContinue = true) else setContentView(loginUi())
    }

    private fun dp(v:Int): Int = (v * resources.displayMetrics.density).toInt()
    private var phoneVerificationId: String? = null

    private fun loginUi():View {
        val content=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(20),dp(20),dp(20),dp(28))
        }
        val scroll=ScrollView(this).apply{isFillViewport=true; clipToPadding=false}
        scroll.addView(content)
        val root=content
        root.addView(TextView(this).apply{text="🔐 Admin Login";textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(4))})
        root.addView(TextView(this).apply{text="केवल Admin account से Login करें";textSize=13f;gravity=Gravity.CENTER;setPadding(0,dp(6),0,dp(18))})

        root.addView(TextView(this).apply{text="1️⃣ Email + Password";textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));setPadding(0,dp(8),0,dp(7))})
        val email=EditText(this).apply{hint="Admin Email ID";inputType=33;setSingleLine(true);setPadding(dp(14),0,dp(14),0)}
        val pass=EditText(this).apply{hint="Password";inputType=129;setSingleLine(true);setPadding(dp(14),0,dp(14),0)}
        pass.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.ic_menu_view,0)
        pass.setOnTouchListener { v,event -> if(event.action==android.view.MotionEvent.ACTION_UP && event.rawX >= pass.right-80){ pass.inputType=if(pass.inputType==129) 1 else 129; pass.setSelection(pass.text.length); true } else false }
        root.addView(email,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(8)})
        root.addView(pass,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(10)})
        root.addView(Button(this).apply{text="LOGIN WITH EMAIL";minHeight=dp(52);setOnClickListener{
            val e=email.text.toString().trim(); val p=pass.text.toString()
            if(e.isBlank() || p.isBlank()){toast("Email और Password दोनों भरें");return@setOnClickListener}
            isEnabled=false
            auth.signOut()
            auth.signInWithEmailAndPassword(e,p)
                .addOnSuccessListener { isEnabled=true; checkAdmin() }
                .addOnFailureListener { isEnabled=true; toast("Login असफल: ${it.message}") }
        }},LinearLayout.LayoutParams(-1,dp(52)))

        root.addView(TextView(this).apply{text="या";textSize=13f;gravity=Gravity.CENTER;setPadding(0,dp(12),0,dp(12))})
        root.addView(TextView(this).apply{text="2️⃣ Mobile OTP Login";textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));setPadding(0,dp(4),0,dp(7))})
        val phone=EditText(this).apply{hint="मोबाइल नंबर (10 अंक या +91XXXXXXXXXX)";inputType=3;setSingleLine(true);setPadding(dp(14),0,dp(14),0)}
        val otp=EditText(this).apply{hint="OTP (6 अंक)";inputType=2;setSingleLine(true);visibility=View.GONE;setPadding(dp(14),0,dp(14),0)}
        val send=Button(this).apply{text="📱 SEND OTP";minHeight=dp(52)}
        val verify=Button(this).apply{text="✓ VERIFY & LOGIN";visibility=View.GONE;minHeight=dp(52)}
        root.addView(phone,LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(8)})
        root.addView(send,LinearLayout.LayoutParams(-1,dp(52)))
        root.addView(otp,LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(8);bottomMargin=dp(8)})
        root.addView(verify,LinearLayout.LayoutParams(-1,dp(52)))

        send.setOnClickListener {
            val raw=phone.text.toString().trim().replace(" ","").replace("-","")
            val number=when { raw.startsWith("+91") -> raw; raw.startsWith("0") && raw.length==11 -> "+91"+raw.substring(1); raw.length==10 -> "+91$raw"; else -> "" }
            if(number.isBlank()){ toast("10 अंकों का भारतीय मोबाइल नंबर डालें"); return@setOnClickListener }
            send.isEnabled=false
            val options=PhoneAuthOptions.newBuilder(auth).setPhoneNumber(number).setTimeout(60L,TimeUnit.SECONDS).setActivity(this)
                .setCallbacks(object:PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                    override fun onVerificationCompleted(credential:PhoneAuthCredential){ auth.signInWithCredential(credential).addOnSuccessListener{checkAdmin()}.addOnFailureListener{send.isEnabled=true;toast("Phone Login असफल: ${it.message}")} }
                    override fun onVerificationFailed(e:FirebaseException){send.isEnabled=true;showPhoneError(e)}
                    override fun onCodeSent(id:String,token:PhoneAuthProvider.ForceResendingToken){phoneVerificationId=id;otp.visibility=View.VISIBLE;verify.visibility=View.VISIBLE;send.text="OTP भेजा गया";toast("OTP भेज दिया गया")}
                }).build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
        verify.setOnClickListener {
            val id=phoneVerificationId; val code=otp.text.toString().trim()
            if(id.isNullOrBlank() || code.length<6){toast("6 अंकों का OTP डालें");return@setOnClickListener}
            verify.isEnabled=false
            auth.signInWithCredential(PhoneAuthProvider.getCredential(id,code)).addOnSuccessListener{verify.isEnabled=true;checkAdmin()}.addOnFailureListener{verify.isEnabled=true;toast("OTP गलत है या Login असफल है")}
        }

        root.addView(Button(this).apply{text="ℹ️ Firebase Phone Login Setup";minHeight=dp(50);setOnClickListener{showPhoneSetup()}},LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(12)})
        root.addView(TextView(this).apply{text="Email/Password या Mobile OTP से Firebase Authentication Login करें। Admin अधिकार Firebase users document से सत्यापित होता है।";textSize=12f;setPadding(0,dp(12),0,0)})
        return scroll
    }

    private fun showPhoneError(e: FirebaseException) {
        val code = if (e is FirebaseAuthException) e.errorCode else e.javaClass.simpleName
        val raw = e.message ?: "Unknown Firebase error"
        val hint = when {
            raw.contains("CONFIGURATION_NOT_FOUND", true) || raw.contains("OPERATION_NOT_ALLOWED", true) ->
                "Firebase Console में Phone provider ON करें और India को SMS region policy में allow करें।"
            raw.contains("INVALID_APP_CREDENTIAL", true) || raw.contains("app credential", true) ->
                "इस APK के certificate SHA-1/SHA-256 को Firebase Project Settings में जोड़ें। Debug APK के लिए Debug SHA और Release APK के लिए Release SHA अलग हो सकती है।"
            raw.contains("TOO_MANY_REQUESTS", true) -> "बहुत अधिक OTP requests हुई हैं। कुछ समय बाद फिर कोशिश करें।"
            else -> "Firebase Phone Auth की configuration जाँचें।"
        }
        AlertDialog.Builder(this)
            .setTitle("OTP नहीं भेजा गया")
            .setMessage("Error: $code\n\n$raw\n\nक्या करें:\n$hint")
            .setPositiveButton("OK", null).show()
    }

    private fun showPhoneSetup() {
        AlertDialog.Builder(this)
            .setTitle("📱 Firebase Phone Login Setup")
            .setMessage("1. Firebase Console → Authentication → Sign-in method → Phone ON करें।\n\n2. Authentication → Settings → SMS region policy में India allow करें।\n\n3. Project Settings → Android app में इस APK का SHA-256 और SHA-1 जोड़ें। Debug APK और Release APK के fingerprints अलग हो सकते हैं।\n\n4. Phone से बना user Admin तभी बनेगा जब उसी UID के users document में admin=true (या isAdmin=true / role=admin) सेट हो।")
            .setPositiveButton("OK", null).show()
    }
    private fun checkAdmin(autoContinue:Boolean = false){
        // Emergency-safe fallback for the configured Firebase Admin Auth UID.
        // Firestore document lookup remains the primary verification path.
        val configuredAdminUid = "PfpAI3BR2XfihY4t4B5ldInDBg72"
        val user = auth.currentUser
        val uid = user?.uid
        if(uid.isNullOrBlank()) { toast("Admin account नहीं मिला"); return }
        val email = user.email ?: "null"
        val providers = user.providerData.filter{it.providerId != "firebase"}.joinToString(", "){it.providerId}
        val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "unknown"

        fun evaluate(doc: com.google.firebase.firestore.DocumentSnapshot, source: String) {
            val adminValue = doc.getBoolean("admin")
            val isAdminValue = doc.getBoolean("isAdmin")
            val roleValue = doc.getString("role")
            val firestoreAdmin = doc.exists() && (adminValue == true || isAdminValue == true || roleValue?.equals("admin", true) == true)
            val uidAdminFallback = uid == configuredAdminUid && !doc.exists()
            val isAdmin = firestoreAdmin || uidAdminFallback
            val message = "Signed-in Email: $email\nProvider: ${if(providers.isBlank()) "unknown" else providers}\n\nFirebase Project: $projectId\nCurrent Auth UID: $uid\n\nusers/$uid document: ${if(doc.exists()) "OK ($source)" else "NOT FOUND"}\nadmin = ${adminValue ?: "null"}\nisAdmin = ${isAdminValue ?: "null"}\nrole = ${roleValue ?: "null"}"
            if(isAdmin) {
                if (autoContinue) { showPanel(); return }
                val verificationSource = if (firestoreAdmin) "Firestore users document" else "configured Admin Auth UID fallback"
                AlertDialog.Builder(this).setTitle("Admin Verification Diagnostic").setMessage(message + "\n\nAdmin verification सफल है।\nVerification source: $verificationSource")
                    .setPositiveButton("CONTINUE") { _, _ -> showPanel() }.setOnCancelListener { showPanel() }.show()
            } else {
                AlertDialog.Builder(this).setTitle("Admin Verification Diagnostic").setMessage(message + "\n\nAdmin verification असफल है।")
                    .setPositiveButton("OK") { _, _ -> auth.signOut() }.setOnCancelListener { auth.signOut() }.show()
            }
        }

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if(doc.exists()) evaluate(doc, "direct document")
            else {
                // Older setup may have stored the Auth UID in a field but used a different document ID.
                db.collection("users").whereEqualTo("uid", uid).limit(1).get()
                    .addOnSuccessListener { q -> if(!q.isEmpty) evaluate(q.documents.first(), "uid field fallback") else evaluate(doc, "direct document") }
                    .addOnFailureListener { evaluate(doc, "direct document") }
            }
        }.addOnFailureListener { e ->
            AlertDialog.Builder(this).setTitle("Admin Verification Error")
                .setMessage("Signed-in Email: $email\nFirebase Project: $projectId\nCurrent Auth UID: $uid\n\nFirestore पढ़ने में समस्या:\n${e.message ?: "Unknown error"}")
                .setPositiveButton("OK") { _, _ -> auth.signOut() }.show()
        }
    }
    private fun showPanel(){
        panel = LinearLayout(this)
        // Fixed admin controls at the top; ONLY the posts list scrolls.
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(245,248,252))}
        setContentView(root)

        val fixed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(8));setBackgroundColor(Color.WHITE);elevation=8f}
        fixed.addView(TextView(this).apply{text="⚙ शिक्षा रोजगार Channel Admin";textSize=22f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));setPadding(0,0,0,8)})
        fixed.addView(Button(this).apply{text="🚪 Logout";setOnClickListener{
            auth.signOut(); Toast.makeText(this@AdminActivity,"Admin Logout हो गया",Toast.LENGTH_SHORT).show(); setContentView(loginUi())
        }},LinearLayout.LayoutParams(-1,dp(46)).apply{bottomMargin=dp(6)})
        globalSwitch=Switch(this).apply{text="सभी Posts के Comments";textSize=15f}
        fixed.addView(globalSwitch)
        db.document("channel_config/main").get().addOnSuccessListener{globalCommentsEnabled=it.getBoolean("commentsEnabled") ?: true; globalSwitch.isChecked=globalCommentsEnabled}
        globalSwitch.setOnCheckedChangeListener{_,checked-> globalCommentsEnabled=checked; db.document("channel_config/main").set(mapOf("commentsEnabled" to checked), com.google.firebase.firestore.SetOptions.merge()) }
        fixed.addView(Button(this).apply{text="📢 CREATE NEW CHANNEL POST";setOnClickListener{newPostDialog()}},LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(4)})
        val postTools=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        postTools.addView(Button(this).apply{text="☑ SELECT ALL";setOnClickListener{selectAllAdminPosts()}},LinearLayout.LayoutParams(0,dp(46),1f).apply{rightMargin=dp(4)})
        postTools.addView(Button(this).apply{text="🗑 DELETE SELECTED";setOnClickListener{deleteSelectedAdminPosts()}},LinearLayout.LayoutParams(0,dp(46),1f).apply{leftMargin=dp(4)})
        fixed.addView(postTools)
        fixed.addView(Button(this).apply{text="🗑 DELETE ALL POSTS";setOnClickListener{deleteAllAdminPosts()}},LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(4)})
        fixed.addView(TextView(this).apply{text="📚 HOME PAGES";textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));setPadding(0,dp(8),0,dp(4))})
        val pageManager=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val pageDefs=listOf("syllabus" to "Syllabus","notices" to "Notices","career" to "Career Guide","tools" to "Useful Tools")
        pageDefs.forEach{(id,label)-> pageManager.addView(Button(this).apply{text="✏️ $label  —  ADD / EDIT";setOnClickListener{editHomePage(id,label)}})}
        fixed.addView(pageManager)
        fixed.addView(Button(this).apply{text="📊 Channel Analytics";setOnClickListener{showAnalytics()}},LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(4)})
        root.addView(fixed,LinearLayout.LayoutParams(-1,-2))

        val postHeader=TextView(this).apply{text="📋 Channel Posts — नई post नीचे दिखाई देगी";textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(7,89,133));setPadding(dp(12),dp(8),dp(12),dp(6));setBackgroundColor(Color.rgb(245,248,252))}
        root.addView(postHeader)
        postScroll=ScrollView(this).apply{isFillViewport=true;isVerticalScrollBarEnabled=true}
        postsContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,dp(12),dp(20))}
        postScroll.addView(postsContainer)
        root.addView(postScroll,LinearLayout.LayoutParams(-1,0,1f))
        loadAdminPosts()
    }
    private fun showAnalytics(){
        val dialog=AlertDialog.Builder(this)
            .setTitle("📊 Shiksha Rojgar Analytics")
            .setMessage("लोड हो रहा है…")
            .setPositiveButton("OK",null)
            .create()
        dialog.show()

        db.document("channel_config/main").get()
            .addOnSuccessListener { d ->
                val msg =
                    "👥 Channel Followers: ${d.getLong("followerCount") ?: 0}\n" +
                    "👁 Channel Post Views: ${d.getLong("totalPostViews") ?: 0}\n" +
                    "↗ Channel Post Shares: ${d.getLong("totalPostShares") ?: 0}\n" +
                    "👍 Channel Likes: ${d.getLong("totalLikes") ?: 0}\n" +
                    "💬 Channel Comments: ${d.getLong("totalComments") ?: 0}\n\n" +
                    "📱 App First-use / Install Users: ${d.getLong("totalAppUsers") ?: 0}\n" +
                    "📈 Active User-Days: ${d.getLong("totalActiveUserDays") ?: 0}\n\n" +
                    "Analytics अब channel_config/main के aggregate counters से पढ़े जा रहे हैं।\n" +
                    "इससे collection-group permission error के कारण पूरा Analytics dialog बंद नहीं होगा।\n\n" +
                    "नोट: Website से APK file download और APK install अलग metrics हैं।"
                dialog.setMessage(msg)
            }
            .addOnFailureListener { e ->
                dialog.setMessage("Channel analytics पढ़ा नहीं जा सका: ${e.message ?: "Firestore error"}")
            }
    }

    private fun newPostDialog(){
        selectedImageUris.clear()
        imageUri = null
        fileUri = null

        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(10,10,10,10)}
        val title=EditText(this).apply{hint="पोस्ट शीर्षक"}
        val body=EditText(this).apply{hint="विवरण / आदेश का पाठ";minLines=4}
        val cat=EditText(this).apply{hint="Category: आदेश/निर्देश"}
        box.addView(title);box.addView(body);box.addView(cat)

        box.addView(Button(this).apply{
            text="🖼️  फोटो / Gallery"
            setOnClickListener{
                pickImage.launch("image/*")
            }
        })

        imagePreviewContainer=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
            setPadding(0,6,0,6)
        }
        box.addView(imagePreviewContainer, LinearLayout.LayoutParams(-1,dp(90)))

        box.addView(Button(this).apply{
            text="📄 PDF/Document चुनें"
            setOnClickListener{pickFile.launch(arrayOf("application/pdf","image/*","text/plain"))}
        })
        val comments=Switch(this).apply{text="इस Post पर Comments ON";isChecked=globalCommentsEnabled;isEnabled=globalCommentsEnabled}
        box.addView(comments)

        AlertDialog.Builder(this).setTitle("नई Channel Post").setView(box)
            .setPositiveButton("PUBLISH",null).setNegativeButton("Cancel",null).create().also{d->
            d.setOnShowListener{
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                    val rawTitle=title.text.toString().trim()
                    val rawBody=body.text.toString().trim()
                    val chosenFile=fileUri
                    val finalTitle=if(rawTitle.isNotBlank()) rawTitle else when {
                        rawBody.isNotBlank() -> rawBody.replace("\n"," ").take(90)
                        selectedImageUris.isNotEmpty() -> "📷 शिक्षा रोजगार फोटो अपडेट"
                        chosenFile!=null -> displayName(chosenFile).substringBeforeLast('.').ifBlank{"शिक्षा रोजगार अपडेट"}
                        else -> "शिक्षा रोजगार अपडेट"
                    }
                    if (rawBody.isBlank() && selectedImageUris.isEmpty() && chosenFile == null) {
                        toast("कम से कम फोटो, PDF या विवरण जोड़ें")
                        return@setOnClickListener
                    }
                    d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                    val category = cat.text.toString().trim().ifBlank{"आदेश/निर्देश"}
                    val commentsEnabled = globalCommentsEnabled && comments.isChecked
                    // Text-only posts bypass media upload completely.
                    if (rawBody.isNotBlank() && selectedImageUris.isEmpty() && chosenFile == null) {
                        ChannelRepository.createPost(mapOf(
                            "title" to finalTitle, "body" to rawBody, "published" to true,
                            "postType" to "text", "category" to category,
                            "imageUrl" to "", "fileUrl" to "", "fileName" to "",
                            "imageMime" to "", "fileMime" to "", "createdAt" to System.currentTimeMillis(),
                            "commentsEnabled" to commentsEnabled, "likeCount" to 0L,
                            "commentCount" to 0L, "viewCount" to 0L, "shareCount" to 0L
                        )) { ok,msg -> runOnUiThread {
                            d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true
                            if(ok){ d.dismiss(); toast("Text-only Post Publish हो गई"); loadAdminPosts() }
                            else toast("Publish असफल: ${msg ?: "Firestore permission/error"}")
                        }}
                    } else {
                        uploadBoth(finalTitle,rawBody,category,commentsEnabled){ok,msg-> runOnUiThread {
                            d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true
                            if(ok){ d.dismiss(); toast("Post Publish हो गई"); loadAdminPosts() }
                            else toast("Publish असफल: ${msg ?: "Firestore/media error"}")
                        }}
                    }
                }
            }
            d.show()
        }
    }

    private fun updateImagePreview(){
        val container=imagePreviewContainer ?: return
        container.removeAllViews()
        selectedImageUris.take(5).forEach { uri ->
            val iv=ImageView(this).apply{
                setImageURI(uri)
                scaleType=ImageView.ScaleType.CENTER_CROP
                setPadding(3,3,3,3)
            }
            container.addView(iv,LinearLayout.LayoutParams(dp(78),dp(78)).apply{rightMargin=dp(5)})
        }
    }

    private fun uploadBoth(title:String,body:String,cat:String,comments:Boolean,done:(Boolean,String?)->Unit){
        val images=selectedImageUris.toList()
        val file=fileUri

        fun createOne(image:String,fileUrl:String,imageMime:String,fileMime:String,index:Int,total:Int,finish:(Boolean,String?)->Unit){
            val postTitle=if(total>1) "$title (${index+1}/$total)" else title
            ChannelRepository.createPost(
                mapOf(
                    "title" to postTitle,
                    "body" to body,
                    "published" to true,
                    "postType" to if (body.isNotBlank() && image.isBlank() && fileUrl.isBlank()) "text" else "media",
                    "category" to cat,
                    "imageUrl" to image,
                    "fileUrl" to fileUrl,
                    "fileName" to (file?.let{displayName(it)} ?: "document.pdf"),
                    "imageMime" to imageMime,
                    "fileMime" to fileMime,
                    "createdAt" to System.currentTimeMillis(),
                    "commentsEnabled" to comments,
                    "likeCount" to 0L,
                    "commentCount" to 0L,
                    "viewCount" to 0L,
                    "shareCount" to 0L
                )
            ){ok,msg->finish(ok,msg)}
        }

        fun publishImages(index:Int){
            if(index>=images.size){
                if(file!=null && images.isEmpty()){
                    ChannelRepository.upload(file,"documents"){fu,fe->
                        if(fu==null) done(false,fe) else createOne("","$fu","image/jpeg",contentResolver.getType(file) ?: "application/pdf",0,1,done)
                    }
                } else done(true,null)
                return
            }
            val uri=images[index]
            ChannelRepository.upload(uri,"images"){url,err->
                if(url==null){done(false,err);return@upload}
                val imageMime=contentResolver.getType(uri) ?: "image/jpeg"
                if(index==images.lastIndex && file!=null){
                    ChannelRepository.upload(file,"documents"){fu,fe->
                        if(fu==null){done(false,fe);return@upload}
                        createOne(url,fu,imageMime,contentResolver.getType(file) ?: "application/pdf",index,images.size){ok,msg->
                            if(!ok) done(false,msg) else done(true,null)
                        }
                    }
                } else {
                    createOne(url,"",imageMime,"application/pdf",index,images.size){ok,msg->
                        if(!ok) done(false,msg) else publishImages(index+1)
                    }
                }
            }
        }

        publishImages(0)
    }

    private fun editHomePage(pageId:String, fallbackTitle:String){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(4),dp(8),dp(4))}
        pageImageUri=null
        val title=EditText(this).apply{hint="Page title"}; val body=EditText(this).apply{hint="Page content / text";minLines=7;gravity=Gravity.TOP}
        val imageUrl=EditText(this).apply{hint="Photo URL या firestore-media://... (optional)"}
        pageImageField=imageUrl
        box.addView(title);box.addView(body);box.addView(imageUrl)
        box.addView(Button(this).apply{text="🖼️ Page में फोटो जोड़ें / बदलें";setOnClickListener{pickPageImage.launch("image/*")}})
        db.collection("home_pages").document(pageId).get().addOnSuccessListener{d->title.setText(d.getString("title") ?: fallbackTitle);body.setText(d.getString("body") ?: "");imageUrl.setText(d.getString("imageUrl") ?: "")}
        AlertDialog.Builder(this).setTitle("✏️ $fallbackTitle").setView(box).setPositiveButton("SAVE",null).setNegativeButton("Cancel",null).create().also{dlg->
            dlg.setOnShowListener{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                val saveUrl: (String)->Unit = { url ->
                    db.collection("home_pages").document(pageId).set(mapOf("title" to title.text.toString().trim().ifBlank{fallbackTitle},"body" to body.text.toString(),"imageUrl" to url,"updatedAt" to System.currentTimeMillis()),com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener{dlg.dismiss();toast("Page सामग्री सेव हो गई")}.addOnFailureListener{toast("SAVE FAILED: ${it.message ?: "Permission denied"}\nUID: ${auth.currentUser?.uid ?: "none"}")}
                }
                val chosen=pageImageUri
                if(chosen!=null){
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                    ChannelRepository.upload(chosen,"images"){url,err -> runOnUiThread{if(url!=null)saveUrl(url) else {dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true;toast("Photo upload असफल: $err")}}}
                } else saveUrl(imageUrl.text.toString().trim())
            }};dlg.show()
        }
    }

    private fun loadAdminPosts(){
        if(!::postsContainer.isInitialized)return
        selectedPostIds.clear()
        postsContainer.removeAllViews()
        ChannelRepository.posts({ps->
            ps.takeLast(100).forEach{p->addAdminPost(p)}
            postScroll.post { postScroll.fullScroll(View.FOCUS_DOWN) }
        }, { e -> toast("Posts लोड नहीं हुए: ${e.message ?: "Firestore error"}") })
    }

    private fun addAdminPost(p:ChannelPost){
        val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=GradientFactory.roundedWhite();setPadding(12,10,12,10)}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val check=CheckBox(this).apply{
            isChecked=false
            setOnCheckedChangeListener{_,checked->if(checked)selectedPostIds.add(p.id) else selectedPostIds.remove(p.id)}
        }
        top.addView(check,LinearLayout.LayoutParams(dp(50),dp(50)))
        top.addView(TextView(this).apply{text=p.title;textSize=16f;typeface=Typeface.DEFAULT_BOLD},LinearLayout.LayoutParams(0,dp(50),1f))
        row.addView(top)
        row.addView(TextView(this).apply{text="${p.category} • ${p.likeCount} likes • ${p.commentCount} comments • ${p.viewCount} views • ${p.shareCount} shares";textSize=12f})
        row.addView(TextView(this).apply{text="${if(p.createdAt>0)java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",java.util.Locale("hi","IN")).format(java.util.Date(p.createdAt)) else ""}";textSize=11f;setTextColor(Color.DKGRAY);setPadding(0,3,0,3)})
        val actions=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val sw=Switch(this).apply{text="Comments";isChecked=p.commentsEnabled;setOnCheckedChangeListener{_,v->db.collection("channel_posts").document(p.id).update("commentsEnabled",v)}}
        actions.addView(sw,LinearLayout.LayoutParams(0,dp(48),1f))
        actions.addView(Button(this).apply{text="✏️ EDIT";setOnClickListener{editChannelPostDialog(p)}},LinearLayout.LayoutParams(dp(92),dp(48)))
        actions.addView(Button(this).apply{text="🗑 DELETE";setOnClickListener{confirmDeletePosts(listOf(p.id))}},LinearLayout.LayoutParams(dp(100),dp(48)))
        row.addView(actions)
        postsContainer.addView(row,LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(10)})
    }

    private fun selectAllAdminPosts(){
        for(i in 0 until postsContainer.childCount){
            val row=postsContainer.getChildAt(i) as? LinearLayout ?: continue
            val top=row.getChildAt(0) as? LinearLayout ?: continue
            val cb=top.getChildAt(0) as? CheckBox ?: continue
            cb.isChecked=true
        }
        toast("सभी posts select हो गईं")
    }

    private fun deleteSelectedAdminPosts(){
        if(selectedPostIds.isEmpty()){toast("पहले posts के checkbox चुनें");return}
        confirmDeletePosts(selectedPostIds.toList())
    }

    private fun deleteAllAdminPosts(){
        ChannelRepository.posts({ps->
            val ids=ps.map{it.id}
            if(ids.isEmpty()){toast("Delete करने के लिए कोई post नहीं है");return@posts}
            confirmDeletePosts(ids)
        }, {e->toast("Posts नहीं मिलीं: ${e.message ?: "Firestore error"}")})
    }

    private fun confirmDeletePosts(ids:List<String>){
        val clean=ids.filter{it.isNotBlank()}.distinct()
        if(clean.isEmpty())return
        AlertDialog.Builder(this)
            .setTitle("🗑 Delete Posts")
            .setMessage("${clean.size} post delete की जाएंगी। यह action वापस नहीं होगा।")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("DELETE",null)
            .create().also{dlg->
                dlg.setOnShowListener{
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                        dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                        deletePostIds(clean,0,dlg)
                    }
                }
                dlg.show()
            }
    }

    private fun deletePostIds(ids:List<String>, index:Int, dlg:AlertDialog){
        if(index>=ids.size){dlg.dismiss();selectedPostIds.clear();toast("${ids.size} posts delete हो गईं");loadAdminPosts();return}
        ChannelRepository.deletePost(ids[index]){ok->runOnUiThread{
            if(!ok){dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true;toast("Delete असफल — Firestore Permission/Rules जाँचें");return@runOnUiThread}
            deletePostIds(ids,index+1,dlg)
        }}
    }

    private fun editChannelPostDialog(p:ChannelPost){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(4),dp(8),dp(4))}
        val title=EditText(this).apply{hint="Post title";setText(p.title)}
        val body=EditText(this).apply{hint="Post text / details";setText(p.body);minLines=6;gravity=Gravity.TOP}
        val cat=EditText(this).apply{hint="Category";setText(p.category)}
        val comments=Switch(this).apply{text="Comments ON";isChecked=p.commentsEnabled}
        box.addView(title);box.addView(body);box.addView(cat);box.addView(comments)
        if(p.imageUrl.isNotBlank()) box.addView(TextView(this).apply{text="🖼️ Existing photo सुरक्षित रहेगी";setPadding(0,dp(6),0,dp(2))})
        if(p.fileUrl.isNotBlank()) box.addView(TextView(this).apply{text="📄 Existing document सुरक्षित रहेगा";setPadding(0,dp(2),0,dp(6))})
        AlertDialog.Builder(this).setTitle("✏️ Edit Channel Post").setView(box).setNegativeButton("Cancel",null).setPositiveButton("SAVE",null).create().also{dlg->
            dlg.setOnShowListener{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                val data=mapOf("title" to title.text.toString().trim().ifBlank{p.title},"body" to body.text.toString(),"category" to cat.text.toString().trim().ifBlank{p.category},"commentsEnabled" to comments.isChecked,"updatedAt" to System.currentTimeMillis())
                ChannelRepository.updatePost(p.id,data){ok,msg->runOnUiThread{
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true
                    if(ok){dlg.dismiss();toast("Post update हो गई");loadAdminPosts()} else toast("Edit Save असफल: ${msg ?: "Permission denied"}")
                }}
            }}
            dlg.show()
        }
    }
    private fun displayName(uri:Uri):String{var n="document.pdf";contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())n=it.getString(0)};return n}
    private fun toast(s:String){if(s.isNotBlank())Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}
}
