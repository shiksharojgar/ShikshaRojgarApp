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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var imageUri: Uri? = null
    private var fileUri: Uri? = null

    private var pageImageUri: Uri? = null
    private var pageImageField: EditText? = null

    private val selectedImageUris = mutableListOf<Uri>()

    private var imagePreviewContainer: LinearLayout? = null

    private lateinit var panel: LinearLayout
    private lateinit var globalSwitch: Switch
    private lateinit var postsContainer: LinearLayout
    private lateinit var postScroll: ScrollView

    private var globalCommentsEnabled = true

    private val selectedPostIds = mutableSetOf<String>()

    private var phoneVerificationId: String? = null

    // ---------------------------------------------------------
    // IMAGE PICKER
    // ---------------------------------------------------------

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->

            selectedImageUris.clear()
            selectedImageUris.addAll(uris.take(5))

            imageUri = selectedImageUris.firstOrNull()

            updateImagePreview()

            toast(
                if (uris.isNotEmpty()) {
                    "${uris.size} फोटो चुनी गईं"
                } else {
                    "कोई फोटो नहीं चुनी गई"
                }
            )
        }

    // ---------------------------------------------------------
    // FILE PICKER
    // ---------------------------------------------------------

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {

            fileUri = it

            toast(
                if (it != null) {
                    "PDF/Document चुना गया"
                } else {
                    ""
                }
            )
        }

    // ---------------------------------------------------------
    // HOME PAGE IMAGE PICKER
    // ---------------------------------------------------------

    private val pickPageImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            pageImageUri = uri

            pageImageField?.setText(
                if (uri != null) {
                    "नई फोटो चुनी गई — SAVE दबाएँ"
                } else {
                    ""
                }
            )

            toast(
                if (uri != null) {
                    "Page फोटो चुनी गई"
                } else {
                    ""
                }
            )
        }

    // ---------------------------------------------------------
    // ON CREATE
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        auth.useAppLanguage()

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        if (auth.currentUser != null) {

            checkAdmin(autoContinue = true)

        } else {

            setContentView(loginUi())
        }
    }

    // ---------------------------------------------------------
    // DP
    // ---------------------------------------------------------

    private fun dp(v: Int): Int {

        return (v * resources.displayMetrics.density).toInt()
    }

    // ---------------------------------------------------------
    // LOGIN UI
    // ---------------------------------------------------------

    private fun loginUi(): View {

        val content = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(28)
            )
        }

        val scroll = ScrollView(this).apply {

            isFillViewport = true

            clipToPadding = false
        }

        scroll.addView(content)

        val root = content

        root.addView(
            TextView(this).apply {

                text = "🔐 Admin Login"

                textSize = 25f

                typeface = Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(4)
                )
            }
        )

        root.addView(
            TextView(this).apply {

                text = "केवल Admin account से Login करें"

                textSize = 13f

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(18)
                )
            }
        )

        // -----------------------------------------------------
        // EMAIL LOGIN
        // -----------------------------------------------------

        root.addView(
            TextView(this).apply {

                text = "1️⃣ Email + Password"

                textSize = 16f

                typeface = Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(7)
                )
            }
        )

        val email = EditText(this).apply {

            hint = "Admin Email ID"

            inputType = 33

            setSingleLine(true)

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )
        }

        val pass = EditText(this).apply {

            hint = "Password"

            inputType = 129

            setSingleLine(true)

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )
        }

        pass.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            android.R.drawable.ic_menu_view,
            0
        )

        pass.setOnTouchListener { _, event ->

            if (
                event.action == android.view.MotionEvent.ACTION_UP &&
                event.rawX >= pass.right - 80
            ) {

                pass.inputType =
                    if (pass.inputType == 129) {
                        1
                    } else {
                        129
                    }

                pass.setSelection(
                    pass.text.length
                )

                true

            } else {

                false
            }
        }

        root.addView(
            email,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                bottomMargin = dp(8)
            }
        )

        root.addView(
            pass,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                bottomMargin = dp(10)
            }
        )

        root.addView(
            Button(this).apply {

                text = "LOGIN WITH EMAIL"

                minHeight = dp(52)

                setOnClickListener {

                    val e =
                        email.text.toString().trim()

                    val p =
                        pass.text.toString()

                    if (
                        e.isBlank() ||
                        p.isBlank()
                    ) {

                        toast(
                            "Email और Password दोनों भरें"
                        )

                        return@setOnClickListener
                    }

                    isEnabled = false

                    auth.signOut()

                    auth.signInWithEmailAndPassword(
                        e,
                        p
                    )
                        .addOnSuccessListener {

                            isEnabled = true

                            checkAdmin()
                        }
                        .addOnFailureListener {

                            isEnabled = true

                            toast(
                                "Login असफल: ${it.message}"
                            )
                        }
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        // -----------------------------------------------------
        // OTP
        // -----------------------------------------------------

        root.addView(
            TextView(this).apply {

                text = "या"

                textSize = 13f

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            TextView(this).apply {

                text = "2️⃣ Mobile OTP Login"

                textSize = 16f

                typeface = Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(7)
                )
            }
        )

        val phone = EditText(this).apply {

            hint = "मोबाइल नंबर (10 अंक या +91XXXXXXXXXX)"

            inputType = 3

            setSingleLine(true)

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )
        }

        val otp = EditText(this).apply {

            hint = "OTP (6 अंक)"

            inputType = 2

            setSingleLine(true)

            visibility = View.GONE

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )
        }

        val send = Button(this).apply {

            text = "📱 SEND OTP"

            minHeight = dp(52)
        }

        val verify = Button(this).apply {

            text = "✓ VERIFY & LOGIN"

            visibility = View.GONE

            minHeight = dp(52)
        }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                bottomMargin = dp(8)
            }
        )

        root.addView(
            send,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        root.addView(
            otp,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {

                topMargin = dp(8)

                bottomMargin = dp(8)
            }
        )

        root.addView(
            verify,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        send.setOnClickListener {

            val raw =
                phone.text.toString()
                    .trim()
                    .replace(" ", "")
                    .replace("-", "")

            val number =
                when {

                    raw.startsWith("+91") ->
                        raw

                    raw.startsWith("0") &&
                            raw.length == 11 ->
                        "+91" + raw.substring(1)

                    raw.length == 10 ->
                        "+91$raw"

                    else ->
                        ""
                }

            if (number.isBlank()) {

                toast(
                    "10 अंकों का भारतीय मोबाइल नंबर डालें"
                )

                return@setOnClickListener
            }

            send.isEnabled = false

            val options =
                PhoneAuthOptions
                    .newBuilder(auth)
                    .setPhoneNumber(number)
                    .setTimeout(
                        60L,
                        TimeUnit.SECONDS
                    )
                    .setActivity(this)
                    .setCallbacks(
                        object :
                            PhoneAuthProvider
                                .OnVerificationStateChangedCallbacks() {

                            override fun onVerificationCompleted(
                                credential: PhoneAuthCredential
                            ) {

                                auth
                                    .signInWithCredential(
                                        credential
                                    )
                                    .addOnSuccessListener {

                                        checkAdmin()
                                    }
                                    .addOnFailureListener {

                                        send.isEnabled =
                                            true

                                        toast(
                                            "Phone Login असफल: ${it.message}"
                                        )
                                    }
                            }

                            override fun onVerificationFailed(
                                e: FirebaseException
                            ) {

                                send.isEnabled =
                                    true

                                showPhoneError(e)
                            }

                            override fun onCodeSent(
                                id: String,
                                token:
                                PhoneAuthProvider
                                    .ForceResendingToken
                            ) {

                                phoneVerificationId =
                                    id

                                otp.visibility =
                                    View.VISIBLE

                                verify.visibility =
                                    View.VISIBLE

                                send.text =
                                    "OTP भेजा गया"

                                toast(
                                    "OTP भेज दिया गया"
                                )
                            }
                        }
                    )
                    .build()

            PhoneAuthProvider
                .verifyPhoneNumber(options)
        }

        verify.setOnClickListener {

            val id =
                phoneVerificationId

            val code =
                otp.text.toString().trim()

            if (
                id.isNullOrBlank() ||
                code.length < 6
            ) {

                toast(
                    "6 अंकों का OTP डालें"
                )

                return@setOnClickListener
            }

            verify.isEnabled = false

            auth.signInWithCredential(
                PhoneAuthProvider
                    .getCredential(
                        id,
                        code
                    )
            )
                .addOnSuccessListener {

                    verify.isEnabled =
                        true

                    checkAdmin()
                }
                .addOnFailureListener {

                    verify.isEnabled =
                        true

                    toast(
                        "OTP गलत है या Login असफल है"
                    )
                }
        }

        root.addView(
            Button(this).apply {

                text =
                    "ℹ️ Firebase Phone Login Setup"

                minHeight = dp(50)

                setOnClickListener {
                    showPhoneSetup()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {
                topMargin = dp(12)
            }
        )

        root.addView(
            TextView(this).apply {

                text =
                    "Email/Password या Mobile OTP से Firebase Authentication Login करें। Admin अधिकार Firebase users document से सत्यापित होता है।"

                textSize = 12f

                setPadding(
                    0,
                    dp(12),
                    0,
                    0
                )
            }
        )

        return scroll
    }

    // ---------------------------------------------------------
    // PHONE ERROR
    // ---------------------------------------------------------

    private fun showPhoneError(
        e: FirebaseException
    ) {

        val code =
            if (e is FirebaseAuthException) {
                e.errorCode
            } else {
                e.javaClass.simpleName
            }

        val raw =
            e.message ?: "Unknown Firebase error"

        val hint =
            when {

                raw.contains(
                    "CONFIGURATION_NOT_FOUND",
                    true
                ) ||
                        raw.contains(
                            "OPERATION_NOT_ALLOWED",
                            true
                        ) ->
                    "Firebase Console में Phone provider ON करें और India को SMS region policy में allow करें।"

                raw.contains(
                    "INVALID_APP_CREDENTIAL",
                    true
                ) ||
                        raw.contains(
                            "app credential",
                            true
                        ) ->
                    "इस APK के certificate SHA-1/SHA-256 को Firebase Project Settings में जोड़ें। Debug APK और Release APK के fingerprints अलग हो सकती हैं।"

                raw.contains(
                    "TOO_MANY_REQUESTS",
                    true
                ) ->
                    "बहुत अधिक OTP requests हुई हैं। कुछ समय बाद फिर कोशिश करें।"

                else ->
                    "Firebase Phone Auth की configuration जाँचें।"
            }

        AlertDialog.Builder(this)
            .setTitle("OTP नहीं भेजा गया")
            .setMessage(
                "Error: $code\n\n" +
                        "$raw\n\n" +
                        "क्या करें:\n$hint"
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // ---------------------------------------------------------
    // PHONE SETUP
    // ---------------------------------------------------------

    private fun showPhoneSetup() {

        AlertDialog.Builder(this)
            .setTitle(
                "📱 Firebase Phone Login Setup"
            )
            .setMessage(
                "1. Firebase Console → Authentication → Sign-in method → Phone ON करें।\n\n" +
                        "2. Authentication → Settings → SMS region policy में India allow करें।\n\n" +
                        "3. Project Settings → Android app में इस APK का SHA-256 और SHA-1 जोड़ें। Debug APK और Release APK के fingerprints अलग हो सकते हैं।\n\n" +
                        "4. Phone से बना user Admin तभी बनेगा जब उसी UID के users document में admin=true (या isAdmin=true / role=admin) सेट हो।"
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // ---------------------------------------------------------
    // ADMIN CHECK
    // ---------------------------------------------------------

    private fun checkAdmin(
        autoContinue: Boolean = false
    ) {

        val configuredAdminUid =
            "PfpAI3BR2XfihY4t4B5ldInDBg72"

        val user =
            auth.currentUser

        val uid =
            user?.uid

        if (uid.isNullOrBlank()) {

            toast(
                "Admin account नहीं मिला"
            )

            return
        }

        val email =
            user.email ?: "null"

        val providers =
            user.providerData
                .filter {
                    it.providerId != "firebase"
                }
                .joinToString(", ") {
                    it.providerId
                }

        val projectId =
            com.google.firebase.FirebaseApp
                .getInstance()
                .options
                .projectId
                ?: "unknown"

        fun evaluate(
            doc:
            com.google.firebase.firestore
                .DocumentSnapshot,
            source: String
        ) {

            val adminValue =
                doc.getBoolean("admin")

            val isAdminValue =
                doc.getBoolean("isAdmin")

            val roleValue =
                doc.getString("role")

            val firestoreAdmin =
                doc.exists() &&
                        (
                                adminValue == true ||
                                        isAdminValue == true ||
                                        roleValue?.equals(
                                            "admin",
                                            true
                                        ) == true
                                )

            val uidAdminFallback =
                uid == configuredAdminUid &&
                        !doc.exists()

            val isAdmin =
                firestoreAdmin ||
                        uidAdminFallback

            val message =
                "Signed-in Email: $email\n" +
                        "Provider: ${
                            if (providers.isBlank()) {
                                "unknown"
                            } else {
                                providers
                            }
                        }\n\n" +
                        "Firebase Project: $projectId\n" +
                        "Current Auth UID: $uid\n\n" +
                        "users/$uid document: ${
                            if (doc.exists()) {
                                "OK ($source)"
                            } else {
                                "NOT FOUND"
                            }
                        }\n" +
                        "admin = ${
                            adminValue ?: "null"
                        }\n" +
                        "isAdmin = ${
                            isAdminValue ?: "null"
                        }\n" +
                        "role = ${
                            roleValue ?: "null"
                        }"

            if (isAdmin) {

                if (autoContinue) {

                    showPanel()

                    return
                }

                val verificationSource =
                    if (firestoreAdmin) {
                        "Firestore users document"
                    } else {
                        "configured Admin Auth UID fallback"
                    }

                AlertDialog.Builder(this)
                    .setTitle(
                        "Admin Verification Diagnostic"
                    )
                    .setMessage(
                        message +
                                "\n\nAdmin verification सफल है।" +
                                "\nVerification source: $verificationSource"
                    )
                    .setPositiveButton(
                        "CONTINUE"
                    ) { _, _ ->
                        showPanel()
                    }
                    .setOnCancelListener {
                        showPanel()
                    }
                    .show()

            } else {

                AlertDialog.Builder(this)
                    .setTitle(
                        "Admin Verification Diagnostic"
                    )
                    .setMessage(
                        message +
                                "\n\nAdmin verification असफल है।"
                    )
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        auth.signOut()
                    }
                    .setOnCancelListener {
                        auth.signOut()
                    }
                    .show()
            }
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    evaluate(
                        doc,
                        "direct document"
                    )

                } else {

                    db.collection("users")
                        .whereEqualTo(
                            "uid",
                            uid
                        )
                        .limit(1)
                        .get()
                        .addOnSuccessListener { q ->

                            if (!q.isEmpty) {

                                evaluate(
                                    q.documents.first(),
                                    "uid field fallback"
                                )

                            } else {

                                evaluate(
                                    doc,
                                    "direct document"
                                )
                            }
                        }
                        .addOnFailureListener {

                            evaluate(
                                doc,
                                "direct document"
                            )
                        }
                }
            }
            .addOnFailureListener { e ->

                AlertDialog.Builder(this)
                    .setTitle(
                        "Admin Verification Error"
                    )
                    .setMessage(
                        "Signed-in Email: $email\n" +
                                "Firebase Project: $projectId\n" +
                                "Current Auth UID: $uid\n\n" +
                                "Firestore पढ़ने में समस्या:\n" +
                                "${e.message ?: "Unknown error"}"
                    )
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        auth.signOut()
                    }
                    .show()
            }
    }

    // ---------------------------------------------------------
    // ADMIN PANEL
    // ---------------------------------------------------------

    private fun showPanel() {

        panel =
            LinearLayout(this)

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(
                        245,
                        248,
                        252
                    )
                )
            }

        setContentView(root)

        val fixed =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(10),
                    dp(8),
                    dp(10),
                    dp(6)
                )

                setBackgroundColor(
                    Color.WHITE
                )

                elevation = 8f
            }

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        fixed.addView(
            TextView(this).apply {

                text =
                    "⚙ शिक्षा रोजगार Channel Admin"

                textSize = 20f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        7,
                        89,
                        133
                    )
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(5)
                )
            }
        )

        // -----------------------------------------------------
        // LOGOUT
        // -----------------------------------------------------

        fixed.addView(
            Button(this).apply {

                text =
                    "🚪 Logout"

                minHeight =
                    dp(40)

                setOnClickListener {

                    auth.signOut()

                    Toast.makeText(
                        this@AdminActivity,
                        "Admin Logout हो गया",
                        Toast.LENGTH_SHORT
                    ).show()

                    setContentView(
                        loginUi()
                    )
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(40)
            ).apply {
                bottomMargin = dp(4)
            }
        )

        // -----------------------------------------------------
        // GLOBAL COMMENTS
        // -----------------------------------------------------

        globalSwitch =
            Switch(this).apply {

                text =
                    "सभी Posts के Comments"

                textSize = 14f

                minHeight = dp(40)
            }

        fixed.addView(
            globalSwitch,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        db.document(
            "channel_config/main"
        )
            .get()
            .addOnSuccessListener {

                globalCommentsEnabled =
                    it.getBoolean(
                        "commentsEnabled"
                    ) ?: true

                globalSwitch.isChecked =
                    globalCommentsEnabled
            }

        globalSwitch.setOnCheckedChangeListener {
                _,
                checked ->

            globalCommentsEnabled =
                checked

            db.document(
                "channel_config/main"
            ).set(
                mapOf(
                    "commentsEnabled" to checked
                ),
                com.google.firebase.firestore
                    .SetOptions.merge()
            )
        }

        // -----------------------------------------------------
        // NEW POST
        // -----------------------------------------------------

        fixed.addView(
            Button(this).apply {

                text =
                    "📢 CREATE NEW CHANNEL POST"

                minHeight =
                    dp(44)

                setOnClickListener {
                    newPostDialog()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(44)
            ).apply {
                topMargin = dp(3)
            }
        )

        // -----------------------------------------------------
        // POST TOOLS
        // -----------------------------------------------------

        val postTools =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        postTools.addView(
            Button(this).apply {

                text =
                    "☑ SELECT ALL"

                minHeight =
                    dp(40)

                setOnClickListener {
                    selectAllAdminPosts()
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            ).apply {
                rightMargin = dp(3)
            }
        )

        postTools.addView(
            Button(this).apply {

                text =
                    "🗑 DELETE SELECTED"

                minHeight =
                    dp(40)

                setOnClickListener {
                    deleteSelectedAdminPosts()
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            ).apply {
                leftMargin = dp(3)
            }
        )

        fixed.addView(
            postTools
        )

        // -----------------------------------------------------
        // DELETE ALL
        // -----------------------------------------------------

        fixed.addView(
            Button(this).apply {

                text =
                    "🗑 DELETE ALL POSTS"

                minHeight =
                    dp(40)

                setOnClickListener {
                    deleteAllAdminPosts()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(40)
            ).apply {
                topMargin = dp(3)
            }
        )

        // -----------------------------------------------------
        // HOME PAGES
        // -----------------------------------------------------

        fixed.addView(
            TextView(this).apply {

                text =
                    "📚 HOME PAGES"

                textSize = 16f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        7,
                        89,
                        133
                    )
                )

                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(3)
                )
            }
        )

        val pageManager =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        val pageDefs =
            listOf(
                "syllabus" to "Syllabus",
                "notices" to "Notices",
                "career" to "Career Guide",
                "tools" to "Useful Tools"
            )

        pageDefs.forEach { (id, label) ->

            pageManager.addView(
                Button(this).apply {

                    text =
                        "✏️ $label — ADD / EDIT"

                    minHeight =
                        dp(38)

                    setPadding(
                        dp(5),
                        0,
                        dp(5),
                        0
                    )

                    setOnClickListener {
                        editHomePage(
                            id,
                            label
                        )
                    }
                }
            )
        }

        fixed.addView(
            pageManager
        )

        // -----------------------------------------------------
        // MEDIA STORAGE BUTTON
        // -----------------------------------------------------

        fixed.addView(
            Button(this).apply {

                text =
                    "💾 Channel Media Storage"

                textSize = 12f

                minHeight =
                    dp(38)

                setPadding(
                    dp(6),
                    0,
                    dp(6),
                    0
                )

                setOnClickListener {
                    showChannelMediaStorage()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(38)
            ).apply {
                topMargin = dp(3)
            }
        )

        // -----------------------------------------------------
        // ANALYTICS BUTTON
        // -----------------------------------------------------

        fixed.addView(
            Button(this).apply {

                text =
                    "📊 CHANNEL ANALYSIS"

                textSize = 12f

                minHeight =
                    dp(38)

                setOnClickListener {
                    showAnalytics()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(38)
            ).apply {
                topMargin = dp(3)
            }
        )

        root.addView(
            fixed,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        // -----------------------------------------------------
        // POST HEADER
        // -----------------------------------------------------

        val postHeader =
            TextView(this).apply {

                text =
                    "📋 Channel Posts — नई post नीचे दिखाई देगी"

                textSize = 15f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        7,
                        89,
                        133
                    )
                )

                setPadding(
                    dp(10),
                    dp(6),
                    dp(10),
                    dp(5)
                )

                setBackgroundColor(
                    Color.rgb(
                        245,
                        248,
                        252
                    )
                )
            }

        root.addView(
            postHeader
        )

        // -----------------------------------------------------
        // POST SCROLL
        // -----------------------------------------------------

        postScroll =
            ScrollView(this).apply {

                isFillViewport =
                    true

                isVerticalScrollBarEnabled =
                    true
            }

        postsContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(10),
                    0,
                    dp(10),
                    dp(18)
                )
            }

        postScroll.addView(
            postsContainer
        )

        root.addView(
            postScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        loadAdminPosts()
    }

    // ---------------------------------------------------------
    // MEDIA STORAGE
    // ---------------------------------------------------------

    private fun showChannelMediaStorage() {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "💾 Channel Media Storage"
                )
                .setMessage(
                    "Storage calculate हो रहा है…"
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .create()

        dialog.show()

        db.collection(
            "channel_media"
        )
            .get()
            .addOnSuccessListener { snap ->

                var totalBytes = 0L
                var fileCount = 0
                var imageCount = 0
                var documentCount = 0

                snap.documents.forEach { doc ->

                    val size =
                        doc.getLong("size")
                            ?: 0L

                    totalBytes +=
                        size.coerceAtLeast(
                            0L
                        )

                    fileCount++

                    val folder =
                        doc.getString(
                            "folder"
                        ).orEmpty()

                    if (folder == "images") {

                        imageCount++

                    } else {

                        documentCount++
                    }
                }

                val mb =
                    totalBytes.toDouble() /
                            (
                                    1024.0 *
                                            1024.0
                                    )

                val gb =
                    totalBytes.toDouble() /
                            (
                                    1024.0 *
                                            1024.0 *
                                            1024.0
                                    )

                val freeLimitGb =
                    1.0

                val percent =
                    (
                            gb /
                                    freeLimitGb *
                                    100.0
                            )
                        .coerceAtMost(
                            100.0
                        )

                val message =
                    """
                    💾 Channel Media
                    
                    📁 Total media files: $fileCount
                    🖼️ Images: $imageCount
                    📄 Documents: $documentCount
                    
                    📦 Uploaded media:
                    ${String.format(Locale.US, "%.2f MB", mb)}
                    
                    ${String.format(Locale.US, "%.4f GB", gb)}
                    
                    📊 Firestore free storage:
                    1 GiB shared Firestore limit
                    
                    📈 Approx. media usage:
                    ${String.format(Locale.US, "%.2f%%", percent)}
                    
                    ⚠️ यह Channel media का declared
                    file-size total है।
                    
                    Firebase Console में दिखने वाला
                    actual Firestore storage
                    indexes/document overhead सहित
                    इससे थोड़ा अलग हो सकता है।
                    
                    🗑️ Delete की गई post की unused
                    image/PDF और उसके chunks
                    नई delete system से हटेंगे।
                    """.trimIndent()

                dialog.setMessage(
                    message
                )
            }
            .addOnFailureListener { e ->

                dialog.setMessage(
                    "Channel media storage पढ़ा नहीं जा सका:\n\n" +
                            (
                                    e.message
                                        ?: "Firestore error"
                                    )
                )
            }
    }

    // ---------------------------------------------------------
    // ANALYTICS
    // ---------------------------------------------------------

    private fun showAnalytics() {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "📊 Shiksha Rojgar Analytics"
                )
                .setMessage(
                    "लोड हो रहा है…"
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .create()

        dialog.show()

        db.document(
            "channel_config/main"
        )
            .get()
            .addOnSuccessListener { d ->

                val msg =
                    "👥 Channel Followers: ${
                        d.getLong(
                            "followerCount"
                        ) ?: 0
                    }\n" +

                            "👁 Channel Post Views: ${
                                d.getLong(
                                    "totalPostViews"
                                ) ?: 0
                            }\n" +

                            "↗ Channel Post Shares: ${
                                d.getLong(
                                    "totalPostShares"
                                ) ?: 0
                            }\n" +

                            "👍 Channel Likes: ${
                                d.getLong(
                                    "totalLikes"
                                ) ?: 0
                            }\n" +

                            "💬 Channel Comments: ${
                                d.getLong(
                                    "totalComments"
                                ) ?: 0
                            }\n\n" +

                            "📱 App First-use / Install Users: ${
                                d.getLong(
                                    "totalAppUsers"
                                ) ?: 0
                            }\n" +

                            "📈 Active User-Days: ${
                                d.getLong(
                                    "totalActiveUserDays"
                                ) ?: 0
                            }\n\n" +

                            "Analytics channel_config/main के aggregate counters से पढ़े जा रहे हैं।\n\n" +

                            "नोट: Website से APK file download और APK install अलग metrics हैं।"

                dialog.setMessage(
                    msg
                )
            }
            .addOnFailureListener { e ->

                dialog.setMessage(
                    "Channel analytics पढ़ा नहीं जा सका: " +
                            (
                                    e.message
                                        ?: "Firestore error"
                                    )
                )
            }
    }

    // ---------------------------------------------------------
    // NEW POST
    // ---------------------------------------------------------

    private fun newPostDialog() {

        selectedImageUris.clear()

        imageUri = null

        fileUri = null

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    dp(5),
                    dp(8),
                    dp(5)
                )
            }

        val title =
            EditText(this).apply {

                hint =
                    "पोस्ट शीर्षक"
            }

        val body =
            EditText(this).apply {

                hint =
                    "विवरण / आदेश का पाठ"

                minLines = 4
            }

        val cat =
            EditText(this).apply {

                hint =
                    "Category: आदेश/निर्देश"
            }

        box.addView(title)
        box.addView(body)
        box.addView(cat)

        // -----------------------------------------------------
        // IMAGE
        // -----------------------------------------------------

        box.addView(
            Button(this).apply {

                text =
                    "🖼️ फोटो / Gallery"

                setOnClickListener {

                    pickImage.launch(
                        "image/*"
                    )
                }
            }
        )

        imagePreviewContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    6,
                    0,
                    6
                )
            }

        box.addView(
            imagePreviewContainer,
            LinearLayout.LayoutParams(
                -1,
                dp(90)
            )
        )

        // -----------------------------------------------------
        // FILE
        // -----------------------------------------------------

        box.addView(
            Button(this).apply {

                text =
                    "📄 PDF/Document चुनें"

                setOnClickListener {

                    pickFile.launch(
                        arrayOf(
                            "application/pdf",
                            "image/*",
                            "text/plain"
                        )
                    )
                }
            }
        )

        // -----------------------------------------------------
        // COMMENTS
        // -----------------------------------------------------

        val comments =
            Switch(this).apply {

                text =
                    "इस Post पर Comments ON"

                isChecked =
                    globalCommentsEnabled

                isEnabled =
                    globalCommentsEnabled
            }

        box.addView(
            comments
        )

        AlertDialog.Builder(this)
            .setTitle(
                "नई Channel Post"
            )
            .setView(box)
            .setPositiveButton(
                "PUBLISH",
                null
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .create()
            .also { d ->

                d.setOnShowListener {

                    d.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val rawTitle =
                            title.text
                                .toString()
                                .trim()

                        val rawBody =
                            body.text
                                .toString()
                                .trim()

                        val chosenFile =
                            fileUri

                        val finalTitle =
                            if (
                                rawTitle.isNotBlank()
                            ) {

                                rawTitle

                            } else {

                                when {

                                    rawBody.isNotBlank() ->
                                        rawBody
                                            .replace(
                                                "\n",
                                                " "
                                            )
                                            .take(90)

                                    selectedImageUris.isNotEmpty() ->
                                        "📷 शिक्षा रोजगार फोटो अपडेट"

                                    chosenFile != null ->
                                        displayName(
                                            chosenFile
                                        )
                                            .substringBeforeLast(
                                                '.'
                                            )
                                            .ifBlank {
                                                "शिक्षा रोजगार अपडेट"
                                            }

                                    else ->
                                        "शिक्षा रोजगार अपडेट"
                                }
                            }

                        if (
                            rawBody.isBlank() &&
                            selectedImageUris.isEmpty() &&
                            chosenFile == null
                        ) {

                            toast(
                                "कम से कम फोटो, PDF या विवरण जोड़ें"
                            )

                            return@setOnClickListener
                        }

                        d.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled = false

                        val category =
                            cat.text
                                .toString()
                                .trim()
                                .ifBlank {
                                    "आदेश/निर्देश"
                                }

                        val commentsEnabled =
                            globalCommentsEnabled &&
                                    comments.isChecked

                        // -------------------------------------------------
                        // TEXT ONLY
                        // -------------------------------------------------

                        if (
                            rawBody.isNotBlank() &&
                            selectedImageUris.isEmpty() &&
                            chosenFile == null
                        ) {

                            ChannelRepository
                                .createPost(
                                    mapOf(
                                        "title" to finalTitle,
                                        "body" to rawBody,
                                        "published" to true,
                                        "postType" to "text",
                                        "category" to category,
                                        "imageUrl" to "",
                                        "fileUrl" to "",
                                        "fileName" to "",
                                        "imageMime" to "",
                                        "fileMime" to "",
                                        "createdAt" to System.currentTimeMillis(),
                                        "commentsEnabled" to commentsEnabled,
                                        "likeCount" to 0L,
                                        "commentCount" to 0L,
                                        "viewCount" to 0L,
                                        "shareCount" to 0L
                                    )
                                ) { ok, msg ->

                                    runOnUiThread {

                                        d.getButton(
                                            AlertDialog.BUTTON_POSITIVE
                                        ).isEnabled = true

                                        if (ok) {

                                            d.dismiss()

                                            toast(
                                                "Text-only Post Publish हो गई"
                                            )

                                            loadAdminPosts()

                                        } else {

                                            toast(
                                                "Publish असफल: ${
                                                    msg
                                                        ?: "Firestore permission/error"
                                                }"
                                            )
                                        }
                                    }
                                }

                        } else {

                            uploadBoth(
                                finalTitle,
                                rawBody,
                                category,
                                commentsEnabled
                            ) { ok, msg ->

                                runOnUiThread {

                                    d.getButton(
                                        AlertDialog.BUTTON_POSITIVE
                                    ).isEnabled = true

                                    if (ok) {

                                        d.dismiss()

                                        toast(
                                            "Post Publish हो गई"
                                        )

                                        loadAdminPosts()

                                    } else {

                                        toast(
                                            "Publish असफल: ${
                                                msg
                                                    ?: "Firestore/media error"
                                            }"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                d.show()
            }
    }

    // ---------------------------------------------------------
    // IMAGE PREVIEW
    // ---------------------------------------------------------

    private fun updateImagePreview() {

        val container =
            imagePreviewContainer
                ?: return

        container.removeAllViews()

        selectedImageUris
            .take(5)
            .forEach { uri ->

                val iv =
                    ImageView(this).apply {

                        setImageURI(uri)

                        scaleType =
                            ImageView.ScaleType.CENTER_CROP

                        setPadding(
                            3,
                            3,
                            3,
                            3
                        )
                    }

                container.addView(
                    iv,
                    LinearLayout.LayoutParams(
                        dp(78),
                        dp(78)
                    ).apply {
                       
