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

    // =========================================================
    // IMAGE PICKER
    // =========================================================

    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->

            selectedImageUris.clear()
            selectedImageUris.addAll(
                uris.take(5)
            )

            imageUri =
                selectedImageUris.firstOrNull()

            updateImagePreview()

            toast(
                if (uris.isNotEmpty()) {
                    "${uris.size} फोटो चुनी गईं"
                } else {
                    "कोई फोटो नहीं चुनी गई"
                }
            )
        }

    // =========================================================
    // FILE PICKER
    // =========================================================

    private val pickFile =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) {

            fileUri = it

            toast(
                if (it != null) {
                    "PDF/Document चुना गया"
                } else {
                    ""
                }
            )
        }

    // =========================================================
    // HOME PAGE IMAGE PICKER
    // =========================================================

    private val pickPageImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

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

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        auth.useAppLanguage()

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        if (auth.currentUser != null) {

            checkAdmin(
                autoContinue = true
            )

        } else {

            setContentView(
                loginUi()
            )
        }
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(v: Int): Int {

        return (
            v *
                resources.displayMetrics.density
            ).toInt()
    }

    // =========================================================
    // LOGIN UI
    // =========================================================

    private fun loginUi(): View {

        val content =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(28)
                )
            }

        val scroll =
            ScrollView(this).apply {

                isFillViewport = true

                clipToPadding = false
            }

        scroll.addView(content)

        val root = content

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        root.addView(
            TextView(this).apply {

                text = "🔐 Admin Login"

                textSize = 25f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(
                        7,
                        89,
                        133
                    )
                )

                gravity =
                    Gravity.CENTER

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

                text =
                    "केवल Admin account से Login करें"

                textSize = 13f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(18)
                )
            }
        )

        // =====================================================
        // EMAIL LOGIN
        // =====================================================

        root.addView(
            TextView(this).apply {

                text =
                    "1️⃣ Email + Password"

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
                    dp(8),
                    0,
                    dp(7)
                )
            }
        )

        val email =
            EditText(this).apply {

                hint =
                    "Admin Email ID"

                inputType =
                    33

                setSingleLine(true)

                setPadding(
                    dp(14),
                    0,
                    dp(14),
                    0
                )
            }

        val pass =
            EditText(this).apply {

                hint =
                    "Password"

                inputType =
                    129

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
                event.action ==
                    android.view.MotionEvent.ACTION_UP &&
                event.rawX >=
                    pass.right - 80
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

                bottomMargin =
                    dp(8)
            }
        )

        root.addView(
            pass,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {

                bottomMargin =
                    dp(10)
            }
        )

        root.addView(
            Button(this).apply {

                text =
                    "LOGIN WITH EMAIL"

                minHeight =
                    dp(52)

                setOnClickListener {

                    val e =
                        email.text
                            .toString()
                            .trim()

                    val p =
                        pass.text
                            .toString()

                    if (
                        e.isBlank() ||
                        p.isBlank()
                    ) {

                        toast(
                            "Email और Password दोनों भरें"
                        )

                        return@setOnClickListener
                    }

                    isEnabled =
                        false

                    auth.signOut()

                    auth
                        .signInWithEmailAndPassword(
                            e,
                            p
                        )
                        .addOnSuccessListener {

                            isEnabled =
                                true

                            checkAdmin()
                        }
                        .addOnFailureListener {

                            isEnabled =
                                true

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

        // =====================================================
        // OTP
        // =====================================================

        root.addView(
            TextView(this).apply {

                text =
                    "या"

                textSize =
                    13f

                gravity =
                    Gravity.CENTER

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

                text =
                    "2️⃣ Mobile OTP Login"

                textSize =
                    16f

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
                    dp(4),
                    0,
                    dp(7)
                )
            }
        )

        val phone =
            EditText(this).apply {

                hint =
                    "मोबाइल नंबर (10 अंक या +91XXXXXXXXXX)"

                inputType =
                    3

                setSingleLine(true)

                setPadding(
                    dp(14),
                    0,
                    dp(14),
                    0
                )
            }

        val otp =
            EditText(this).apply {

                hint =
                    "OTP (6 अंक)"

                inputType =
                    2

                setSingleLine(true)

                visibility =
                    View.GONE

                setPadding(
                    dp(14),
                    0,
                    dp(14),
                    0
                )
            }

        val send =
            Button(this).apply {

                text =
                    "📱 SEND OTP"

                minHeight =
                    dp(52)
            }

        val verify =
            Button(this).apply {

                text =
                    "✓ VERIFY & LOGIN"

                visibility =
                    View.GONE

                minHeight =
                    dp(52)
            }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {

                bottomMargin =
                    dp(8)
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

                topMargin =
                    dp(8)

                bottomMargin =
                    dp(8)
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
                phone.text
                    .toString()
                    .trim()
                    .replace(
                        " ",
                        ""
                    )
                    .replace(
                        "-",
                        ""
                    )

            val number =
                when {

                    raw.startsWith(
                        "+91"
                    ) ->
                        raw

                    raw.startsWith(
                        "0"
                    ) &&
                        raw.length == 11 ->
                        "+91" +
                            raw.substring(1)

                    raw.length == 10 ->
                        "+91$raw"

                    else ->
                        ""
                }

            if (
                number.isBlank()
            ) {

                toast(
                    "10 अंकों का भारतीय मोबाइल नंबर डालें"
                )

                return@setOnClickListener
            }

            send.isEnabled =
                false

            val options =
                PhoneAuthOptions
                    .newBuilder(auth)
                    .setPhoneNumber(
                        number
                    )
                    .setTimeout(
                        60L,
                        TimeUnit.SECONDS
                    )
                    .setActivity(
                        this
                    )
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

                                showPhoneError(
                                    e
                                )
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
                .verifyPhoneNumber(
                    options
                )
        }

        verify.setOnClickListener {

            val id =
                phoneVerificationId

            val code =
                otp.text
                    .toString()
                    .trim()

            if (
                id.isNullOrBlank() ||
                code.length < 6
            ) {

                toast(
                    "6 अंकों का OTP डालें"
                )

                return@setOnClickListener
            }

            verify.isEnabled =
                false

            auth
                .signInWithCredential(
                    PhoneAuthProvider.getCredential(
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

                minHeight =
                    dp(50)

                setOnClickListener {

                    showPhoneSetup()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {

                topMargin =
                    dp(12)
            }
        )

        root.addView(
            TextView(this).apply {

                text =
                    "Email/Password या Mobile OTP से Firebase Authentication Login करें। Admin अधिकार Firebase users document से सत्यापित होता है।"

                textSize =
                    12f

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

    // =========================================================
    // PHONE ERROR
    // =========================================================

    private fun showPhoneError(
        e: FirebaseException
    ) {

        val code =
            if (
                e is FirebaseAuthException
            ) {

                e.errorCode

            } else {

                e.javaClass.simpleName
            }

        val raw =
            e.message
                ?: "Unknown Firebase error"

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
                    "इस APK के certificate SHA-1/SHA-256 को Firebase Project Settings में जोड़ें। Debug APK और Release APK के fingerprints अलग हो सकते हैं।"

                raw.contains(
                    "TOO_MANY_REQUESTS",
                    true
                ) ->
                    "बहुत अधिक OTP requests हुई हैं। कुछ समय बाद फिर कोशिश करें।"

                else ->
                    "Firebase Phone Auth की configuration जाँचें।"
            }

        AlertDialog.Builder(this)
            .setTitle(
                "OTP नहीं भेजा गया"
            )
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

    // =========================================================
    // PHONE SETUP
    // =========================================================

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

    // =========================================================
    // ADMIN CHECK
    // =========================================================

    private fun checkAdmin(
        autoContinue: Boolean = false
    ) {

        val configuredAdminUid =
            "PfpAI3BR2XfihY4t4B5ldInDBg72"

        val user =
            auth.currentUser

        val uid =
            user?.uid

        if (
            uid.isNullOrBlank()
        ) {

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
                com.google.firebase.firestore.DocumentSnapshot,
            source: String
        ) {

            val adminValue =
                doc.getBoolean(
                    "admin"
                )

            val isAdminValue =
                doc.getBoolean(
                    "isAdmin"
                )

            val roleValue =
                doc.getString(
                    "role"
                )

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
                uid ==
                    configuredAdminUid &&
                    !doc.exists()

            val isAdmin =
                firestoreAdmin ||
                    uidAdminFallback

            val message =
                "Signed-in Email: $email\n" +
                    "Provider: ${
                        if (
                            providers.isBlank()
                        ) {
                            "unknown"
                        } else {
                            providers
                        }
                    }\n\n" +
                    "Firebase Project: $projectId\n" +
                    "Current Auth UID: $uid\n\n" +
                    "users/$uid document: ${
                        if (
                            doc.exists()
                        ) {
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

        db.collection(
            "users"
        )
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    evaluate(
                        doc,
                        "direct document"
                    )

                } else {

                    db.collection(
                        "users"
                    )
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
                            (
                                e.message
                                    ?: "Unknown error"
                            )
                    )
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->

                        auth.signOut()
                    }
                    .show()
            }
    }

        // =========================================================
    // ADMIN PANEL
    // =========================================================

    private fun showPanel() {

        panel = LinearLayout(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(
                Color.rgb(245, 248, 252)
            )
        }

        setContentView(root)

        // =====================================================
        // FIXED ADMIN CONTROLS
        // =====================================================

        val fixed = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(5)
            )

            setBackgroundColor(Color.WHITE)

            elevation = 6f
        }

        // =====================================================
        // HEADER
        // =====================================================

        fixed.addView(
            TextView(this).apply {

                text = "⚙ शिक्षा रोजगार Channel Admin"

                textSize = 19f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.rgb(7, 89, 133)
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(3)
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(32)
            )
        )

        // =====================================================
        // LOGOUT + GLOBAL COMMENTS
        // =====================================================

        val topRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        topRow.addView(
            Button(this).apply {

                text = "🚪 Logout"

                textSize = 11f

                minHeight = 0

                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
                )

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
                dp(105),
                dp(38)
            ).apply {
                rightMargin = dp(5)
            }
        )

        globalSwitch =
            Switch(this).apply {

                text = "Comments"

                textSize = 12f

                setPadding(
                    0,
                    0,
                    0,
                    0
                )
            }

        topRow.addView(
            globalSwitch,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            )
        )

        fixed.addView(topRow)

        // =====================================================
        // LOAD GLOBAL COMMENTS
        // =====================================================

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
            )
                .set(
                    mapOf(
                        "commentsEnabled" to checked
                    ),
                    com.google.firebase.firestore
                        .SetOptions.merge()
                )
        }

        // =====================================================
        // NEW CHANNEL POST
        // =====================================================

        fixed.addView(
            Button(this).apply {

                text =
                    "📢  NEW CHANNEL POST"

                textSize = 13f

                setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(0, 120, 215)
                )

                minHeight = 0

                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
                )

                setOnClickListener {

                    newPostDialog()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            ).apply {

                topMargin = dp(3)

                bottomMargin = dp(3)
            }
        )

        // =====================================================
        // POST MANAGEMENT BUTTONS
        // =====================================================

        val postTools =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        postTools.addView(
            Button(this).apply {

                text = "☑ ALL"

                textSize = 10f

                minHeight = 0

                setPadding(
                    dp(2),
                    0,
                    dp(2),
                    0
                )

                setOnClickListener {

                    selectAllAdminPosts()
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(36),
                1f
            ).apply {

                rightMargin = dp(3)
            }
        )

        postTools.addView(
            Button(this).apply {

                text =
                    "🗑 DELETE SELECTED"

                textSize = 9f

                minHeight = 0

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        210,
                        55,
                        55
                    )
                )

                setPadding(
                    dp(2),
                    0,
                    dp(2),
                    0
                )

                setOnClickListener {

                    deleteSelectedAdminPosts()
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(36),
                1.35f
            ).apply {

                leftMargin = dp(3)
            }
        )

        fixed.addView(postTools)

        // =====================================================
        // DELETE ALL
        // =====================================================

        fixed.addView(
            Button(this).apply {

                text =
                    "🗑 DELETE ALL POSTS"

                textSize = 10f

                minHeight = 0

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        185,
                        35,
                        35
                    )
                )

                setPadding(
                    dp(2),
                    0,
                    dp(2),
                    0
                )

                setOnClickListener {

                    deleteAllAdminPosts()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(34)
            ).apply {

                topMargin = dp(3)
            }
        )
        // =====================================================
        // MANAGE HOME SUB-CATEGORIES
        // =====================================================
        fixed.addView(
            Button(this).apply {

                text = "🔽 MANAGE SUB-CATEGORIES"

                textSize = 12f

                setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        89,
                        133
                    )
                )

                minHeight = 0

                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
                )

                setOnClickListener {

                    showHomeSubCategoryManager()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            ).apply {

                topMargin = dp(2)
                bottomMargin = dp(3)
            }
        )
        // =====================================================
        // HOME CATEGORY MANAGEMENT
        // =====================================================

        fixed.addView(
            TextView(this).apply {

                text =
                    "🏠 HOME CATEGORY MANAGEMENT"

                textSize = 14f

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
                    dp(7),
                    0,
                    dp(2)
                )
            }
        )

        fixed.addView(
            TextView(this).apply {

                text =
                    "Home के Teacher, Student, School, Vacancy आदि buttons यहाँ से बदलें"

                textSize = 10f

                setTextColor(
                    Color.DKGRAY
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(3)
                )
            }
        )

        fixed.addView(
            Button(this).apply {

                text =
                    "⚙️ MANAGE HOME CATEGORIES"

                textSize = 12f

                setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        89,
                        133
                    )
                )

                minHeight = 0

                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
                )

                setOnClickListener {

                    showHomeCategoryManager()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            ).apply {

                topMargin = dp(2)
                bottomMargin = dp(3)
            }
        )

        // =====================================================
        // EXISTING HOME PAGES
        // =====================================================

        fixed.addView(
            TextView(this).apply {

                text = "📚 HOME PAGES / CONTENT"

                textSize = 13f

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
                    dp(2)
                )
            }
        )

        val pageManager =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        val pageDefs =
            listOf(
                "syllabus" to "Syllabus",
                "notices" to "Notice",
                "career" to "Career",
                "tools" to "Tools",
                "study_material" to "Study Material"
            )

        pageDefs.forEach {
                (id, label) ->

            pageManager.addView(
                Button(this).apply {

                    text = label

                    textSize = 8f

                    minHeight = 0

                    setPadding(
                        dp(1),
                        0,
                        dp(1),
                        0
                    )

                    setOnClickListener {

                        editHomePage(
                            id,
                            label
                        )
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(34),
                    1f
                ).apply {

                    leftMargin = dp(1)

                    rightMargin = dp(1)
                }
            )
        }

        fixed.addView(pageManager)

        // =====================================================
        // MEDIA STORAGE + ANALYTICS
        // =====================================================

        val utilityRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        utilityRow.addView(
            Button(this).apply {

                text = "💾 Media"

                textSize = 9f

                minHeight = 0

                setPadding(
                    dp(2),
                    0,
                    dp(2),
                    0
                )

                setOnClickListener {

                    showChannelMediaStorage()
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            ).apply {

                rightMargin = dp(3)
            }
        )

        utilityRow.addView(
            Button(this).apply {

                text = "📊 Analytics"

                textSize = 9f

                minHeight = 0

                setPadding(
                    dp(2),
                    0,
                    dp(2),
                    0
                )

                setOnClickListener {

                    showAnalytics()
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            ).apply {

                leftMargin = dp(3)
            }
        )

        fixed.addView(
            utilityRow,
            LinearLayout.LayoutParams(
                -1,
                dp(34)
            ).apply {

                topMargin = dp(3)
            }
        )

        // =====================================================
        // ADD FIXED PANEL
        // =====================================================

        root.addView(
            fixed,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================================
        // POSTS HEADER
        // =====================================================

        root.addView(
            TextView(this).apply {

                text =
                    "📋 Channel Posts — पुरानी ऊपर • नई नीचे"

                textSize = 14f

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
                    dp(5),
                    dp(10),
                    dp(4)
                )

                setBackgroundColor(
                    Color.rgb(
                        235,
                        241,
                        248
                    )
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(34)
            )
        )

        // =====================================================
        // POST SCROLL
        // =====================================================

        postScroll =
            ScrollView(this).apply {

                isFillViewport = true

                isVerticalScrollBarEnabled =
                    true

                clipToPadding = false
            }

        postsContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    dp(5),
                    dp(8),
                    dp(20)
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

    // =========================================================
    // DEFAULT HOME CATEGORIES
    // =========================================================

    private fun defaultHomeCategories():
        List<Map<String, Any>> {

        return listOf(

            mapOf(
                "id" to "teacher",
                "name" to "Teacher",
                "icon" to "🧑‍🏫",
                "url" to
                    "https://www.shiksharojgar.com/2026/03/teacher-govt-employees.html",
                "pageId" to "",
                "position" to 1L
            ),

            mapOf(
                "id" to "student",
                "name" to "Student",
                "icon" to "🎓",
                "url" to
                    "https://www.shiksharojgar.com/2026/03/College%20%20University%20Students.html",
                "pageId" to "",
                "position" to 2L
            ),

            mapOf(
                "id" to "school",
                "name" to "School",
                "icon" to "🏫",
                "url" to
                    "https://www.shiksharojgar.com/2026/03/school-students-1-12-section-page.html",
                "pageId" to "",
                "position" to 3L
            ),

            mapOf(
                "id" to "vacancy",
                "name" to "Vacancy",
                "icon" to "💼",
                "url" to
                    "https://www.shiksharojgar.com/2026/03/latest-jobs-page.html",
                "pageId" to "",
                "position" to 4L
            ),

            mapOf(
                "id" to "result",
                "name" to "Result",
                "icon" to "📋",
                "url" to
                    "https://www.shiksharojgar.com/2026/03/results.html",
                "pageId" to "",
                "position" to 5L
            ),

            mapOf(
                "id" to "admit_card",
                "name" to "Admit Card",
                "icon" to "🎫",
                "url" to
                    "https://www.shiksharojgar.com/2026/01/admit-card-download-zone.html",
                "pageId" to "",
                "position" to 6L
            ),

            mapOf(
                "id" to "syllabus",
                "name" to "Syllabus",
                "icon" to "📚",
                "url" to "",
                "pageId" to "syllabus",
                "position" to 7L
            ),

            mapOf(
                "id" to "study_material",
                "name" to "Study Material",
                "icon" to "📖",
                "url" to "",
                "pageId" to "study_material",
                "position" to 8L
            ),

            mapOf(
                "id" to "career",
                "name" to "Career Guide",
                "icon" to "🚀",
                "url" to "",
                "pageId" to "career",
                "position" to 9L
            ),

            mapOf(
                "id" to "notices",
                "name" to "Notice",
                "icon" to "📢",
                "url" to "",
                "pageId" to "notices",
                "position" to 10L
            ),

            mapOf(
                "id" to "tools",
                "name" to "Useful Tools",
                "icon" to "🛠️",
                "url" to "",
                "pageId" to "tools",
                "position" to 11L
            )
        )
    }

    // =========================================================
    // HOME CATEGORY MANAGER
    // =========================================================

    private fun showHomeCategoryManager() {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "🏠 Home Categories"
                )
                .setMessage(
                    "Categories load हो रही हैं…"
                )
                .setPositiveButton(
                    "CLOSE",
                    null
                )
                .create()

        dialog.show()

        db.collection(
            "home_categories"
        )
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {

                    seedDefaultHomeCategories {
                        runOnUiThread {
                            dialog.dismiss()
                            showHomeCategoryManager()
                        }
                    }

                    return@addOnSuccessListener
                }

                dialog.dismiss()

                showHomeCategoryList(
                    snap.documents
                )
            }
            .addOnFailureListener { e ->

                dialog.setMessage(
                    "Categories पढ़ने में समस्या:\n\n" +
                        (
                            e.message
                                ?: "Firestore error"
                            )
                )
            }
    }

    // =========================================================
    // SEED DEFAULT CATEGORIES
    // =========================================================

    private fun seedDefaultHomeCategories(
        done: () -> Unit
    ) {

        val defaults =
            defaultHomeCategories()

        fun saveNext(
            index: Int
        ) {

            if (
                index >= defaults.size
            ) {

                done()

                return
            }

            val item =
                defaults[index]

            val id =
                item["id"]
                    .toString()

            db.collection(
                "home_categories"
            )
                .document(id)
                .set(item)
                .addOnSuccessListener {

                    saveNext(
                        index + 1
                    )
                }
                .addOnFailureListener {

                    toast(
                        "Default categories save नहीं हुईं: ${
                            it.message
                                ?: "Firestore error"
                        }"
                    )
                }
        }

        saveNext(0)
    }

    // =========================================================
    // CATEGORY LIST
    // =========================================================

    private fun showHomeCategoryList(
        docs:
            List<com.google.firebase.firestore.DocumentSnapshot>
    ) {

        val sorted =
            docs.sortedBy {

                it.getLong(
                    "position"
                ) ?: 999L
            }

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(5),
                    dp(3),
                    dp(5),
                    dp(5)
                )
            }

        val scroll =
            ScrollView(this).apply {

                isFillViewport = true
            }

        val list =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL
            }

        scroll.addView(list)

        sorted.forEachIndexed {
                index,
                doc ->

            addCategoryRow(
                list,
                sorted,
                index,
                doc
            )
        }

        box.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                dp(430)
            )
        )

        val add =
            Button(this).apply {

                text =
                    "➕ ADD NEW CATEGORY"

                textSize = 11f

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        0,
                        120,
                        215
                    )
                )

                setOnClickListener {

                    showCategoryEditDialog(
                        null
                    )
                }
            }

        box.addView(
            add,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            ).apply {

                topMargin = dp(5)
            }
        )

        AlertDialog.Builder(this)
            .setTitle(
                "🏠 Manage Home Categories"
            )
            .setView(box)
            .setPositiveButton(
                "CLOSE",
                null
            )
            .show()
    }

    // =========================================================
    // CATEGORY ROW
    // =========================================================

    private fun addCategoryRow(
        parent: LinearLayout,
        sorted:
            List<com.google.firebase.firestore.DocumentSnapshot>,
        index: Int,
        doc:
            com.google.firebase.firestore.DocumentSnapshot
    ) {

        val id =
            doc.id

        val name =
            doc.getString(
                "name"
            ) ?: "Category"

        val icon =
            doc.getString(
                "icon"
            ) ?: "📌"

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(
                        248,
                        250,
                        253
                    )
                )

                setPadding(
                    dp(6),
                    dp(5),
                    dp(6),
                    dp(5)
                )
            }

        val titleRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        titleRow.addView(
            TextView(this).apply {

                text =
                    "$icon  $name"

                textSize = 13f

                typeface =
                    Typeface.DEFAULT_BOLD

                maxLines = 1

                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            },
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            )
        )

        titleRow.addView(
            Button(this).apply {

                text = "✏️"

                textSize = 10f

                minHeight = 0

                setPadding(
                    dp(1),
                    0,
                    dp(1),
                    0
                )

                setOnClickListener {

                    showCategoryEditDialog(
                        doc
                    )
                }
            },
            LinearLayout.LayoutParams(
                dp(42),
                dp(34)
            ).apply {
                leftMargin = dp(2)
            }
        )

        titleRow.addView(
            Button(this).apply {

                text = "🗑"

                textSize = 10f

                minHeight = 0

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        205,
                        45,
                        45
                    )
                )

                setPadding(
                    dp(1),
                    0,
                    dp(1),
                    0
                )

                setOnClickListener {

                    deleteHomeCategory(
                        id,
                        name
                    )
                }
            },
            LinearLayout.LayoutParams(
                dp(42),
                dp(34)
            ).apply {
                leftMargin = dp(2)
            }
        )

        row.addView(titleRow)

        val moveRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        moveRow.addView(
            Button(this).apply {

                text = "⬆ UP"

                textSize = 9f

                minHeight = 0

                isEnabled =
                    index > 0

                setOnClickListener {

                    swapHomeCategoryPosition(
                        sorted[index],
                        sorted[index - 1]
                    )
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(30),
                1f
            ).apply {
                rightMargin = dp(2)
            }
        )

        moveRow.addView(
            Button(this).apply {

                text = "⬇ DOWN"

                textSize = 9f

                minHeight = 0

                isEnabled =
                    index <
                        sorted.lastIndex

                setOnClickListener {

                    swapHomeCategoryPosition(
                        sorted[index],
                        sorted[index + 1]
                    )
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(30),
                1f
            ).apply {
                leftMargin = dp(2)
            }
        )

        row.addView(moveRow)

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin = dp(4)
            }
        )

        parent.addView(
            View(this).apply {

                setBackgroundColor(
                    Color.rgb(
                        220,
                        228,
                        237
                    )
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(1)
            )
        )
    }

    // =========================================================
    // ADD / EDIT CATEGORY
    // =========================================================

    private fun showCategoryEditDialog(
        existing:
            com.google.firebase.firestore.DocumentSnapshot?
    ) {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )
            }

        val id =
            EditText(this).apply {

                hint =
                    "Category ID — जैसे study_material"

                setText(
                    existing?.id ?: ""
                )

                isEnabled =
                    existing == null
            }

        val name =
            EditText(this).apply {

                hint =
                    "Category Name"

                setText(
                    existing?.getString(
                        "name"
                    ) ?: ""
                )
            }

        val icon =
            EditText(this).apply {

                hint =
                    "Icon — जैसे 📖"

                setText(
                    existing?.getString(
                        "icon"
                    ) ?: "📌"
                )
            }

        val url =
            EditText(this).apply {

                hint =
                    "URL — external website या blank"

                setText(
                    existing?.getString(
                        "url"
                    ) ?: ""
                )
            }

        val pageId =
            EditText(this).apply {

                hint =
                    "Internal Page ID — जैसे study_material"

                setText(
                    existing?.getString(
                        "pageId"
                    ) ?: ""
                )
            }

        val position =
            EditText(this).apply {

                hint =
                    "Position"

                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER

                setText(
                    (
                        existing?.getLong(
                            "position"
                        ) ?: 99L
                        ).toString()
                )
            }

        box.addView(id)
        box.addView(name)
        box.addView(icon)
        box.addView(url)
        box.addView(pageId)
        box.addView(position)

        val title =
            if (existing == null) {
                "➕ Add Home Category"
            } else {
                "✏️ Edit Home Category"
            }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(box)
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "SAVE",
                null
            )
            .create()
            .also { dlg ->

                dlg.setOnShowListener {

                    dlg.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val categoryId =
                            id.text
                                .toString()
                                .trim()
                                .lowercase(
                                    Locale.US
                                )
                                .replace(
                                    " ",
                                    "_"
                                )

                        val categoryName =
                            name.text
                                .toString()
                                .trim()

                        val categoryIcon =
                            icon.text
                                .toString()
                                .trim()
                                .ifBlank {
                                    "📌"
                                }

                        val categoryUrl =
                            url.text
                                .toString()
                                .trim()

                        val categoryPageId =
                            pageId.text
                                .toString()
                                .trim()

                        val categoryPosition =
                            position.text
                                .toString()
                                .toLongOrNull()
                                ?: 99L

                        if (
                            categoryId.isBlank()
                        ) {

                            toast(
                                "Category ID डालें"
                            )

                            return@setOnClickListener
                        }

                        if (
                            categoryName.isBlank()
                        ) {

                            toast(
                                "Category Name डालें"
                            )

                            return@setOnClickListener
                        }

                        if (
                            categoryUrl.isBlank() &&
                            categoryPageId.isBlank()
                        ) {

                            toast(
                                "URL या Internal Page ID में से कम से कम एक दें"
                            )

                            return@setOnClickListener
                        }

                        dlg.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled =
                            false

                        val data =
                            mapOf(
                                "name" to categoryName,
                                "icon" to categoryIcon,
                                "url" to categoryUrl,
                                "pageId" to categoryPageId,
                                "position" to
                                    categoryPosition,
                                "updatedAt" to
                                    System.currentTimeMillis()
                            )

                        db.collection(
                            "home_categories"
                        )
                            .document(
                                categoryId
                            )
                            .set(
                                data,
                                com.google.firebase.firestore
                                    .SetOptions.merge()
                            )
                            .addOnSuccessListener {

                                dlg.dismiss()

                                toast(
                                    "Home Category सेव हो गई"
                                )
                            }
                            .addOnFailureListener { e ->

                                dlg.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                                ).isEnabled =
                                    true

                                toast(
                                    "Category SAVE failed:\n${
                                        e.message
                                            ?: "Permission denied"
                                    }"
                                )
                            }
                    }
                }

                dlg.show()
            }
    }

    // =========================================================
    // DELETE CATEGORY
    // =========================================================

    private fun deleteHomeCategory(
        id: String,
        name: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "🗑 Delete Category"
            )
            .setMessage(
                "\"$name\" को Home से delete करना है?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "DELETE"
            ) { _, _ ->

                db.collection(
                    "home_categories"
                )
                    .document(id)
                    .delete()
                    .addOnSuccessListener {

                        toast(
                            "Category delete हो गई"
                        )
                    }
                    .addOnFailureListener { e ->

                        toast(
                            "Delete failed: ${
                                e.message
                                    ?: "Permission denied"
                            }"
                        )
                    }
            }
            .show()
    }

    // =========================================================
    // MOVE CATEGORY UP / DOWN
    // =========================================================

    private fun swapHomeCategoryPosition(
        first:
            com.google.firebase.firestore.DocumentSnapshot,
        second:
            com.google.firebase.firestore.DocumentSnapshot
    ) {

        val firstPosition =
            first.getLong(
                "position"
            ) ?: 0L

        val secondPosition =
            second.getLong(
                "position"
            ) ?: 0L

        val batch =
            db.batch()

        batch.update(
            db.collection(
                "home_categories"
            ).document(
                first.id
            ),
            "position",
            secondPosition
        )

        batch.update(
            db.collection(
                "home_categories"
            ).document(
                second.id
            ),
            "position",
            firstPosition
        )

        batch.commit()
            .addOnSuccessListener {

                toast(
                    "Category position बदल गई"
                )

                showHomeCategoryManager()
            }
            .addOnFailureListener { e ->

                toast(
                    "Position update failed: ${
                        e.message
                            ?: "Firestore error"
                    }"
                )
            }
    }



    // =========================================================
    // CHANNEL MEDIA STORAGE
    // =========================================================

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

                var totalBytes =
                    0L

                var fileCount =
                    0

                var imageCount =
                    0

                var documentCount =
                    0

                snap.documents.forEach { doc ->

                    val size =
                        doc.getLong(
                            "size"
                        ) ?: 0L

                    totalBytes +=
                        size.coerceAtLeast(
                            0L
                        )

                    fileCount++

                    val folder =
                        doc.getString(
                            "folder"
                        ).orEmpty()

                    if (
                        folder == "images"
                    ) {

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
                    ${
                        String.format(
                            Locale.US,
                            "%.2f MB",
                            mb
                        )
                    }
                    
                    ${
                        String.format(
                            Locale.US,
                            "%.4f GB",
                            gb
                        )
                    }
                    
                    📊 Firestore free storage:
                    1 GiB shared Firestore limit
                    
                    📈 Approx. media usage:
                    ${
                        String.format(
                            Locale.US,
                            "%.2f%%",
                            percent
                        )
                    }
                    
                    ⚠️ यह Channel media का declared
                    file-size total है।
                    
                    Firebase Console में दिखने वाला
                    actual Firestore storage
                    indexes/document overhead सहित
                    इससे थोड़ा अलग हो सकता है।
                    
                    🗑️ Delete की गई post की unused
                    image/PDF और उसके chunks
                    repository की delete system से हटेंगे।
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

    // =========================================================
    // ANALYTICS
    // =========================================================

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

                        "Analytics अब channel_config/main के aggregate counters से पढ़े जा रहे हैं।\n\n" +

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

    // =========================================================
    // NEW POST
    // =========================================================

    private fun newPostDialog() {

        selectedImageUris.clear()

        imageUri = null

        fileUri = null

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    10,
                    10,
                    10,
                    10
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

                minLines =
                    4
            }

        val cat =
            EditText(this).apply {

                hint =
                    "Category: आदेश/निर्देश"
            }

        box.addView(
            title
        )

        box.addView(
            body
        )

        box.addView(
            cat
        )

        // -----------------------------------------------------
        // IMAGE BUTTON
        // -----------------------------------------------------

        box.addView(
            Button(this).apply {

                text =
                    "🖼️  फोटो / Gallery"

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
        // DOCUMENT
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

        // =====================================================
        // DIALOG
        // =====================================================

        AlertDialog.Builder(this)
            .setTitle(
                "नई Channel Post"
            )
            .setView(
                box
            )
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
                        ).isEnabled =
                            false

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
                                        "createdAt" to
                                            System.currentTimeMillis(),
                                        "commentsEnabled" to
                                            commentsEnabled,
                                        "likeCount" to 0L,
                                        "commentCount" to 0L,
                                        "viewCount" to 0L,
                                        "shareCount" to 0L
                                    )
                                ) { ok, msg ->

                                    runOnUiThread {

                                        d.getButton(
                                            AlertDialog.BUTTON_POSITIVE
                                        ).isEnabled =
                                            true

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
                                    ).isEnabled =
                                        true

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

    // =========================================================
    // IMAGE PREVIEW
    // =========================================================

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

                        setImageURI(
                            uri
                        )

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

                        rightMargin =
                            dp(5)
                    }
                )
            }
    }

    // =========================================================
    // UPLOAD BOTH
    // =========================================================

    private fun uploadBoth(
        title: String,
        body: String,
        cat: String,
        comments: Boolean,
        done: (
            Boolean,
            String?
        ) -> Unit
    ) {

        val images =
            selectedImageUris.toList()

        val file =
            fileUri

        fun createOne(
            image: String,
            fileUrl: String,
            imageMime: String,
            fileMime: String,
            index: Int,
            total: Int,
            finish: (
                Boolean,
                String?
            ) -> Unit
        ) {

            val postTitle =
                if (
                    total > 1
                ) {
                    "$title (${index + 1}/$total)"
                } else {
                    title
                }

            ChannelRepository
                .createPost(
                    mapOf(
                        "title" to postTitle,
                        "body" to body,
                        "published" to true,
                        "postType" to
                            if (
                                body.isNotBlank() &&
                                image.isBlank() &&
                                fileUrl.isBlank()
                            ) {
                                "text"
                            } else {
                                "media"
                            },
                        "category" to cat,
                        "imageUrl" to image,
                        "fileUrl" to fileUrl,
                        "fileName" to
                            (
                                file?.let {
                                    displayName(it)
                                }
                                    ?: ""
                                ),
                        "imageMime" to imageMime,
                        "fileMime" to fileMime,
                        "createdAt" to
                            System.currentTimeMillis(),
                        "commentsEnabled" to comments,
                        "likeCount" to 0L,
                        "commentCount" to 0L,
                        "viewCount" to 0L,
                        "shareCount" to 0L
                    )
                ) { ok, msg ->

                    finish(
                        ok,
                        msg
                    )
                }
        }

        fun publishImages(
            index: Int
        ) {

            if (
                index >= images.size
            ) {

                // Document-only post
                if (
                    file != null &&
                    images.isEmpty()
                ) {

                    ChannelRepository
                        .upload(
                            file,
                            "documents"
                        ) { fu, fe ->

                            if (
                                fu == null
                            ) {

                                done(
                                    false,
                                    fe
                                )

                            } else {

                                createOne(
                                    "",
                                    fu,
                                    "",
                                    contentResolver
                                        .getType(file)
                                        ?: "application/pdf",
                                    0,
                                    1,
                                    done
                                )
                            }
                        }

                } else {

                    done(
                        true,
                        null
                    )
                }

                return
            }

            val uri =
                images[index]

            ChannelRepository
                .upload(
                    uri,
                    "images"
                ) { url, err ->

                    if (
                        url == null
                    ) {

                        done(
                            false,
                            err
                        )

                        return@upload
                    }

                    val imageMime =
                        contentResolver
                            .getType(uri)
                            ?: "image/jpeg"

                    if (
                        index == images.lastIndex &&
                        file != null
                    ) {

                        ChannelRepository
                            .upload(
                                file,
                                "documents"
                            ) { fu, fe ->

                                if (
                                    fu == null
                                ) {

                                    done(
                                        false,
                                        fe
                                    )

                                    return@upload
                                }

                                createOne(
                                    url,
                                    fu,
                                    imageMime,
                                    contentResolver
                                        .getType(file)
                                        ?: "application/pdf",
                                    index,
                                    images.size
                                ) { ok, msg ->

                                    if (!ok) {

                                        done(
                                            false,
                                            msg
                                        )

                                    } else {

                                        done(
                                            true,
                                            null
                                        )
                                    }
                                }
                            }

                    } else {

                        createOne(
                            url,
                            "",
                            imageMime,
                            "",
                            index,
                            images.size
                        ) { ok, msg ->

                            if (!ok) {

                                done(
                                    false,
                                    msg
                                )

                            } else {

                                publishImages(
                                    index + 1
                                )
                            }
                        }
                    }
                }
        }

        publishImages(0)
    }

    // =========================================================
    // HOME PAGE EDIT
    // =========================================================

    private fun editHomePage(
        pageId: String,
        fallbackTitle: String
    ) {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )
            }

        pageImageUri =
            null

        val title =
            EditText(this).apply {

                hint =
                    "Page title"
            }

        val body =
            EditText(this).apply {

                hint =
                    "Page content / text"

                minLines =
                    7

                gravity =
                    Gravity.TOP
            }

        val imageUrl =
            EditText(this).apply {

                hint =
                    "Photo URL या firestore-media://... (optional)"
            }

        pageImageField =
            imageUrl

        box.addView(
            title
        )

        box.addView(
            body
        )

        box.addView(
            imageUrl
        )

        box.addView(
            Button(this).apply {

                text =
                    "🖼️ Page में फोटो जोड़ें / बदलें"

                setOnClickListener {

                    pickPageImage.launch(
                        "image/*"
                    )
                }
            }
        )

        db.collection(
            "home_pages"
        )
            .document(
                pageId
            )
            .get()
            .addOnSuccessListener { d ->

                title.setText(
                    d.getString(
                        "title"
                    )
                        ?: fallbackTitle
                )

                body.setText(
                    d.getString(
                        "body"
                    )
                        ?: ""
                )

                imageUrl.setText(
                    d.getString(
                        "imageUrl"
                    )
                        ?: ""
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                "✏️ $fallbackTitle"
            )
            .setView(
                box
            )
            .setPositiveButton(
                "SAVE",
                null
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .create()
            .also { dlg ->

                dlg.setOnShowListener {

                    dlg.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val saveUrl:
                            (String) -> Unit =
                            { url ->

                                db.collection(
                                    "home_pages"
                                )
                                    .document(
                                        pageId
                                    )
                                    .set(
                                        mapOf(
                                            "title" to
                                                title.text
                                                    .toString()
                                                    .trim()
                                                    .ifBlank {
                                                        fallbackTitle
                                                    },
                                            "body" to
                                                body.text
                                                    .toString(),
                                            "imageUrl" to
                                                url,
                                            "updatedAt" to
                                                System.currentTimeMillis()
                                        ),
                                        com.google.firebase.firestore
                                            .SetOptions.merge()
                                    )
                                    .addOnSuccessListener {

                                        dlg.dismiss()

                                        toast(
                                            "Page सामग्री सेव हो गई"
                                        )
                                    }
                                    .addOnFailureListener {

                                        toast(
                                            "SAVE FAILED: ${
                                                it.message
                                                    ?: "Permission denied"
                                            }\nUID: ${
                                                auth.currentUser
                                                    ?.uid
                                                    ?: "none"
                                            }"
                                        )
                                    }
                            }

                        val chosen =
                            pageImageUri

                        if (
                            chosen != null
                        ) {

                            dlg.getButton(
                                AlertDialog.BUTTON_POSITIVE
                            ).isEnabled =
                                false

                            ChannelRepository
                                .upload(
                                    chosen,
                                    "images"
                                ) { url, err ->

                                    runOnUiThread {

                                        if (
                                            url != null
                                        ) {

                                            saveUrl(
                                                url
                                            )

                                        } else {

                                            dlg.getButton(
                                                AlertDialog.BUTTON_POSITIVE
                                            ).isEnabled =
                                                true

                                            toast(
                                                "Photo upload असफल: $err"
                                            )
                                        }
                                    }
                                }

                        } else {

                            saveUrl(
                                imageUrl.text
                                    .toString()
                                    .trim()
                            )
                        }
                    }
                }

                dlg.show()
            }
    }

    // =========================================================
    // LOAD POSTS
    // =========================================================

    private fun loadAdminPosts() {

        if (
            !::postsContainer.isInitialized
        ) {
            return
        }

        selectedPostIds.clear()

        postsContainer.removeAllViews()

        ChannelRepository
            .posts(
                { ps ->

                    ps
                        .takeLast(100)
                        .forEach { p ->

                            addAdminPost(
                                p
                            )
                        }

                    postScroll.post {

                        postScroll.fullScroll(
                            View.FOCUS_DOWN
                        )
                    }
                },
                { e ->

                    toast(
                        "Posts लोड नहीं हुए: ${
                            e.message
                                ?: "Firestore error"
                        }"
                    )
                }
            )
    }

    // =========================================================
    // ADD ADMIN POST
    // =========================================================

    private fun addAdminPost(
    p: ChannelPost
) {

    val row =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            background =
                GradientFactory.roundedWhite()

            setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6)
            )

            elevation = 2f
        }

    // =====================================================
    // TOP ROW
    // =====================================================

    val top =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL
        }

    val check =
        CheckBox(this).apply {

            isChecked =
                false

            setPadding(0, 0, 0, 0)

            setOnCheckedChangeListener {
                    _,
                    checked ->

                if (checked) {

                    selectedPostIds.add(
                        p.id
                    )

                } else {

                    selectedPostIds.remove(
                        p.id
                    )
                }
            }
        }

    top.addView(
        check,
        LinearLayout.LayoutParams(
            dp(42),
            dp(40)
        )
    )

    // -----------------------------------------------------
    // TITLE
    // -----------------------------------------------------

    val titleBox =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL
        }

    titleBox.addView(
        TextView(this).apply {

            text =
                if (p.title.isBlank()) {
                    "बिना Title Post"
                } else {
                    p.title
                }

            textSize = 14f

            typeface =
                Typeface.DEFAULT_BOLD

            maxLines = 2

            ellipsize =
                android.text.TextUtils.TruncateAt.END
        }
    )

    titleBox.addView(
        TextView(this).apply {

            text =
                p.category.ifBlank {
                    "General"
                }

            textSize = 10f

            setTextColor(
                Color.DKGRAY
            )
        }
    )

    top.addView(
        titleBox,
        LinearLayout.LayoutParams(
            0,
            dp(40),
            1f
        )
    )

    // -----------------------------------------------------
    // OPEN PREVIEW BUTTON
    // -----------------------------------------------------

    top.addView(
        Button(this).apply {

            text = "OPEN"
            textSize = 8f

            minHeight = 0

            setPadding(
                dp(2),
                0,
                dp(2),
                0
            )

            setOnClickListener {

                showAdminPostPreview(
                    p
                )
            }
        },
        LinearLayout.LayoutParams(
            dp(62),
            dp(36)
        ).apply {
            leftMargin = dp(4)
        }
    )

    row.addView(top)

    // =====================================================
    // DATE
    // =====================================================

    row.addView(
        TextView(this).apply {

            text =
                if (p.createdAt > 0) {

                    java.text
                        .SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale(
                                "hi",
                                "IN"
                            )
                        )
                        .format(
                            java.util.Date(
                                p.createdAt
                            )
                        )

                } else {

                    ""
                }

            textSize = 9f

            setTextColor(
                Color.GRAY
            )

            setPadding(
                dp(42),
                0,
                0,
                dp(2)
            )
        }
    )

    // =====================================================
    // SHORT POST PREVIEW
    // =====================================================

    val previewText =
        when {

            p.body.isNotBlank() ->
                p.body
                    .replace(
                        "\n",
                        " "
                    )
                    .trim()
                    .take(150)

            p.imageUrl.isNotBlank() ->
                "🖼️ Photo attached"

            p.fileUrl.isNotBlank() ->
                "📄 Document attached"

            else ->
                "Text-only post"
        }

    row.addView(
        TextView(this).apply {

            text =
                previewText

            textSize = 11f

            maxLines = 2

            ellipsize =
                android.text.TextUtils.TruncateAt.END

            setPadding(
                dp(42),
                0,
                0,
                dp(3)
            )
        }
    )

    // =====================================================
    // POST STATS
    // =====================================================

    row.addView(
        TextView(this).apply {

            text =
                "👍 ${p.likeCount}   " +
                "💬 ${p.commentCount}   " +
                "👁 ${p.viewCount}   " +
                "↗ ${p.shareCount}"

            textSize = 9f

            setTextColor(
                Color.DKGRAY
            )

            setPadding(
                dp(42),
                0,
                0,
                dp(3)
            )
        }
    )

    // =====================================================
    // ACTION ROW
    // =====================================================

    val actions =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL
        }

    val sw =
        Switch(this).apply {

            text = "💬"
            textSize = 10f

            isChecked =
                p.commentsEnabled

            setPadding(0, 0, 0, 0)

            setOnCheckedChangeListener {
                    _,
                    enabled ->

                db.collection(
                    "channel_posts"
                )
                    .document(
                        p.id
                    )
                    .update(
                        "commentsEnabled",
                        enabled
                    )
                    .addOnFailureListener {
                        toast(
                            "Comments update नहीं हुआ"
                        )
                    }
            }
        }

    actions.addView(
        sw,
        LinearLayout.LayoutParams(
            0,
            dp(34),
            1f
        )
    )

    // -----------------------------------------------------
    // EDIT
    // -----------------------------------------------------

    actions.addView(
        Button(this).apply {

            text = "✏️ Edit"
            textSize = 9f

            minHeight = 0

            setPadding(
                dp(2),
                0,
                dp(2),
                0
            )

            setOnClickListener {

                editChannelPostDialog(
                    p
                )
            }
        },
        LinearLayout.LayoutParams(
            dp(76),
            dp(34)
        ).apply {
            leftMargin = dp(3)
        }
    )

    // -----------------------------------------------------
    // DELETE
    // -----------------------------------------------------

    actions.addView(
        Button(this).apply {

            text = "🗑 Delete"
            textSize = 9f

            minHeight = 0

            setTextColor(
                Color.WHITE
            )

            setBackgroundColor(
                Color.rgb(
                    205,
                    45,
                    45
                )
            )

            setPadding(
                dp(2),
                0,
                dp(2),
                0
            )

            setOnClickListener {

                confirmDeletePosts(
                    listOf(
                        p.id
                    )
                )
            }
        },
        LinearLayout.LayoutParams(
            dp(82),
            dp(34)
        ).apply {
            leftMargin = dp(3)
        }
    )

    row.addView(actions)

    // =====================================================
    // SEPARATOR
    // =====================================================

    row.addView(
        View(this).apply {

            setBackgroundColor(
                Color.rgb(
                    225,
                    232,
                    240
                )
            )
        },
        LinearLayout.LayoutParams(
            -1,
            dp(1)
        ).apply {
            topMargin = dp(5)
        }
    )

    postsContainer.addView(
        row,
        LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {

            bottomMargin =
                dp(6)
        }
    )
}
        
private fun showAdminPostPreview(
    p: ChannelPost
) {

    val box =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )
        }

    box.addView(
        TextView(this).apply {

            text =
                p.title.ifBlank {
                    "बिना Title Post"
                }

            textSize = 19f

            typeface =
                Typeface.DEFAULT_BOLD

            setPadding(
                0,
                0,
                0,
                dp(8)
            )
        }
    )

    box.addView(
        TextView(this).apply {

            text =
                "Category: " +
                    p.category.ifBlank {
                        "General"
                    }

            textSize = 11f

            setTextColor(
                Color.DKGRAY
            )
        }
    )

    box.addView(
        TextView(this).apply {

            text =
                if (p.body.isNotBlank()) {
                    p.body
                } else {
                    "कोई text नहीं"
                }

            textSize = 14f

            setPadding(
                0,
                dp(10),
                0,
                dp(10)
            )
        }
    )

    if (p.imageUrl.isNotBlank()) {

        box.addView(
            TextView(this).apply {

                text =
                    "🖼️ इस post में photo attached है"

                textSize = 12f

                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(5)
                )
            }
        )
    }

    if (p.fileUrl.isNotBlank()) {

        box.addView(
            TextView(this).apply {

                text =
                    "📄 इस post में document/PDF attached है"

                textSize = 12f

                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(5)
                )
            }
        )
    }

    box.addView(
        TextView(this).apply {

            text =
                "👍 ${p.likeCount}   " +
                "💬 ${p.commentCount}   " +
                "👁 ${p.viewCount}   " +
                "↗ ${p.shareCount}"

            textSize = 12f

            setTextColor(
                Color.DKGRAY
            )

            setPadding(
                0,
                dp(8),
                0,
                dp(4)
            )
        }
    )

    AlertDialog.Builder(this)
        .setTitle("📋 Post Preview")
        .setView(box)
        .setPositiveButton(
            "CLOSE",
            null
        )
        .show()
}
    // =========================================================
    // SELECT ALL
    // =========================================================

    private fun selectAllAdminPosts() {

        for (
            i in 0 until postsContainer.childCount
        ) {

            val row =
                postsContainer.getChildAt(i)
                    as? LinearLayout
                    ?: continue

            val top =
                row.getChildAt(0)
                    as? LinearLayout
                    ?: continue

            val cb =
                top.getChildAt(0)
                    as? CheckBox
                    ?: continue

            cb.isChecked =
                true
        }

        toast(
            "सभी posts select हो गईं"
        )
    }

    // =========================================================
    // DELETE SELECTED
    // =========================================================

    private fun deleteSelectedAdminPosts() {

        if (
            selectedPostIds.isEmpty()
        ) {

            toast(
                "पहले posts के checkbox चुनें"
            )

            return
        }

        confirmDeletePosts(
            selectedPostIds.toList()
        )
    }

    // =========================================================
    // DELETE ALL
    // =========================================================

    private fun deleteAllAdminPosts() {

        ChannelRepository
            .posts(
                { ps ->

                    val ids =
                        ps.map {
                            it.id
                        }

                    if (
                        ids.isEmpty()
                    ) {

                        toast(
                            "Delete करने के लिए कोई post नहीं है"
                        )

                        return@posts
                    }

                    confirmDeletePosts(
                        ids
                    )
                },
                { e ->

                    toast(
                        "Posts नहीं मिलीं: ${
                            e.message
                                ?: "Firestore error"
                        }"
                    )
                }
            )
    }

    // =========================================================
    // DELETE CONFIRM
    // =========================================================

    private fun confirmDeletePosts(
        ids: List<String>
    ) {

        val clean =
            ids
                .filter {
                    it.isNotBlank()
                }
                .distinct()

        if (
            clean.isEmpty()
        ) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "🗑 Delete Posts"
            )
            .setMessage(
                "${clean.size} post delete की जाएंगी। यह action वापस नहीं होगा।"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "DELETE",
                null
            )
            .create()
            .also { dlg ->

                dlg.setOnShowListener {

                    dlg.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        dlg.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled =
                            false

                        deletePostIds(
                            clean,
                            0,
                            dlg
                        )
                    }
                }

                dlg.show()
            }
    }

    // =========================================================
    // DELETE POST IDS
    // =========================================================

    private fun deletePostIds(
        ids: List<String>,
        index: Int,
        dlg: AlertDialog
    ) {

        if (
            index >= ids.size
        ) {

            dlg.dismiss()

            selectedPostIds.clear()

            toast(
                "${ids.size} posts delete हो गईं"
            )

            loadAdminPosts()

            return
        }

        ChannelRepository
            .deletePost(
                ids[index]
            ) { ok ->

                runOnUiThread {

                    if (!ok) {

                        dlg.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled =
                            true

                        toast(
                            "Delete असफल — Firestore Permission/Rules जाँचें"
                        )

                        return@runOnUiThread
                    }

                    deletePostIds(
                        ids,
                        index + 1,
                        dlg
                    )
                }
            }
    }

    // =========================================================
    // EDIT CHANNEL POST
    // =========================================================

    private fun editChannelPostDialog(
        p: ChannelPost
    ) {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )
            }

        val title =
            EditText(this).apply {

                hint =
                    "Post title"

                setText(
                    p.title
                )
            }

        val body =
            EditText(this).apply {

                hint =
                    "Post text / details"

                setText(
                    p.body
                )

                minLines =
                    6

                gravity =
                    Gravity.TOP
            }

        val cat =
            EditText(this).apply {

                hint =
                    "Category"

                setText(
                    p.category
                )
            }

        val comments =
            Switch(this).apply {

                text =
                    "Comments ON"

                isChecked =
                    p.commentsEnabled
            }

        box.addView(
            title
        )

        box.addView(
            body
        )

        box.addView(
            cat
        )

        box.addView(
            comments
        )

        if (
            p.imageUrl.isNotBlank()
        ) {

            box.addView(
                TextView(this).apply {

                    text =
                        "🖼️ Existing photo सुरक्षित रहेगी"

                    setPadding(
                        0,
                        dp(6),
                        0,
                        dp(2)
                    )
                }
            )
        }

        if (
            p.fileUrl.isNotBlank()
        ) {

            box.addView(
                TextView(this).apply {

                    text =
                        "📄 Existing document सुरक्षित रहेगा"

                    setPadding(
                        0,
                        dp(2),
                        0,
                        dp(6)
                    )
                }
            )
        }

        AlertDialog.Builder(this)
            .setTitle(
                "✏️ Edit Channel Post"
            )
            .setView(
                box
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "SAVE",
                null
            )
            .create()
            .also { dlg ->

                dlg.setOnShowListener {

                    dlg.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        dlg.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled =
                            false

                        val data =
                            mapOf(
                                "title" to
                                    title.text
                                        .toString()
                                        .trim()
                                        .ifBlank {
                                            p.title
                                        },

                                "body" to
                                    body.text
                                        .toString(),

                                "category" to
                                    cat.text
                                        .toString()
                                        .trim()
                                        .ifBlank {
                                            p.category
                                        },

                                "commentsEnabled" to
                                    comments.isChecked,

                                "updatedAt" to
                                    System.currentTimeMillis()
                            )

                        ChannelRepository
                            .updatePost(
                                p.id,
                                data
                            ) { ok, msg ->

                                runOnUiThread {

                                    dlg.getButton(
                                        AlertDialog.BUTTON_POSITIVE
                                    ).isEnabled =
                                        true

                                    if (ok) {

                                        dlg.dismiss()

                                        toast(
                                            "Post update हो गई"
                                        )

                                        loadAdminPosts()

                                    } else {

                                        toast(
                                            "Edit Save असफल: ${
                                                msg
                                                    ?: "Permission denied"
                                            }"
                                        )
                                    }
                                }
                            }
                    }
                }

                dlg.show()
            }
    }

    // =========================================================
    // DISPLAY NAME
    // =========================================================

    private fun displayName(
        uri: Uri
    ): String {

        var name =
            "document.pdf"

        contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (
                    cursor.moveToFirst()
                ) {

                    name =
                        cursor.getString(
                            0
                        )
                }
            }

        return name
    }
    
 
                // ============================================================
// HOME SUB-CATEGORY MANAGER
// ============================================================
private fun showHomeSubCategoryManager() {

    val categories = listOf(
        "Teacher" to "teacher",
        "Student" to "student",
        "School" to "school",
        "Vacancy" to "vacancy",
        "Result" to "result",
        "Admit Card" to "admit_card"
    )

    val selectedCategory = arrayOf(0)

    val root = LinearLayout(this@AdminActivity).apply {

        orientation = LinearLayout.VERTICAL

        setPadding(
            dp(12),
            dp(8),
            dp(12),
            dp(8)
        )
    }

    val spinner = Spinner(this@AdminActivity)

    val categoryNames =
        categories.map { it.first }.toTypedArray()

    spinner.adapter =
        ArrayAdapter(
            this@AdminActivity,
            android.R.layout.simple_spinner_dropdown_item,
            categoryNames
        )

    root.addView(
        TextView(this@AdminActivity).apply {

            text = "मुख्य Category चुनें"

            textSize = 13f

            typeface = Typeface.DEFAULT_BOLD

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
                dp(4)
            )
        }
    )

    root.addView(
        spinner,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val listContainer =
        LinearLayout(this@AdminActivity).apply {

            orientation =
                LinearLayout.VERTICAL
        }

    val listScroll =
        ScrollView(this@AdminActivity).apply {

            addView(
                listContainer,
                LinearLayout.LayoutParams(
                    -1,
                    -2
                )
            )
        }

    root.addView(
        listScroll,
        LinearLayout.LayoutParams(
            -1,
            0,
            1f
        )
    )

    val addButton =
        Button(this@AdminActivity).apply {

            text = "➕ ADD SUB-CATEGORY"

            textSize = 12f

            setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            setTextColor(Color.WHITE)

            setBackgroundColor(
                Color.rgb(
                    7,
                    89,
                    133
                )
            )

            minHeight = 0

            setPadding(
                dp(4),
                0,
                dp(4),
                0
            )
        }

    root.addView(
        addButton,
        LinearLayout.LayoutParams(
            -1,
            dp(44)
        ).apply {

            topMargin = dp(6)
        }
    )

    val dialog =
        AlertDialog.Builder(this@AdminActivity)
            .setTitle(
                "🔽 MANAGE SUB-CATEGORIES"
            )
            .setView(root)
            .setNegativeButton(
                "Close",
                null
            )
            .create()

    // ========================================================
    // LOAD LIST REFERENCE
    // ========================================================
    lateinit var loadList: () -> Unit

    // ========================================================
    // ADD / EDIT
    // ========================================================
    fun showEditor(
        documentId: String?,
        existing:
            com.google.firebase.firestore.DocumentSnapshot?,
        parentId: String
    ) {

        val form =
            LinearLayout(this@AdminActivity).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(5),
                    dp(18),
                    dp(5)
                )
            }

        val nameInput =
            EditText(this@AdminActivity).apply {

                hint = "Sub-Category Name"

                setSingleLine(true)

                setText(
                    existing?.getString("name")
                        ?: ""
                )
            }

        val iconInput =
            EditText(this@AdminActivity).apply {

                hint = "Icon (जैसे 📚)"

                setSingleLine(true)

                setText(
                    existing?.getString("icon")
                        ?: ""
                )
            }

        val urlInput =
            EditText(this@AdminActivity).apply {

                hint = "Website URL"

                setSingleLine(true)

                setText(
                    existing?.getString("url")
                        ?: ""
                )
            }

        val pageTypes =
            arrayOf(
                "Website",
                "Text",
                "Image",
                "PDF"
            )

        val pageTypeSpinner =
            Spinner(this@AdminActivity)

        pageTypeSpinner.adapter =
            ArrayAdapter(
                this@AdminActivity,
                android.R.layout.simple_spinner_dropdown_item,
                pageTypes
            )

        val oldPageType =
            existing?.getString("pageType")
                ?: "Website"

        val oldPageIndex =
            pageTypes.indexOf(oldPageType)

        if (oldPageIndex >= 0) {

            pageTypeSpinner.setSelection(
                oldPageIndex
            )
        }

        val positionInput =
            EditText(this@AdminActivity).apply {

                hint = "Position"

                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER

                setSingleLine(true)

                val oldPosition =
                    (
                        existing?.get("position")
                            as? Number
                        )?.toLong()
                            ?: 1L

                setText(
                    oldPosition.toString()
                )
            }

        val enabledSwitch =
            Switch(this@AdminActivity).apply {

                text = "Enabled"

                isChecked =
                    existing?.getBoolean("enabled")
                        ?: true
            }

        form.addView(
            nameInput,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        form.addView(
            iconInput,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        form.addView(
            urlInput,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        form.addView(
            TextView(this@AdminActivity).apply {

                text = "Page Type"

                textSize = 12f

                setTextColor(
                    Color.DKGRAY
                )

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }
        )

        form.addView(
            pageTypeSpinner,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        form.addView(
            positionInput,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        form.addView(
            enabledSwitch,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        val editorDialog =
            AlertDialog.Builder(
                this@AdminActivity
            )
                .setTitle(
                    if (documentId == null)
                        "➕ Add Sub-Category"
                    else
                        "✏️ Edit Sub-Category"
                )
                .setView(form)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Save",
                    null
                )
                .create()

        editorDialog.setOnShowListener {

            editorDialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name =
                    nameInput.text
                        .toString()
                        .trim()

                val icon =
                    iconInput.text
                        .toString()
                        .trim()

                val url =
                    urlInput.text
                        .toString()
                        .trim()

                val pageType =
                    pageTypes[
                        pageTypeSpinner.selectedItemPosition
                    ]

                val position =
                    positionInput.text
                        .toString()
                        .toLongOrNull()
                        ?: 1L

                val enabled =
                    enabledSwitch.isChecked

                if (name.isEmpty()) {

                    nameInput.error =
                        "Name जरूरी है"

                    return@setOnClickListener
                }

                if (
                    pageType == "Website" &&
                    url.isEmpty()
                ) {

                    urlInput.error =
                        "Website URL जरूरी है"

                    return@setOnClickListener
                }

                // IMPORTANT:
                // Explicit Map<String, Any>
                // Firestore type mismatch fix
                val data:
                    Map<String, Any> =
                    hashMapOf<String, Any>(

                        "name" to name,

                        "icon" to icon,

                        "url" to url,

                        "pageType" to pageType,

                        "parentId" to parentId,

                        "position" to position,

                        "enabled" to enabled
                    )

                val task =
                    if (documentId == null) {

                        val newId =
                            parentId +
                            "_" +
                            System.currentTimeMillis()

                        db.collection(
                            "home_subcategories"
                        )
                            .document(newId)
                            .set(data)

                    } else {

                        db.collection(
                            "home_subcategories"
                        )
                            .document(documentId)
                            .update(data)
                    }

                task.addOnSuccessListener {

                    Toast.makeText(
                        this@AdminActivity,
                        "Sub-Category Save हो गई",
                        Toast.LENGTH_SHORT
                    ).show()

                    editorDialog.dismiss()

                    loadList()
                }

                task.addOnFailureListener { e ->

                    Toast.makeText(
                        this@AdminActivity,
                        "Save failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        editorDialog.show()
    }

    // ========================================================
    // LOAD LIST
    // ========================================================
    loadList = {

        listContainer.removeAllViews()

        val parentId =
            categories[
                selectedCategory[0]
            ].second

        listContainer.addView(
            TextView(this@AdminActivity).apply {

                text = "Loading..."

                textSize = 13f

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(10)
                )
            }
        )

        db.collection(
            "home_subcategories"
        )
            .whereEqualTo(
                "parentId",
                parentId
            )
            .get()
            .addOnSuccessListener { result ->

                listContainer.removeAllViews()

                val docs =
                    result.documents.sortedBy { snapshot ->

                        (
                            snapshot.get("position")
                                as? Number
                            )?.toLong()
                                ?: 999999L
                    }

                if (docs.isEmpty()) {

                    listContainer.addView(
                        TextView(
                            this@AdminActivity
                        ).apply {

                            text =
                                "इस Category में अभी कोई Sub-Category नहीं है।"

                            textSize = 13f

                            setTextColor(
                                Color.DKGRAY
                            )

                            setPadding(
                                0,
                                dp(12),
                                0,
                                dp(12)
                            )
                        }
                    )

                    return@addOnSuccessListener
                }

                docs.forEachIndexed {
                    index,
                    doc ->

                    val card =
                        LinearLayout(
                            this@AdminActivity
                        ).apply {

                            orientation =
                                LinearLayout.VERTICAL

                            setPadding(
                                dp(8),
                                dp(7),
                                dp(8),
                                dp(7)
                            )

                            setBackgroundColor(
                                Color.rgb(
                                    245,
                                    248,
                                    250
                                )
                            )
                        }

                    val name =
                        doc.getString("name")
                            ?: ""

                    val icon =
                        doc.getString("icon")
                            ?: ""

                    val pageType =
                        doc.getString("pageType")
                            ?: "Website"

                    val url =
                        doc.getString("url")
                            ?: ""

                    val position =
                        (
                            doc.get("position")
                                as? Number
                            )?.toLong()
                                ?: 0L

                    val enabled =
                        doc.getBoolean("enabled")
                            ?: true

                    card.addView(
                        TextView(
                            this@AdminActivity
                        ).apply {

                            text =
                                "$icon  $name"

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
                        }
                    )

                    card.addView(
                        TextView(
                            this@AdminActivity
                        ).apply {

                            text =
                                "Type: $pageType   |   Position: $position   |   Enabled: $enabled"

                            textSize = 11f

                            setTextColor(
                                Color.DKGRAY
                            )
                        }
                    )

                    if (url.isNotEmpty()) {

                        card.addView(
                            TextView(
                                this@AdminActivity
                            ).apply {

                                text = url

                                textSize = 10f

                                setTextColor(
                                    Color.GRAY
                                )

                                maxLines = 2
                            }
                        )
                    }

                    val buttons =
                        LinearLayout(
                            this@AdminActivity
                        ).apply {

                            orientation =
                                LinearLayout.HORIZONTAL

                            gravity =
                                Gravity.CENTER_VERTICAL
                        }

                    val editButton =
                        Button(
                            this@AdminActivity
                        ).apply {

                            text = "✏️ Edit"

                            textSize = 10f

                            minHeight = 0

                            setPadding(
                                dp(3),
                                0,
                                dp(3),
                                0
                            )

                            setOnClickListener {

                                showEditor(
                                    doc.id,
                                    doc,
                                    parentId
                                )
                            }
                        }

                    val deleteButton =
                        Button(
                            this@AdminActivity
                        ).apply {

                            text = "🗑 Delete"

                            textSize = 10f

                            minHeight = 0

                            setPadding(
                                dp(3),
                                0,
                                dp(3),
                                0
                            )

                            setOnClickListener {

                                AlertDialog.Builder(
                                    this@AdminActivity
                                )
                                    .setTitle(
                                        "Delete Sub-Category?"
                                    )
                                    .setMessage(
                                        name
                                    )
                                    .setNegativeButton(
                                        "Cancel",
                                        null
                                    )
                                    .setPositiveButton(
                                        "Delete"
                                    ) { _, _ ->

                                        db.collection(
                                            "home_subcategories"
                                        )
                                            .document(
                                                doc.id
                                            )
                                            .delete()
                                            .addOnSuccessListener {

                                                Toast.makeText(
                                                    this@AdminActivity,
                                                    "Deleted",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                loadList()
                                            }
                                            .addOnFailureListener { e ->

                                                Toast.makeText(
                                                    this@AdminActivity,
                                                    "Delete failed: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                    .show()
                            }
                        }

                    val upButton =
                        Button(
                            this@AdminActivity
                        ).apply {

                            text = "⬆"

                            textSize = 11f

                            minHeight = 0

                            setPadding(
                                dp(3),
                                0,
                                dp(3),
                                0
                            )

                            isEnabled =
                                index > 0

                            setOnClickListener {

                                val previous =
                                    docs[index - 1]

                                val currentPosition =
                                    (
                                        doc.get(
                                            "position"
                                        )
                                            as? Number
                                        )?.toLong()
                                            ?: 0L

                                val previousPosition =
                                    (
                                        previous.get(
                                            "position"
                                        )
                                            as? Number
                                        )?.toLong()
                                            ?: 0L

                                val batch =
                                    db.batch()

                                batch.update(
                                    db.collection(
                                        "home_subcategories"
                                    ).document(
                                        doc.id
                                    ),
                                    "position",
                                    previousPosition
                                )

                                batch.update(
                                    db.collection(
                                        "home_subcategories"
                                    ).document(
                                        previous.id
                                    ),
                                    "position",
                                    currentPosition
                                )

                                batch.commit()
                                    .addOnSuccessListener {
                                        loadList()
                                    }
                                    .addOnFailureListener { e ->

                                        Toast.makeText(
                                            this@AdminActivity,
                                            "Move failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }

                    val downButton =
                        Button(
                            this@AdminActivity
                        ).apply {

                            text = "⬇"

                            textSize = 11f

                            minHeight = 0

                            setPadding(
                                dp(3),
                                0,
                                dp(3),
                                0
                            )

                            isEnabled =
                                index < docs.lastIndex

                            setOnClickListener {

                                val next =
                                    docs[index + 1]

                                val currentPosition =
                                    (
                                        doc.get(
                                            "position"
                                        )
                                            as? Number
                                        )?.toLong()
                                            ?: 0L

                                val nextPosition =
                                    (
                                        next.get(
                                            "position"
                                        )
                                            as? Number
                                        )?.toLong()
                                            ?: 0L

                                val batch =
                                    db.batch()

                                batch.update(
                                    db.collection(
                                        "home_subcategories"
                                    ).document(
                                        doc.id
                                    ),
                                    "position",
                                    nextPosition
                                )

                                batch.update(
                                    db.collection(
                                        "home_subcategories"
                                    ).document(
                                        next.id
                                    ),
                                    "position",
                                    currentPosition
                                )

                                batch.commit()
                                    .addOnSuccessListener {
                                        loadList()
                                    }
                                    .addOnFailureListener { e ->

                                        Toast.makeText(
                                            this@AdminActivity,
                                            "Move failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }

                    val enabledButton =
                        Button(
                            this@AdminActivity
                        ).apply {

                            text =
                                if (enabled)
                                    "🔴 Disable"
                                else
                                    "🟢 Enable"

                            textSize = 10f

                            minHeight = 0

                            setPadding(
                                dp(3),
                                0,
                                dp(3),
                                0
                            )

                            setOnClickListener {

                                db.collection(
                                    "home_subcategories"
                                )
                                    .document(
                                        doc.id
                                    )
                                    .update(
                                        "enabled",
                                        !enabled
                                    )
                                    .addOnSuccessListener {
                                        loadList()
                                    }
                                    .addOnFailureListener { e ->

                                        Toast.makeText(
                                            this@AdminActivity,
                                            "Update failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }

                    // ====================================================
                    // BUTTONS
                    // ====================================================
                    buttons.addView(
                        editButton,
                        LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            1f
                        ).apply {
                            marginEnd = dp(2)
                        }
                    )

                    buttons.addView(
                        deleteButton,
                        LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            1f
                        ).apply {
                            marginEnd = dp(2)
                        }
                    )

                    buttons.addView(
                        upButton,
                        LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            0.65f
                        ).apply {
                            marginEnd = dp(2)
                        }
                    )

                    buttons.addView(
                        downButton,
                        LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            0.65f
                        ).apply {
                            marginEnd = dp(2)
                        }
                    )

                    buttons.addView(
                        enabledButton,
                        LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            1f
                        )
                    )

                    card.addView(
                        buttons,
                        LinearLayout.LayoutParams(
                            -1,
                            dp(44)
                        )
                    )

                    listContainer.addView(
                        card,
                        LinearLayout.LayoutParams(
                            -1,
                            -2
                        ).apply {

                            bottomMargin = dp(6)
                        }
                    )
                }
            }
            .addOnFailureListener { e ->

                listContainer.removeAllViews()

                listContainer.addView(
                    TextView(
                        this@AdminActivity
                    ).apply {

                        text =
                            "List load failed: ${e.message}"

                        textSize = 13f

                        setTextColor(
                            Color.RED
                        )

                        setPadding(
                            0,
                            dp(12),
                            0,
                            dp(12)
                        )
                    }
                )
            }
    }

    // ========================================================
    // ADD BUTTON
    // ========================================================
    addButton.setOnClickListener {

        val parentId =
            categories[
                selectedCategory[0]
            ].second

        showEditor(
            null,
            null,
            parentId
        )
    }

    // ========================================================
    // CATEGORY CHANGE
    // ========================================================
    spinner.onItemSelectedListener =
        object :
            android.widget.AdapterView
                .OnItemSelectedListener {

            override fun onItemSelected(
                parent:
                    android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                selectedCategory[0] =
                    position

                loadList()
            }

            override fun onNothingSelected(
                parent:
                    android.widget.AdapterView<*>?
            ) {
            }
        }

    // ========================================================
    // SHOW
    // ========================================================
    dialog.show()

    loadList()
}


    // =========================================================
    // TOAST
    // =========================================================

    private fun toast(
        s: String
    ) {

        if (
            s.isNotBlank()
        ) {

            Toast.makeText(
                this,
                s,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
