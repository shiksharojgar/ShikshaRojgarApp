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
import com.google.android.gms.tasks.Tasks
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

private var adminSessionId: String? = null
    private var adminSessionListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var phoneVerificationId: String? = null
    
// =========================================================
// NOTICE PHOTO PICKER
// =========================================================

private var noticeImageUri: Uri? = null

private val pickNoticeImage =
    registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        noticeImageUri = uri
    }
    
    // =========================================================
    // COMMON POST PHOTO / PDF PICKER
    // =========================================================

    private var commonPostImageUri: Uri? = null
    private var commonPostFileUri: Uri? = null
private var commonPostImagePreview: ImageView? = null
    private val pickCommonPostImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            commonPostImageUri = uri
commonPostImagePreview?.apply {
    if (uri != null) {
        setImageURI(uri)
        visibility = View.VISIBLE
    } else {
        setImageDrawable(null)
        visibility = View.GONE
    }
}
if (uri != null) {
    toast("पोस्ट की फोटो चुन ली गई")
}
        }

    private val pickCommonPostFile =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            commonPostFileUri = uri
            if (uri != null) {
                toast("पोस्ट की PDF/File चुन ली गई")
            }
        }
        
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
// ADMIN UI DESIGN HELPERS
// =========================================================

private fun adminSectionTitle(
    icon: String,
    title: String
): TextView {

    return TextView(this).apply {

        text = "$icon  $title"

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
            dp(4),
            dp(8),
            dp(4),
            dp(4)
        )
    }
}


private fun adminSectionNote(
    textValue: String
): TextView {

    return TextView(this).apply {

        text = textValue

        textSize = 11f

        setTextColor(
            Color.rgb(
                71,
                85,
                105
            )
        )

        setPadding(
            dp(4),
            0,
            dp(4),
            dp(6)
        )
    }
}

private fun adminActionButton(
    textValue: String,
    click: () -> Unit
): Button {

    val buttonColor = when {
        textValue.contains("DELETE", ignoreCase = true) ||
        textValue.contains("🗑") ->
            Color.rgb(185, 35, 35)

        textValue.contains("SAVE", ignoreCase = true) ||
        textValue.contains("PUBLISH", ignoreCase = true) ||
        textValue.contains("💾") ->
            Color.rgb(22, 125, 75)

        textValue.contains("EDIT", ignoreCase = true) ||
        textValue.contains("✏") ->
            Color.rgb(124, 58, 237)

        textValue.contains("HOME", ignoreCase = true) ||
        textValue.contains("🏠") ->
            Color.rgb(2, 132, 199)

        textValue.contains("NOTICE", ignoreCase = true) ||
        textValue.contains("📢") ->
            Color.rgb(217, 119, 6)

        textValue.contains("MEDIA", ignoreCase = true) ||
        textValue.contains("📷") ||
        textValue.contains("🎥") ->
            Color.rgb(13, 148, 136)

        textValue.contains("ANALYTICS", ignoreCase = true) ||
        textValue.contains("📊") ->
            Color.rgb(71, 85, 105)

        else ->
            Color.rgb(7, 89, 133)
    }

    return Button(this).apply {

        text = textValue

        textSize = 11f

        typeface = Typeface.DEFAULT_BOLD

        setTextColor(Color.WHITE)

        setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                buttonColor
            )
        )

        minHeight = 0
        minimumHeight = 0

        setPadding(
            dp(5),
            0,
            dp(5),
            0
        )

        setOnClickListener {
            click()
        }
    }
}


private fun addFormattingToolbar(
    box: LinearLayout,
    bodyField: EditText
) {
    val toolbar = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun formatSelection(
        prefix: String,
        suffix: String = prefix
    ) {
        val start = bodyField.selectionStart
        val end = bodyField.selectionEnd

        if (start < 0 || end <= start) {
            toast("पहले विवरण में टेक्स्ट चुनें")
            return
        }

        val selected = bodyField.text
            .subSequence(start, end)
            .toString()

        bodyField.text.replace(
            start,
            end,
            prefix + selected + suffix
        )

        bodyField.setSelection(
            start + prefix.length,
            start + prefix.length + selected.length
        )
    }

    toolbar.addView(
        adminActionButton("B  बोल्ड") {
            formatSelection("*")
        }
    )

    toolbar.addView(
        adminActionButton("H  हाइलाइट") {
            formatSelection("`")
        }
    )

    toolbar.addView(
        adminActionButton("> हल्का") {
            val start = bodyField.selectionStart
            val end = bodyField.selectionEnd

            if (start < 0 || end <= start) {
                toast("पहले विवरण में टेक्स्ट चुनें")
            } else {
                val selected = bodyField.text
                    .subSequence(start, end)
                    .toString()

                val quoted = selected
                    .lines()
                    .joinToString("\n") { "> $it" }

                bodyField.text.replace(
                    start,
                    end,
                    quoted
                )
            }
        }
    )

    box.addView(
        TextView(this).apply {
            text = "टेक्स्ट चुनें, फिर फ़ॉर्मेटिंग बटन दबाएँ"
            textSize = 11f
            setTextColor(Color.DKGRAY)
        }
    )

    box.addView(toolbar)
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

        registerAdminSession(uid)
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

                        registerAdminSession(uid)
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

private fun registerAdminSession(uid: String) {
    val sessionRef =
        db.collection("admin_sessions").document()

    val sessionData = hashMapOf<String, Any>(
        "uid" to uid,
        "email" to (auth.currentUser?.email ?: ""),
        "deviceLabel" to
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
        "appVersion" to "unknown",
        "createdAt" to
            com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "lastSeenAt" to
            com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "revoked" to false
    )

    sessionRef.set(sessionData)
        .addOnSuccessListener {
            adminSessionId = sessionRef.id

            adminSessionListener?.remove()
            adminSessionListener =
                sessionRef.addSnapshotListener { snapshot, error ->

                    if (
                        snapshot?.exists() == true &&
                        snapshot.getBoolean("revoked") == true &&
                        adminSessionId == sessionRef.id
                    ) {
                        adminSessionId = null
                        adminSessionListener?.remove()
                        adminSessionListener = null

                        auth.signOut()
                        setContentView(loginUi())

                        Toast.makeText(
                            this@AdminActivity,
                            "यह Admin Session बंद कर दिया गया है।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
        .addOnFailureListener { error ->
            Toast.makeText(
                this,
                "Admin session दर्ज नहीं हुआ: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
}

private fun showAdminSessionsDialog() {
    db.collection("admin_sessions")
        .get()
        .addOnSuccessListener { result ->

            val sessions = result.documents.sortedByDescending {
                it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            }

            if (sessions.isEmpty()) {
                Toast.makeText(
                    this,
                    "कोई Admin Session नहीं मिला।",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }

            val activeCount = sessions.count {
                it.getBoolean("revoked") != true
            }

            val labels = sessions.mapIndexed { index, session ->
                val device =
                    session.getString("deviceLabel") ?: "Unknown device"
                val email =
                    session.getString("email") ?: "Email उपलब्ध नहीं"
                val uid =
                    session.getString("uid") ?: ""
                val status =
                    if (session.getBoolean("revoked") == true) {
                        "बंद"
                    } else {
                        "चालू"
                    }

                "${index + 1}. $device\n$email\nUID: ${uid.take(8)}… | $status"
            }

            AlertDialog.Builder(this)
                .setTitle(
                    "🔐 Admin Sessions — चालू: $activeCount"
                )
                .setItems(labels.toTypedArray()) { _, which ->

                    val selected = sessions[which]
                    val sessionId = selected.id
                    val isRevoked =
                        selected.getBoolean("revoked") == true
                    val device =
                        selected.getString("deviceLabel")
                            ?: "Unknown device"
                    val email =
                        selected.getString("email") ?: ""

                    if (isRevoked) {
                        Toast.makeText(
                            this,
                            "यह Session पहले से बंद है।",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("Session बंद करें?")
                            .setMessage(
                                "डिवाइस: $device\n$email\n\n" +
                                    "इस डिवाइस का Admin Session बंद किया जाएगा।"
                            )
                            .setNegativeButton("रद्द करें", null)
                            .setPositiveButton("बंद करें") { _, _ ->

                                db.collection("admin_sessions")
                                    .document(sessionId)
                                    .update(
                                        mapOf(
                                            "revoked" to true,
                                            "revokedAt" to
                                                com.google.firebase.firestore
                                                    .FieldValue.serverTimestamp()
                                        )
                                    )
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Session बंद कर दिया गया।",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    .addOnFailureListener { error ->
                                        Toast.makeText(
                                            this,
                                            "Session बंद नहीं हुआ: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                            .show()
                    }
                }
                .setNegativeButton("बंद", null)
                .show()
        }
        .addOnFailureListener { error ->
            Toast.makeText(
                this,
                "Sessions लोड नहीं हुए: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
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
topRow.addView(
            Button(this).apply {
                text = "🔐 Sessions"
                textSize = 10f
                minHeight = 0

                setPadding(
                    dp(3),
                    0,
                    dp(3),
                    0
                )

                setOnClickListener {
                    showAdminSessionsDialog()
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
        // =========================================================
// HOME MANAGEMENT CARD
// =========================================================

val homeManagement =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.VERTICAL

        setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(10)
        )

        setBackgroundColor(
            Color.rgb(
                239,
                248,
                255
            )
        )

        elevation = 2f
    }

fixed.addView(
    homeManagement,
    LinearLayout.LayoutParams(
        -1,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {

        topMargin = dp(5)
        bottomMargin = dp(5)
    }
)

homeManagement.addView(
    adminSectionTitle(
        "🏠",
        "HOME MANAGEMENT"
    )
)

homeManagement.addView(
    adminSectionNote(
        "Home के मुख्य buttons और उनके अंदर के sub-buttons यहाँ से manage करें।"
    )
)



homeManagement.addView(
    adminActionButton(
        "🔽  MANAGE SUB-CATEGORIES"
    ) {
        showHomeSubCategoryManager("")
    },
    LinearLayout.LayoutParams(
        -1,
        dp(38)
    )
)


// =========================================================
// MORE MENU CARD
// =========================================================

val moreManagement =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.VERTICAL

        setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(10)
        )

        setBackgroundColor(
            Color.rgb(
                247,
                244,
                255
            )
        )

        elevation = 2f
    }

fixed.addView(
    moreManagement,
    LinearLayout.LayoutParams(
        -1,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {

        topMargin = dp(5)
        bottomMargin = dp(5)
    }
)

moreManagement.addView(
    adminSectionTitle(
        "☰",
        "MORE MENU MANAGEMENT"
    )
)

moreManagement.addView(
    adminSectionNote(
        "More में दिखने वाले menu को बिना APK update के बदलें।"
    )
)

moreManagement.addView(
    adminActionButton(
        "☰  MANAGE MORE MENU"
    ) {
        showMoreMenuManager()
    },
    LinearLayout.LayoutParams(
        -1,
        dp(38)
    )
)
moreManagement.addView(
    adminActionButton("🌐  MANAGE WEBSITE LINKS") {
        showHeaderLinkManager(
            "header_website_links",
            "Website Links",
            listOf(
                mapOf("icon" to "🌐", "label" to "Shiksha Rojgar", "url" to "https://www.shiksharojgar.com/"),
                mapOf("icon" to "🌐", "label" to "Vacancy Bazaar", "url" to "https://www.vacancybazaar.in/")
            )
        )
    },
    LinearLayout.LayoutParams(-1, dp(38))
)

moreManagement.addView(
    adminActionButton("🤝  MANAGE SOCIAL MEDIA LINKS") {
        showHeaderLinkManager(
            "header_social_links",
            "Social Media Links",
            listOf(
                mapOf("icon" to "📱", "label" to "WhatsApp Channel", "url" to "https://www.whatsapp.com/channel/0029Vb6GEDZKLaHxODkeHP1a"),
                mapOf("icon" to "✈️", "label" to "Telegram", "url" to "https://t.me/Shiksha_Rojgar"),
                mapOf("icon" to "▶️", "label" to "YouTube", "url" to "https://youtube.com/@mp_education_jobs_news"),
                mapOf("icon" to "📘", "label" to "Facebook", "url" to "https://www.facebook.com/share/1EGdKsY3zL/"),
                mapOf("icon" to "📸", "label" to "Instagram", "url" to "https://www.instagram.com/shiksha_rojgar?igsh=MX"),
                mapOf("icon" to "𝕏", "label" to "X / Twitter", "url" to "https://x.com/EducationNewsMP?t=pgn4MwOAqY7-WIaoJgX5_A&s=09"),
                mapOf("icon" to "💬", "label" to "ShareChat", "url" to "https://sharechat.com/profile/4200901361?d=n"),
                mapOf("icon" to "💬", "label" to "Arattai", "url" to "https://aratt.ai/@a_s_chouhan")
            )
        )
    },
    LinearLayout.LayoutParams(-1, dp(38))
)

// =========================================================
// HOME CONTENT CARD
// =========================================================

val homeContent =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.VERTICAL

        setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(10)
        )

        setBackgroundColor(
            Color.rgb(
                255,
                250,
                240
            )
        )

        elevation = 2f
    }

fixed.addView(
    homeContent,
    LinearLayout.LayoutParams(
        -1,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {

        topMargin = dp(5)
        bottomMargin = dp(7)
    }
)

homeContent.addView(
    adminSectionTitle(
        "📚",
        "HOME PAGES / CONTENT"
    )
)

homeContent.addView(
    adminSectionNote(
        "Study Material, Career Guide, Notice, Useful Tools आदि का content manage करें।"
    )
)


    

        // =========================================================
// HOME PAGE CONTENT — 4 FIXED MANAGEMENT BUTTONS
// =========================================================

val pageManager =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

// -------------------------
// ROW 1
// -------------------------

val pageRow1 =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

pageManager.addView(
    pageRow1,
    LinearLayout.LayoutParams(
        -1,
        dp(40)
    ).apply {
        bottomMargin = dp(6)
    }
)

// 1. Study Material / Syllabus
pageRow1.addView(
    adminActionButton(
        "📖  Study Material / Syllabus"
    ) {
        editHomePage(
            "study_material",
            "Study Material / Syllabus"
        )
    },
    LinearLayout.LayoutParams(
        0,
        -1,
        1f
    ).apply {
        rightMargin = dp(3)
    }
)

// 2. Career Guide / Useful Tools
pageRow1.addView(
    adminActionButton(
        "📚  Career Guide / Useful Tools"
    ) {
        editHomePage(
            "career",
            "Career Guide / Useful Tools"
        )
    },
    LinearLayout.LayoutParams(
        0,
        -1,
        1f
    ).apply {
        leftMargin = dp(3)
    }
)

// -------------------------
// ROW 2
// -------------------------

val pageRow2 =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

pageManager.addView(
    pageRow2,
    LinearLayout.LayoutParams(
        -1,
        dp(40)
    )
)

 // 4. Implementation Information — Full Width
pageRow2.addView(
    adminActionButton(
        "📌  Important Information / Imp Info"
    ) {
        editHomePage(
            "important_information",
            "Implementation Information"
        )
    },
    LinearLayout.LayoutParams(
        -1,
        -1
    )
)


homeContent.addView(pageManager)

homeContent.addView(
    adminSectionTitle(
        "📝",
        "CATEGORY POST MANAGEMENT"
    )
)
// =========================================================
// SHIKSHA ROJGAR GROUP - ADMIN MODERATION
// =========================================================

homeContent.addView(
    adminActionButton(
        "🛡️  शिक्षा रोजगार ग्रुप — पोस्ट समीक्षा"
    ) {
        startActivity(
            android.content.Intent(
                this,
                GroupAdminActivity::class.java
            )
        )
    },
    LinearLayout.LayoutParams(
        -1,
        dp(48)
    ).apply {
        bottomMargin = dp(10)
    }
)

// =========================================================
// USER MANAGEMENT
// =========================================================

homeContent.addView(
    adminActionButton(
        "👥 यूजर प्रबंधन — User Management"
    ) {
        showUserManagementDialog()
    },
    LinearLayout.LayoutParams(
        -1,
        dp(48)
    ).apply {
        bottomMargin = dp(10)
    }
)


listOf(
    "📢 Notice / Update" to "notice",
    "📌 Important Information" to "important_information",
    "📖 Study Material / Syllabus" to "study_material",
    "📚 Career Guide / Useful Tools" to "career_guide"
).forEach { (label, categoryKey) ->

    val borderColor = when (categoryKey) {
        "notice" -> Color.rgb(25, 118, 210)
        "important_information" -> Color.rgb(0, 150, 136)
        "study_material" -> Color.rgb(123, 104, 238)
        else -> Color.rgb(230, 126, 34)
    }

    val categoryCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(8), dp(6), dp(8), dp(8))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(8).toFloat()
            setStroke(dp(2), borderColor)
        }
    }

    categoryCard.addView(
        TextView(this).apply {
            text = label
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(borderColor)
            setPadding(dp(4), dp(2), dp(4), dp(6))
        }
    )

    val actionRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun categoryButton(
        buttonText: String,
        buttonColor: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = buttonText
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(buttonColor)
            minHeight = dp(40)
            minimumHeight = dp(40)
            setPadding(dp(3), 0, dp(3), 0)
            setOnClickListener { action() }
        }
    }

    actionRow.addView(
        categoryButton(
            "📋 Manage Post",
            Color.rgb(25, 118, 210)
        ) {
            showCategoryPostManager(categoryKey, label)
        },
        LinearLayout.LayoutParams(
            0, dp(44), 1f
        ).apply {
            marginEnd = dp(5)
        }
    )

    actionRow.addView(
        categoryButton(
            "➕ Create Post",
            Color.rgb(46, 125, 50)
        ) {
            showCommonPostCreator(categoryKey)
        },
        LinearLayout.LayoutParams(
            0, dp(44), 1f
        )
    )

    categoryCard.addView(
        actionRow,
        LinearLayout.LayoutParams(-1, -2)
    )

    homeContent.addView(
        categoryCard,
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
            bottomMargin = dp(4)
        }
    )
}




// =====================================================
// NOTICE MANAGEMENT
// =====================================================

homeContent.addView(
    Button(this).apply {

        text =
            "📢 MANAGE NOTICES"

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

            showNoticeManager()
        }
    },
    LinearLayout.LayoutParams(
        -1,
        dp(36)
    ).apply {

        topMargin = dp(3)
        bottomMargin = dp(3)
    }
)

        // =====================================================
        // COMMON POST CREATOR
        // =====================================================

        homeContent.addView(
            Button(this).apply {

                text = "📝 COMMON POST CREATOR"

                textSize = 12f

                setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

                setTextColor(Color.WHITE)

                setBackgroundColor(
                    Color.rgb(22, 125, 75)
                )

                minHeight = 0

                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
                )

                setOnClickListener {
                    showCommonPostCreator()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(36)
            ).apply {
                topMargin = dp(3)
                bottomMargin = dp(3)
            }
        )

        // =========================================================
// USER MANAGEMENT DIALOG
// =========================================================

private fun showUserManagementDialog() {

    val currentAdmin = auth.currentUser

    if (currentAdmin == null) {
        toast("पहले Admin Login करें")
        return
    }

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(16),
            dp(12),
            dp(16),
            dp(12)
        )
    }

    val summary = TextView(this).apply {
        text = "⏳ यूजर आँकड़े लोड हो रहे हैं..."
        textSize = 14f
        setTextColor(Color.DKGRAY)
    }

    root.addView(summary)

    val userList = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    val scroll = ScrollView(this)

    scroll.addView(userList)

    root.addView(
        scroll,
        LinearLayout.LayoutParams(
            -1,
            dp(360)
        )
    )

    val dialog = AlertDialog.Builder(this)
        .setTitle("👥 यूजर प्रबंधन")
        .setView(root)
        .setPositiveButton("बंद करें", null)
        .create()

    dialog.show()

    summary.text =
        "पंजीकृत प्रोफाइल और चैनल फॉलो रिकॉर्ड पढ़े जा रहे हैं।"

    db.collection("group_profiles")
        .get()
        .addOnSuccessListener { profiles ->

            if (isFinishing || isDestroyed) {
                return@addOnSuccessListener
            }

            db.collection("channel_followers")
                .get()
                .addOnSuccessListener { followers ->

                    if (isFinishing || isDestroyed) {
                        return@addOnSuccessListener
                    }

                    val followerIds = followers.documents
                        .map { it.id }
                        .toSet()

                    val registeredIds = profiles.documents
                        .map { it.id }
                        .toSet()

                    val registeredCount = registeredIds.size

                    val followerCount = followerIds
                        .count { it in registeredIds }

                    val notFollowingCount =
                        (registeredCount - followerCount)
                            .coerceAtLeast(0)

                    summary.text =
                        "📋 पंजीकृत प्रोफाइल: $registeredCount\n\n" +
                        "🔔 चैनल फॉलो करने वाले: $followerCount\n\n" +
                        "🔕 चैनल फॉलो नहीं करने वाले: " +
                        "$notFollowingCount\n\n" +
                        "ℹ️ ये आँकड़े उपलब्ध प्रोफाइल रिकॉर्ड पर " +
                        "आधारित हैं।"

                    userList.removeAllViews()

                    if (profiles.isEmpty) {

                        userList.addView(
                            TextView(this).apply {
                                text =
                                    "अभी कोई पंजीकृत प्रोफाइल रिकॉर्ड नहीं मिली।"
                                textSize = 14f
                                setPadding(
                                    0,
                                    dp(16),
                                    0,
                                    dp(16)
                                )
                            }
                        )

                        return@addOnSuccessListener
                    }

                    profiles.documents.forEach { profile ->

                        val uid = profile.id

                        val username =
                            profile.getString("username")
                                ?: profile.getString("name")
                                ?: "रिकॉर्ड में नाम उपलब्ध नहीं"

                        val email =
                            profile.getString("email")
                                ?: "रिकॉर्ड में ईमेल उपलब्ध नहीं"

                        val mobile =
                            profile.getString("mobileNumber")
                                ?: "रिकॉर्ड में मोबाइल उपलब्ध नहीं"

                        val following =
                            uid in followerIds

                        val card = LinearLayout(this).apply {
                            orientation =
                                LinearLayout.VERTICAL

                            setPadding(
                                dp(10),
                                dp(10),
                                dp(10),
                                dp(10)
                            )

                            setBackgroundColor(
                                Color.rgb(243, 247, 250)
                            )
                        }

                        card.addView(
                            TextView(this).apply {
                                text = "👤 $username"
                                textSize = 16f
                                typeface =
                                    Typeface.DEFAULT_BOLD
                            }
                        )

                        card.addView(
                            TextView(this).apply {
                                text = "UID: $uid"
                                textSize = 11f
                            }
                        )

                        card.addView(
                            TextView(this).apply {
                                text = "📧 $email"
                                textSize = 12f
                            }
                        )

                        card.addView(
                            TextView(this).apply {
                                text = "📱 $mobile"
                                textSize = 12f
                            }
                        )

                        card.addView(
                            TextView(this).apply {
                                text = if (following) {
                                    "🔔 चैनल फॉलो: हाँ"
                                } else {
                                    "🔕 चैनल फॉलो: नहीं"
                                }

                                textSize = 13f

                                setTextColor(
                                    if (following) {
                                        Color.rgb(22, 125, 75)
                                    } else {
                                        Color.rgb(185, 35, 35)
                                    }
                                )
                            }
                        )

                        userList.addView(
                            card,
                            LinearLayout.LayoutParams(
                                -1,
                                -2
                            ).apply {
                                bottomMargin = dp(10)
                            }
                        )
                    }
                }
                .addOnFailureListener { error ->

                    if (!isFinishing && !isDestroyed) {
                        summary.text =
                            "चैनल फॉलो रिकॉर्ड नहीं पढ़ सके।\n" +
                            (error.message ?: "अनुमति या नेटवर्क की समस्या")
                    }
                }
        }
        .addOnFailureListener { error ->

            if (!isFinishing && !isDestroyed) {
                summary.text =
                    "यूजर प्रोफाइल नहीं पढ़ सके।\n" +
                    (error.message ?: "अनुमति या नेटवर्क की समस्या")
            }
        }
}

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
        // UPPER ADMIN AREA — 60%
        // =====================================================

        val upperScroll =
            ScrollView(this).apply {

                isFillViewport = true

                isVerticalScrollBarEnabled = true

                clipToPadding = false
            }

        upperScroll.addView(
            fixed,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            upperScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.60f
            )
        )

        // =====================================================
        // LOWER CHANNEL POSTS AREA — 40%
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
                0.40f
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
// MORE MENU MANAGER
// =========================================================

private fun showMoreMenuManager() {

    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                "☰ More Menu Management"
            )
            .setMessage(
                "More Menu items load हो रहे हैं…"
            )
            .setPositiveButton(
                "CLOSE",
                null
            )
            .create()

    dialog.show()

    db.collection(
        "more_menu_items"
    )
        .orderBy(
            "position"
        )
        .get()
        .addOnSuccessListener { snap ->

            dialog.dismiss()

            val items =
                snap.documents.toMutableList()

            val names =
                ArrayList<String>()

            items.forEach { doc ->

                val icon =
                    doc.getString("icon")
                        ?: "☰"

                val label =
                    doc.getString("label")
                        ?: "Unnamed"

    
        
                val enabled =
                    doc.getBoolean("enabled")
                        ?: true

                val status =
                    if (enabled) "✅" else "❌"

                names.add(
                    "$status $icon $label"
                )
            }

            names.add(
                "➕ Add New More Menu"
            )

            AlertDialog.Builder(this)
                .setTitle(
                    "☰ More Menu"
                )
                .setItems(
                    names.toTypedArray()
                ) { _, which ->

                    if (which == items.size) {

                        showMoreMenuEditor(
                            null
                        )

                    } else {

                        showMoreMenuActions(
                            items[which]
                        )
                    }
                }
                .setNegativeButton(
                    "CLOSE",
                    null
                )
                .show()
        }
        .addOnFailureListener { e ->

            dialog.dismiss()

            Toast.makeText(
                this,
                "More Menu load नहीं हुआ: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
}

// =========================================================
// HEADER WEBSITE / SOCIAL LINKS MANAGEMENT
// =========================================================

private fun showHeaderLinkManager(
    collectionName: String,
    screenTitle: String,
    defaults: List<Map<String, String>>
) {
    db.collection(collectionName)
        .orderBy("position")
        .get()
        .addOnSuccessListener { snap ->
            if (snap.isEmpty && defaults.isNotEmpty()) {
                val batch = db.batch()

                defaults.forEachIndexed { index, item ->
                    val ref = db.collection(collectionName).document()
                    val data = hashMapOf<String, Any>(
                        "icon" to (item["icon"] ?: "🔗"),
                        "label" to (item["label"] ?: "New Link"),
                        "url" to (item["url"] ?: ""),
                        "enabled" to true,
                        "position" to index
                    )
                    batch.set(ref, data)
                }

                batch.commit()
                    .addOnSuccessListener {
                        showHeaderLinkManager(
                            collectionName,
                            screenTitle,
                            emptyList()
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Links save नहीं हुए: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                return@addOnSuccessListener
            }

            val documents = snap.documents
            val labels = documents.map { doc ->
                val icon = doc.getString("icon") ?: "🔗"
                val label = doc.getString("label") ?: "Unnamed"
                val enabled = doc.getBoolean("enabled") ?: true
                "${if (enabled) "✅" else "❌"} $icon $label"
            }.toMutableList()

            labels.add("➕ नया लिंक जोड़ें")

            AlertDialog.Builder(this)
                .setTitle("🔗 $screenTitle")
                .setItems(labels.toTypedArray()) { _, which ->
                    if (which == documents.size) {
                        showHeaderLinkEditor(
                            collectionName,
                            screenTitle,
                            null,
                            documents.size
                        )
                    } else {
                        showHeaderLinkActions(
                            collectionName,
                            screenTitle,
                            documents,
                            which
                        )
                    }
                }
                .setNegativeButton("बंद करें", null)
                .show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(
                this,
                "Links load नहीं हुए: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
}

private fun showHeaderLinkActions(
    collectionName: String,
    screenTitle: String,
    documents: List<com.google.firebase.firestore.DocumentSnapshot>,
    index: Int
) {
    val document = documents[index]
    val enabled = document.getBoolean("enabled") ?: true

    val actions = arrayOf(
        "✏️ नाम / आइकन / URL बदलें",
        if (enabled) "❌ लिंक बंद करें" else "✅ लिंक चालू करें",
        "⬆️ क्रम ऊपर करें",
        "⬇️ क्रम नीचे करें",
        "🗑️ लिंक हटाएँ"
    )

    AlertDialog.Builder(this)
        .setTitle(document.getString("label") ?: screenTitle)
        .setItems(actions) { _, choice ->
            when (choice) {
                0 -> showHeaderLinkEditor(
                    collectionName,
                    screenTitle,
                    document,
                    index
                )

                1 -> document.reference
                    .update("enabled", !enabled)
                    .addOnSuccessListener {
                        showHeaderLinkManager(
                            collectionName,
                            screenTitle,
                            emptyList()
                        )
                    }

                2 -> {
                    if (index > 0) {
                        val previous = documents[index - 1]
                        val currentPosition =
                            document.getLong("position") ?: index.toLong()
                        val previousPosition =
                            previous.getLong("position") ?: (index - 1).toLong()

                        db.runBatch { batch ->
                            batch.update(document.reference, "position", previousPosition)
                            batch.update(previous.reference, "position", currentPosition)
                        }.addOnSuccessListener {
                            showHeaderLinkManager(
                                collectionName,
                                screenTitle,
                                emptyList()
                            )
                        }
                    } else {
                        Toast.makeText(this, "यह पहला लिंक है।", Toast.LENGTH_SHORT).show()
                    }
                }

                3 -> {
                    if (index < documents.lastIndex) {
                        val next = documents[index + 1]
                        val currentPosition =
                            document.getLong("position") ?: index.toLong()
                        val nextPosition =
                            next.getLong("position") ?: (index + 1).toLong()

                        db.runBatch { batch ->
                            batch.update(document.reference, "position", nextPosition)
                            batch.update(next.reference, "position", currentPosition)
                        }.addOnSuccessListener {
                            showHeaderLinkManager(
                                collectionName,
                                screenTitle,
                                emptyList()
                            )
                        }
                    } else {
                        Toast.makeText(this, "यह आखिरी लिंक है।", Toast.LENGTH_SHORT).show()
                    }
                }

                4 -> AlertDialog.Builder(this)
                    .setTitle("लिंक हटाएँ?")
                    .setMessage("यह लिंक Firestore से हट जाएगा।")
                    .setNegativeButton("रद्द करें", null)
                    .setPositiveButton("हटाएँ") { _, _ ->
                        document.reference.delete()
                            .addOnSuccessListener {
                                showHeaderLinkManager(
                                    collectionName,
                                    screenTitle,
                                    emptyList()
                                )
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "लिंक नहीं हटा: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .show()
            }
        }
        .setNegativeButton("वापस", null)
        .show()
}

private fun showHeaderLinkEditor(
    collectionName: String,
    screenTitle: String,
    document: com.google.firebase.firestore.DocumentSnapshot?,
    position: Int
) {
    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(8), dp(20), dp(8))
    }

    fun addField(hintText: String, value: String): EditText {
        val editText = EditText(this).apply {
            hint = hintText
            setText(value)
            textSize = 14f
        }

        layout.addView(
            editText,
            LinearLayout.LayoutParams(-1, dp(48))
        )
        return editText
    }

    val iconField = addField(
        "आइकन / Emoji",
        document?.getString("icon") ?: "🔗"
    )
    val labelField = addField(
        "लिंक का नाम",
        document?.getString("label") ?: ""
    )
    val urlField = addField(
        "पूरा URL (https://...)",
        document?.getString("url") ?: ""
    )

    AlertDialog.Builder(this)
        .setTitle(if (document == null) "नया लिंक जोड़ें" else "लिंक संपादित करें")
        .setView(layout)
        .setNegativeButton("रद्द करें", null)
        .setPositiveButton("सेव करें") { _, _ ->
            val icon = iconField.text.toString().trim()
            val label = labelField.text.toString().trim()
            val url = urlField.text.toString().trim()

            if (label.isBlank() || !url.startsWith("https://") && !url.startsWith("http://")) {
                Toast.makeText(
                    this,
                    "नाम और सही पूरा URL भरें।",
                    Toast.LENGTH_LONG
                ).show()
                return@setPositiveButton
            }

            val data = hashMapOf<String, Any>(
                "icon" to if (icon.isBlank()) "🔗" else icon,
                "label" to label,
                "url" to url,
                "enabled" to (document?.getBoolean("enabled") ?: true),
                "position" to (document?.getLong("position")?.toInt() ?: position)
            )

            val operation = if (document == null) {
                db.collection(collectionName).document().set(data)
            } else {
                document.reference.set(data)
            }

            operation
                .addOnSuccessListener {
                    Toast.makeText(this, "लिंक सेव हो गया।", Toast.LENGTH_SHORT).show()
                    showHeaderLinkManager(
                        collectionName,
                        screenTitle,
                        emptyList()
                    )
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "सेव नहीं हुआ: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
        .show()
}
// =========================================================
// MORE MENU ACTIONS
// =========================================================

private fun showMoreMenuActions(
    document:
        com.google.firebase.firestore.DocumentSnapshot
) {

    val label =
        document.getString("label")
            ?: "More Menu"

    val enabled =
        document.getBoolean("enabled")
            ?: true

    val options =
        arrayOf(
            "✏️ Edit",
            if (enabled)
                "❌ Disable"
            else
                "✅ Enable",
            "⬆️ Move Up",
            "⬇️ Move Down",
            "🗑 Delete"
        )

    AlertDialog.Builder(this)
        .setTitle(
            "☰ $label"
        )
        .setItems(
            options
        ) { _, which ->

            when (which) {

                0 -> {
                    showMoreMenuEditor(
                        document
                    )
                }

                1 -> {

                    document.reference
                        .update(
                            "enabled",
                            !enabled
                        )
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "More Menu status बदल गया",
                                Toast.LENGTH_SHORT
                            ).show()

                            showMoreMenuManager()
                        }
                }

                2 -> {
                    moveMoreMenuItem(
                        document,
                        -1
                    )
                }

                3 -> {
                    moveMoreMenuItem(
                        document,
                        1
                    )
                }

                4 -> {

                    AlertDialog.Builder(this)
                        .setTitle(
                            "Delete More Menu?"
                        )
                        .setMessage(
                            "क्या \"$label\" को delete करना है?"
                        )
                        .setNegativeButton(
                            "CANCEL",
                            null
                        )
                        .setPositiveButton(
                            "DELETE"
                        ) { _, _ ->

                            document.reference
                                .delete()
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        this,
                                        "More Menu delete हो गया",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showMoreMenuManager()
                                }
                        }
                        .show()
                }
            }
        }
        .setNegativeButton(
            "BACK",
            null
        )
        .show()
}


// =========================================================
// MORE MENU EDITOR
// =========================================================

private fun showMoreMenuEditor(
    document:
        com.google.firebase.firestore.DocumentSnapshot?
) {

    val isNew =
        document == null

    val layout =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(8),
                dp(20),
                dp(8)
            )
        }

    fun field(
        hint: String,
        value: String
    ): EditText {

        return EditText(this).apply {

            this.hint = hint

            setText(value)

            textSize = 14f

            setPadding(
                dp(4),
                dp(6),
                dp(4),
                dp(6)
            )

            layout.addView(
                this,
                LinearLayout.LayoutParams(
                    -1,
                    dp(48)
                )
            )
        }
    }

    val icon =
        field(
            "Icon जैसे ☰ / 📢 / 📚",
            document?.getString("icon")
                ?: "☰"
        )

    val label =
        field(
            "Menu Name",
            document?.getString("label")
                ?: ""
        )
val menuType =
        field(
            "Menu Type: section / item",
            document?.getString("menuType")
                ?: "item"
        )

    val menuId =
        field(
            "Menu ID (Section के लिए): जैसे explore_sections",
            document?.getString("menuId")
                ?: ""
        )

    val parentId =
        field(
            "Parent Menu ID: जैसे explore_sections",
            document?.getString("parentId")
                ?: ""
        )
    val position =
        field(
            "Position जैसे 1, 2, 3",
            (
                document
                    ?.getLong("position")
                    ?: 1L
                ).toString()
        )

    val actionType =
        field(
            "Action Type: internal / external / phone / whatsapp / email / text",
            document?.getString("actionType")
                ?: "external"
        )

    val target =
        field(
            "Target / Page ID",
            document?.getString("target")
                ?: ""
        )

    val url =
        field(
            "URL",
            document?.getString("url")
                ?: ""
        )

    val phone =
        field(
            "Phone Number",
            document?.getString("phone")
                ?: ""
        )

    val whatsapp =
        field(
            "WhatsApp Number",
            document?.getString("whatsapp")
                ?: ""
        )

    val email =
        field(
            "Email",
            document?.getString("email")
                ?: ""
        )

    val content =
        field(
            "Text / About / Help Content",
            document?.getString("content")
                ?: ""
        )

    layout.addView(
        CheckBox(this).apply {

            text =
                "Menu Enabled"

            isChecked =
                document?.getBoolean("enabled")
                    ?: true

            tag =
                "enabled"
        }
    )

    val enabledBox =
        layout.getChildAt(
            layout.childCount - 1
        ) as CheckBox

    AlertDialog.Builder(this)
        .setTitle(
            if (isNew)
                "➕ Add More Menu"
            else
                "✏️ Edit More Menu"
        )
        .setView(layout)
        .setNegativeButton(
            "CANCEL",
            null
        )
        .setPositiveButton(
            "SAVE"
        ) { _, _ ->

            val data =
                hashMapOf<String, Any>(

                    "icon" to
                        icon.text
                            .toString()
                            .trim(),

                    "menuType" to
                        menuType.text
                            .toString()
                            .trim()
                            .lowercase(),

                    "menuId" to
                        menuId.text
                            .toString()
                            .trim(),

                    "parentId" to
                        parentId.text
                            .toString()
                            .trim(),
                    
                    "label" to
                        label.text
                            .toString()
                            .trim(),

                    "position" to
                        (
                            position.text
                                .toString()
                                .toLongOrNull()
                                ?: 1L
                            ),

                    "actionType" to
                        actionType.text
                            .toString()
                            .trim(),

                    "target" to
                        target.text
                            .toString()
                            .trim(),

                    "url" to
                        url.text
                            .toString()
                            .trim(),

                    "phone" to
                        phone.text
                            .toString()
                            .trim(),

                    "whatsapp" to
                        whatsapp.text
                            .toString()
                            .trim(),

                    "email" to
                        email.text
                            .toString()
                            .trim(),

                    "content" to
                        content.text
                            .toString()
                            .trim(),

                    "enabled" to
                        enabledBox.isChecked
                )

            val task =
                if (isNew) {

                    db.collection(
                        "more_menu_items"
                    )
                        .add(data)

                } else {

                    document!!
                        .reference
                        .set(
                            data
                        )
                }

            task.addOnSuccessListener {

                Toast.makeText(
                    this,
                    "More Menu सेव हो गया",
                    Toast.LENGTH_SHORT
                ).show()

                showMoreMenuManager()

            }.addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Save error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        .show()
}


// =========================================================
// MORE MENU MOVE UP / DOWN
// =========================================================

private fun moveMoreMenuItem(
    document:
        com.google.firebase.firestore.DocumentSnapshot,
    direction: Int
) {

    val current =
        document.getLong(
            "position"
        ) ?: 1L

    val newPosition =
        current + direction

    if (newPosition < 1L) {

        Toast.makeText(
            this,
            "यह पहला item है",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    db.collection(
        "more_menu_items"
    )
        .whereEqualTo(
            "position",
            newPosition
        )
        .limit(1)
        .get()
        .addOnSuccessListener { snap ->

            if (snap.isEmpty) {

                document.reference
                    .update(
                        "position",
                        newPosition
                    )
                    .addOnSuccessListener {

                        showMoreMenuManager()
                    }

                return@addOnSuccessListener
            }

            val other =
                snap.documents[0]

            val batch =
                db.batch()

            batch.update(
                document.reference,
                "position",
                newPosition
            )

            batch.update(
                other.reference,
                "position",
                current
            )

            batch.commit()
                .addOnSuccessListener {

                    showMoreMenuManager()
                }
        }
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

    private fun showHomeSubCategoryManager(
    parentId: String
) {

    val dialog = AlertDialog.Builder(this)
        .setTitle("🔽 MANAGE SUB-CATEGORIES")
        .create()

    val root = LinearLayout(this).apply {

        orientation = LinearLayout.VERTICAL

        setPadding(
            dp(12),
            dp(8),
            dp(12),
            dp(8)
        )
    }

    // =====================================================
    // CATEGORY SELECTOR
    // =====================================================

    val categoryLabel = TextView(this).apply {

        text = "Main Category"

        textSize = 13f

        setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        setTextColor(
            Color.rgb(7, 89, 133)
        )

        setPadding(
            0,
            dp(2),
            0,
            dp(4)
        )
    }

    root.addView(categoryLabel)

    val categorySpinner = Spinner(this)

    val categories = arrayOf(
        "Teacher",
        "Student",
        "School",
        "Vacancy",
        "Result",
        "Admit Card"
    )

    val spinnerAdapter = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        categories
    )

    categorySpinner.adapter = spinnerAdapter

    root.addView(
        categorySpinner,
        LinearLayout.LayoutParams(
            -1,
            dp(45)
        )
    )

    // =====================================================
    // ADD BUTTON
    // =====================================================

    val addButton = Button(this).apply {

        text = "➕ ADD SUB-CATEGORY"

        textSize = 12f

        setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        setTextColor(Color.WHITE)

        setBackgroundColor(
            Color.rgb(7, 89, 133)
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
            dp(42)
        ).apply {

            topMargin = dp(6)
            bottomMargin = dp(6)
        }
    )

    // =====================================================
    // SCROLL AREA
    // =====================================================

    val scrollView = ScrollView(this).apply {

        isFillViewport = true
    }

    val listLayout = LinearLayout(this).apply {

        orientation = LinearLayout.VERTICAL
    }

    scrollView.addView(
        listLayout,
        android.widget.FrameLayout.LayoutParams(
    -1,
    -2
)
    )

    root.addView(
        scrollView,
        LinearLayout.LayoutParams(
            -1,
            0,
            1f
        )
    )

    // =====================================================
    // SCROLL INDICATOR
    // =====================================================

    val scrollIndicator = TextView(this).apply {

        text = "↓ नीचे और देखें"

        textSize = 11f

        gravity = Gravity.CENTER

        setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        setTextColor(
            Color.rgb(7, 89, 133)
        )

        setPadding(
            0,
            dp(4),
            0,
            dp(4)
        )

        visibility = View.GONE
    }

    root.addView(
        scrollIndicator,
        LinearLayout.LayoutParams(
            -1,
            dp(30)
        )
    )

    // =====================================================
    // SCROLL POSITION CHECK
    // =====================================================

    scrollView.viewTreeObserver.addOnScrollChangedListener {

        val child = scrollView.getChildAt(0)

        if (child != null) {

            val canScrollDown =
                scrollView.scrollY <
                        child.measuredHeight -
                        scrollView.measuredHeight

            val canScrollUp =
                scrollView.scrollY > 0

            when {

                canScrollDown -> {

                    scrollIndicator.text =
                        "↓ नीचे और देखें"

                    scrollIndicator.visibility =
                        View.VISIBLE
                }

                canScrollUp -> {

                    scrollIndicator.text =
                        "↑ ऊपर और देखें"

                    scrollIndicator.visibility =
                        View.VISIBLE
                }

                else -> {

                    scrollIndicator.visibility =
                        View.GONE
                }
            }
        }
    }

    // =====================================================
    // LOAD SUB-CATEGORIES
    // =====================================================

    fun loadSubCategories(parentId: String) {

        listLayout.removeAllViews()

        val loading = TextView(this).apply {

            text = "लोड हो रहा है..."

            textSize = 13f

            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(15),
                0,
                dp(15)
            )
        }

        listLayout.addView(
            loading,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            )
        )

        db.collection("home_subcategories")
            .whereEqualTo("parentId", parentId)
            .get()
            .addOnSuccessListener { result ->

                listLayout.removeAllViews()

                if (result.isEmpty) {

                    val emptyText =
                        TextView(this).apply {

                            text =
                                "इस category में अभी कोई Sub-Category नहीं है।"

                            textSize = 13f

                            gravity = Gravity.CENTER

                            setPadding(
                                0,
                                dp(20),
                                0,
                                dp(20)
                            )
                        }

                    listLayout.addView(
                        emptyText,
                        LinearLayout.LayoutParams(
                            -1,
                            -2
                        )
                    )

                } else {

                    val documents =
                        result.documents.sortedBy {

                            it.getLong("position")
                                ?: 999999L
                        }

                    documents.forEach { doc ->

                        val card =
                            LinearLayout(this).apply {

                                orientation =
                                    LinearLayout.VERTICAL

                                setPadding(
                                    dp(10),
                                    dp(8),
                                    dp(10),
                                    dp(8)
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
                                ?: "Unnamed"

                        val icon =
                            doc.getString("icon")
                                ?: ""

                        val pageType =
                            doc.getString("pageType")
                                ?: ""

                        val url =
                            doc.getString("url")
                                ?: ""

                        val enabled =
                            doc.getBoolean("enabled")
                                ?: true

                        val position =
                            doc.getLong("position")
                                ?: 0L

                        val info =
                            TextView(this).apply {

                                text =
                                    "$icon  $name\n" +
                                    "Type: $pageType\n" +
                                    "URL: $url\n" +
                                    "Position: $position\n" +
                                    "Enabled: ${if (enabled) "YES" else "NO"}"

                                textSize = 12f

                                setTextColor(
                                    Color.DKGRAY
                                )

                                setPadding(
                                    0,
                                    0,
                                    0,
                                    dp(6)
                                )
                            }

                        card.addView(
                            info,
                            LinearLayout.LayoutParams(
                                -1,
                                -2
                            )
                        )

                        val buttonsContainer =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.VERTICAL
    }

fun smallButton(
    title: String,
    backgroundColor: Int,
    action: () -> Unit
): Button {

    return Button(this).apply {

        text = title

        textSize = 10f

        minHeight = 0
        minWidth = 0

        setPadding(
            dp(4),
            0,
            dp(4),
            0
        )

        setTextColor(Color.WHITE)

        setBackgroundColor(
            backgroundColor
        )

        setOnClickListener {
            action()
        }
    }
}

/* ---------- ROW 1 ---------- */

val row1 =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.HORIZONTAL
    }

row1.addView(
    smallButton(
        "✏️ Edit",
        Color.rgb(33, 150, 243)
    ) {

        showEditSubCategoryDialog(
            doc.id,
            parentId,
            doc
        )
    },
    LinearLayout.LayoutParams(
        0,
        dp(40),
        1f
    ).apply {
        rightMargin = dp(3)
    }
)

row1.addView(
    smallButton(
        "⬆ Up",
        Color.rgb(96, 125, 139)
    ) {

        moveSubCategory(
            doc.id,
            parentId,
            position - 1,
            ::loadSubCategories
        )
    },
    LinearLayout.LayoutParams(
        0,
        dp(40),
        1f
    ).apply {
        rightMargin = dp(3)
    }
)

row1.addView(
    smallButton(
        "⬇ Down",
        Color.rgb(96, 125, 139)
    ) {

        moveSubCategory(
            doc.id,
            parentId,
            position + 1,
            ::loadSubCategories
        )
    },
    LinearLayout.LayoutParams(
        0,
        dp(40),
        1f
    )
)

buttonsContainer.addView(
    row1
)

/* ---------- ROW 2 ---------- */

val row2 =
    LinearLayout(this).apply {

        orientation =
            LinearLayout.HORIZONTAL
    }

row2.addView(
    smallButton(
        if (enabled)
            "🔴 Disable"
        else
            "🟢 Enable",

        if (enabled)
            Color.rgb(198, 40, 40)
        else
            Color.rgb(46, 125, 50)
    ) {

        db.collection(
            "home_subcategories"
        )
            .document(doc.id)
            .update(
                "enabled",
                !enabled
            )
            .addOnSuccessListener {

                toast(
                    if (enabled)
                        "Sub-Category Disabled"
                    else
                        "Sub-Category Enabled"
                )

                loadSubCategories(
                    parentId
                )
            }
            .addOnFailureListener { e ->

                toast(
                    "Status update failed: ${
                        e.message
                            ?: "Firestore error"
                    }"
                )
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

row2.addView(
    smallButton(
        "🗑 Delete",
        Color.rgb(211, 47, 47)
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Delete Sub-Category?"
            )
            .setMessage(
                "क्या \"$name\" को delete करना है?"
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
                    .document(doc.id)
                    .delete()
                    .addOnSuccessListener {

                        toast(
                            "Sub-Category deleted"
                        )

                        loadSubCategories(
                            parentId
                        )
                    }
                    .addOnFailureListener { e ->

                        toast(
                            "Delete failed: ${e.message}"
                        )
                    }
            }
            .show()
    },
    LinearLayout.LayoutParams(
        0,
        dp(40),
        1f
    )
)

buttonsContainer.addView(
    row2
)

card.addView(
    buttonsContainer,
    LinearLayout.LayoutParams(
        -1,
        -2
    )
)

                                
                        
                                

                

                        listLayout.addView(
                            card,
                            LinearLayout.LayoutParams(
                                -1,
                                -2
                            ).apply {

                                bottomMargin = dp(8)
                            }
                        )
                    }
                }

                scrollView.post {

                    val child =
                        scrollView.getChildAt(0)

                    if (child != null) {

                        val canScrollDown =
                            scrollView.scrollY <
                                    child.measuredHeight -
                                    scrollView.measuredHeight

                        if (canScrollDown) {

                            scrollIndicator.text =
                                "↓ नीचे और देखें"

                            scrollIndicator.visibility =
                                View.VISIBLE

                        } else {

                            scrollIndicator.visibility =
                                View.GONE
                        }
                    }
                }
            }
            .addOnFailureListener { e ->

                listLayout.removeAllViews()

                val error =
                    TextView(this).apply {

                        text =
                            "Data load नहीं हुआ:\n${e.message}"

                        textSize = 13f

                        setTextColor(Color.RED)

                        setPadding(
                            0,
                            dp(15),
                            0,
                            dp(15)
                        )
                    }

                listLayout.addView(
                    error,
                    LinearLayout.LayoutParams(
                        -1,
                        -2
                    )
                )
            }
    }

    // =====================================================
    // CATEGORY CHANGE
    // =====================================================

    categorySpinner.onItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                loadSubCategories(
                    categories[position].lowercase(
                        Locale.getDefault()
                    ).replace(" ", "_")
                )
            }

            override fun onNothingSelected(
                parent: AdapterView<*>?
            ) {
            }
        }

    // =====================================================
    // ADD SUB-CATEGORY
    // =====================================================

    addButton.setOnClickListener {

        val selectedCategory =
            categories[
                categorySpinner.selectedItemPosition
            ]

        val parentId =
            selectedCategory.lowercase(
                Locale.getDefault()
            ).replace(" ", "_")

        showAddSubCategoryDialog(
            parentId
        ) {

            loadSubCategories(parentId)
        }
    }

    dialog.setView(root)

    dialog.setButton(
        AlertDialog.BUTTON_NEGATIVE,
        "Close"
    ) { _, _ ->
        dialog.dismiss()
    }

    dialog.show()

    loadSubCategories("teacher")
}
    // =========================================================
// ADD SUB-CATEGORY
// =========================================================

private fun showAddSubCategoryDialog(
    parentId: String,
    onSaved: () -> Unit
) {

    val box =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
            )
        }

    val nameField =
        EditText(this).apply {

            hint = "Sub-Category Name"

            textSize = 13f

            setSingleLine(true)
        }

    box.addView(
        nameField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val iconField =
        EditText(this).apply {

            hint = "Icon e.g. 🧑‍🏫"

            textSize = 13f

            setSingleLine(true)
        }

    box.addView(
        iconField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val urlField =
        EditText(this).apply {

            hint = "Website / URL"

            textSize = 13f

            setSingleLine(true)

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

    box.addView(
        urlField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val pageTypeLabel =
        TextView(this).apply {

            text = "Page Type"

            textSize = 12f

            setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            setTextColor(
                Color.rgb(7, 89, 133)
            )

            setPadding(
                0,
                dp(4),
                0,
                dp(2)
            )
        }

    box.addView(
        pageTypeLabel
    )

    val pageTypeSpinner =
        Spinner(this)

    val pageTypes =
        arrayOf(
            "Website",
            "App Page",
            "External Link"
        )

    pageTypeSpinner.adapter =
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            pageTypes
        )

    box.addView(
        pageTypeSpinner,
        LinearLayout.LayoutParams(
            -1,
            dp(45)
        )
    )

    val positionField =
        EditText(this).apply {

            hint = "Position"

          setText("1")

            textSize = 13f

            setSingleLine(true)

            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
        }

    box.addView(
        positionField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val enabledSwitch =
        Switch(this).apply {

            text = "Enabled"

            textSize = 12f

            isChecked = true
        }

    box.addView(
        enabledSwitch,
        LinearLayout.LayoutParams(
            -1,
            dp(45)
        )
    )

    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                "➕ Add Sub-Category"
            )
            .setView(box)
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "SAVE",
                null
            )
            .create()

    dialog.setOnShowListener {

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener {

            val name =
                nameField.text
                    .toString()
                    .trim()

            val icon =
                iconField.text
                    .toString()
                    .trim()

            val url =
                urlField.text
                    .toString()
                    .trim()

            val pageType =
                pageTypes[
                    pageTypeSpinner.selectedItemPosition
                ]

            val position =
                positionField.text
                    .toString()
                    .trim()
                    .toLongOrNull()

            if (name.isEmpty()) {

                nameField.error =
                    "Name जरूरी है"

                return@setOnClickListener
            }

            if (position == null) {

                positionField.error =
                    "सही Position डालें"

                return@setOnClickListener
            }
val selectedNoticeImage =
    noticeImageUri
            
            val data =
                hashMapOf<String, Any>(
                    "name" to name,
                    "icon" to icon,
                    "url" to url,
                    "pageType" to pageType,
                    "parentId" to parentId,
                    "position" to position,
"buttonColor" to when (
    position % 6
) {
    1L -> "#4CAF50"
    2L -> "#2196F3"
    3L -> "#9C27B0"
    4L -> "#FF9800"
    5L -> "#009688"
    else -> "#E53935"
},
                    
"enabled" to enabledSwitch.isChecked
                )

            val doc =
                db.collection(
                    "home_subcategories"
                ).document()

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).isEnabled = false

            doc.set(data)
                .addOnSuccessListener {

                    toast(
                        "Sub-Category save हो गई"
                    )

                    dialog.dismiss()

                    onSaved()
                }
                .addOnFailureListener { e ->

                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = true

                    toast(
                        "Save failed: ${
                            e.message
                                ?: "Firestore error"
                        }"
                    )
                }
        }
    }

    dialog.show()
}
// =========================================================
// EDIT SUB-CATEGORY
// =========================================================

private fun showEditSubCategoryDialog(
    documentId: String,
    parentId: String,
    doc: com.google.firebase.firestore.DocumentSnapshot
) {

    val box =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
            )
        }

    val nameField =
        EditText(this).apply {

            hint = "Sub-Category Name"

            textSize = 13f

            setSingleLine(true)

            setText(
                doc.getString("name") ?: ""
            )
        }

    box.addView(
        nameField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val iconField =
        EditText(this).apply {

            hint = "Icon"

            textSize = 13f

            setSingleLine(true)

            setText(
                doc.getString("icon") ?: ""
            )
        }

    box.addView(
        iconField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val urlField =
        EditText(this).apply {

            hint = "Website / URL"

            textSize = 13f

            setSingleLine(true)

            setText(
                doc.getString("url") ?: ""
            )

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

    box.addView(
        urlField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val pageTypeLabel =
        TextView(this).apply {

            text = "Page Type"

            textSize = 12f

            setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            setTextColor(
                Color.rgb(7, 89, 133)
            )

            setPadding(
                0,
                dp(4),
                0,
                dp(2)
            )
        }

    box.addView(
        pageTypeLabel
    )

    val pageTypes =
        arrayOf(
            "Website",
            "App Page",
            "External Link"
        )

    val pageTypeSpinner =
        Spinner(this)

    pageTypeSpinner.adapter =
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            pageTypes
        )

    val oldPageType =
        doc.getString("pageType")
            ?: "Website"

    val oldPageIndex =
        pageTypes.indexOf(oldPageType)

    pageTypeSpinner.setSelection(
        if (oldPageIndex >= 0) {
            oldPageIndex
        } else {
            0
        }
    )

    box.addView(
        pageTypeSpinner,
        LinearLayout.LayoutParams(
            -1,
            dp(45)
        )
    )

    val oldPosition =
        doc.getLong("position")
            ?: 1L

    val positionField =
        EditText(this).apply {

            hint = "Position"

            setText(
    oldPosition.toString()
)

            textSize = 13f

            setSingleLine(true)

            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
        }

    box.addView(
        positionField,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val enabledSwitch =
        Switch(this).apply {

            text = "Enabled"

            textSize = 12f

            isChecked =
                doc.getBoolean("enabled")
                    ?: true
        }

    box.addView(
        enabledSwitch,
        LinearLayout.LayoutParams(
            -1,
            dp(45)
        )
    )

    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                "✏️ Edit Sub-Category"
            )
            .setView(box)
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "SAVE",
                null
            )
            .create()

    dialog.setOnShowListener {

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener {

            val name =
                nameField.text
                    .toString()
                    .trim()

            val icon =
                iconField.text
                    .toString()
                    .trim()

            val url =
                urlField.text
                    .toString()
                    .trim()

            val pageType =
                pageTypes[
                    pageTypeSpinner.selectedItemPosition
                ]

            val position =
                positionField.text
                    .toString()
                    .trim()
                    .toLongOrNull()

            if (name.isEmpty()) {

                nameField.error =
                    "Name जरूरी है"

                return@setOnClickListener
            }

            if (position == null) {

                positionField.error =
                    "सही Position डालें"

                return@setOnClickListener
            }
            
            val data =
                hashMapOf<String, Any>(
                    "name" to name,
                    "icon" to icon,
                    "url" to url,
                    "pageType" to pageType,
                    "parentId" to parentId,
                    "position" to position,
"buttonColor" to when (position % 6L) {
    1L -> "#4CAF50"
    2L -> "#2196F3"
    3L -> "#9C27B0"
    4L -> "#FF9800"
    5L -> "#009688"
    else -> "#E53935"
},
"enabled" to enabledSwitch.isChecked
                )

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).isEnabled = false

            db.collection(
                "home_subcategories"
            )
                .document(documentId)
                .set(
                    data,
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .addOnSuccessListener {

                    toast(
                        "Sub-Category update हो गई"
                    )

                    dialog.dismiss()
                }
                .addOnFailureListener { e ->

                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = true

                    toast(
                        "Update failed: ${
                            e.message
                                ?: "Firestore error"
                        }"
                    )
                }
        }
    }

    dialog.show()
}
// =========================================================
// MOVE SUB-CATEGORY UP / DOWN
// =========================================================

private fun moveSubCategory(
    documentId: String,
    parentId: String,
    newPosition: Long,
    refresh: (String) -> Unit
) {

    db.collection(
        "home_subcategories"
    )
        .whereEqualTo(
            "parentId",
            parentId
        )
        .get()
        .addOnSuccessListener { result ->

            if (result.isEmpty) {
                return@addOnSuccessListener
            }

            val sorted =
                result.documents.sortedBy {

                    it.getLong("position")
                        ?: 999999L
                }

            val currentIndex =
                sorted.indexOfFirst {

                    it.id == documentId
                }

            if (currentIndex < 0) {
                return@addOnSuccessListener
            }

            val currentPosition =
                sorted[currentIndex]
                    .getLong("position")
                    ?: 0L

            val targetIndex =
                when {

                    newPosition < currentPosition ->
                        currentIndex - 1

                    newPosition > currentPosition ->
                        currentIndex + 1

                    else ->
                        currentIndex
                }

            if (
                targetIndex < 0 ||
                targetIndex >= sorted.size
            ) {

                toast(
                    if (targetIndex < 0) {
                        "यह पहले से सबसे ऊपर है"
                    } else {
                        "यह पहले से सबसे नीचे है"
                    }
                )

                return@addOnSuccessListener
            }

            val targetDoc =
                sorted[targetIndex]

            val targetPosition =
                targetDoc.getLong("position")
                    ?: currentPosition

            val batch =
                db.batch()

            batch.update(
                db.collection(
                    "home_subcategories"
                ).document(documentId),
                "position",
                targetPosition
            )

            batch.update(
                db.collection(
                    "home_subcategories"
                ).document(targetDoc.id),
                "position",
                currentPosition
            )

            batch.commit()
                .addOnSuccessListener {

                    toast(
                        "Position बदल गई"
                    )

                    refresh(parentId)
                }
                .addOnFailureListener { e ->

                    toast(
                        "Position बदलने में समस्या: ${
                            e.message
                                ?: "Firestore error"
                        }"
                    )
                }
        }
        .addOnFailureListener { e ->

            toast(
                "Sub-Categories पढ़ने में समस्या: ${
                    e.message
                        ?: "Firestore error"
                }"
            )
        }
}

    // =========================================================
    // COMMON POST CREATOR
    // =========================================================
private fun showCommonPostCreator(
    initialCategory: String? = null,
    existingPost: CategoryPost? = null
) {

        val categories = listOf(
            "नोटिस और अपडेट" to "notice",
            "महत्वपूर्ण जानकारी" to "important_information",
            "अध्ययन सामग्री" to "study_material",
            "करियर गाइड और उपयोगी टूल्स" to "career_guide"
        )

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(box)
        }

    

        box.addView(adminSectionTitle("📂", "पोस्ट कहाँ प्रकाशित करें?"))


val publishToChannel = CheckBox(this).apply {
    text = "📱 शिक्षा रोजगार चैनल"
    isChecked = false
    isEnabled = existingPost == null
    textSize = 14f
}

box.addView(publishToChannel)

val categoryChecks = categories.map { (label, key) ->
    CheckBox(this).apply {
        text = label
        tag = key
        isChecked = (existingPost?.category ?: initialCategory) == key
        isEnabled = existingPost == null
        textSize = 14f
    }.also { box.addView(it) }
}


box.addView(
    adminSectionNote(
        "एक या एक से अधिक विकल्प चुनें। टिक किए गए प्रत्येक स्थान पर पोस्ट अलग से सेव होगी।"
    )
)

        
        val titleField = EditText(this).apply {
            hint = "पोस्ट का शीर्षक"
            setSingleLine(false)
            minLines = 1
            setText(existingPost?.title.orEmpty())
        }
        box.addView(titleField)

        val bodyField = EditText(this).apply {
            hint = "पोस्ट का मुख्य टेक्स्ट"
            minLines = 4
            gravity = Gravity.TOP
            setText(existingPost?.body.orEmpty())
        }
        box.addView(bodyField)


        
val formatRow = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun applyBodyFormat(prefix: String, suffix: String) {
    val start = bodyField.selectionStart.coerceAtLeast(0)
    val end = bodyField.selectionEnd.coerceAtLeast(0)
    val from = minOf(start, end)
    val to = maxOf(start, end)

    if (from != to) {
        val selectedText = bodyField.text
            .substring(from, to)
        bodyField.text.replace(
            from,
            to,
            "$prefix$selectedText$suffix"
        )
        bodyField.setSelection(
            from,
            from + prefix.length + selectedText.length + suffix.length
        )
    } else {
        bodyField.text.insert(start, "$prefix$suffix")
        bodyField.setSelection(start + prefix.length)
    }
}

formatRow.addView(Button(this).apply {
    text = "B बोल्ड"
    setOnClickListener {
        applyBodyFormat("*", "*")
    }
})

formatRow.addView(Button(this).apply {
    text = "> हल्का"
    setOnClickListener {
        applyBodyFormat("> ", "")
    }
})

formatRow.addView(Button(this).apply {
    text = "` हाइलाइट"
    setOnClickListener {
        applyBodyFormat("`", "`")
    }
})

box.addView(formatRow)

                val descriptionField = EditText(this).apply {
            hint = "अतिरिक्त विवरण (वैकल्पिक)"
            minLines = 2
            gravity = Gravity.TOP
                                setText(existingPost?.description.orEmpty())
        }
        box.addView(descriptionField)

        val websiteUrlField = EditText(this).apply {
            hint = "🌐 वेबसाइट लिंक (वैकल्पिक)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
                        setText(existingPost?.websiteUrl.orEmpty())
        }
        box.addView(websiteUrlField)
        
        val existingButton = existingPost?.buttons?.firstOrNull()

        val customButtonLabelField = EditText(this).apply {
            hint = "🔘 कस्टम बटन का नाम (वैकल्पिक)"
            setSingleLine(true)
            setText(existingButton?.label.orEmpty())
        }
        box.addView(customButtonLabelField)

        val customButtonUrlField = EditText(this).apply {
            hint = "🔗 कस्टम बटन का URL (वैकल्पिक)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(existingButton?.url.orEmpty())
        }
        box.addView(customButtonUrlField)

        val imageButton = Button(this).apply {
            text = "🖼️ फोटो चुनें"
            setOnClickListener {
                pickCommonPostImage.launch("image/*")
            }
        }
        box.addView(imageButton)
commonPostImagePreview = ImageView(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        -1,
        dp(220)
    ).apply {
        topMargin = dp(8)
        bottomMargin = dp(8)
    }
    scaleType = ImageView.ScaleType.FIT_CENTER
    adjustViewBounds = true
    visibility = View.GONE
}
box.addView(commonPostImagePreview)

        
        existingPost?.imageUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { savedImageUrl ->
                commonPostImagePreview?.visibility = View.VISIBLE

                ChannelRepository.loadImage(savedImageUrl) { bitmap, _ ->
                    runOnUiThread {
                        if (bitmap != null) {
                            commonPostImagePreview?.setImageBitmap(bitmap)
                        }
                    }
                }
            }

            
        val fileButton = Button(this).apply {
            text = "📄 PDF / File चुनें"
            setOnClickListener {
                pickCommonPostFile.launch("*/*")
            }
        }
        box.addView(fileButton)





        AlertDialog.Builder(this)
            .setTitle("📝 Common Post Creator")
            .setView(scroll)
            .setNegativeButton("रद्द करें", null)
            .setPositiveButton("पोस्ट सेव करें", null)
            .create()
            .also { dialog ->

                dialog.setOnShowListener {

                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val title = titleField.text
                            .toString()
                            .trim()

                        val body = bodyField.text
                            .toString()
                            .trim()

                        val description = descriptionField.text
                            .toString()
                            .trim()
val websiteUrl = websiteUrlField.text.toString().trim()
                        val customButtonLabel = customButtonLabelField.text
    .toString()
    .trim()


val customButtonUrl = customButtonUrlField.text
    .toString()
    .trim()

                        if (title.isBlank() && body.isBlank()) {
                            toast("शीर्षक या मुख्य टेक्स्ट भरें")
                            return@setOnClickListener
                        }

                        val selectedCategories = categoryChecks
    .filter { it.isChecked }
    .map { it.tag as String }

if (!publishToChannel.isChecked && selectedCategories.isEmpty()) {
    toast("कम से कम एक कैटेगरी या Channel चुनें")
    return@setOnClickListener
}
                         
                        dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled = false

                        val selectedImage = commonPostImageUri
                        val selectedFile = commonPostFileUri

                        
fun savePostWithMedia(
    imageUrl: String = "",
    fileUrl: String = ""
) {
    val fileMime = selectedFile
        ?.let { contentResolver.getType(it) }
        ?: "application/pdf"

    val fileName = selectedFile?.let { uri ->
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0).orEmpty()
            } else {
                ""
            }
        }.orEmpty()
    }.orEmpty()

    fun finishPublishing() {
        if (!publishToChannel.isChecked) {
            commonPostImageUri = null
            commonPostFileUri = null
            dialog.dismiss()
            toast("चुनी गई कैटेगरी में पोस्ट प्रकाशित हो गई")
            return
        }

        val channelData = hashMapOf<String, Any>(
            "title" to title,
            "body" to body,
            "description" to description,
            "websiteUrl" to websiteUrl,
            "buttons" to if (
    customButtonLabel.isNotBlank() &&
    customButtonUrl.isNotBlank()
) {
    listOf(
        mapOf(
            "label" to customButtonLabel,
            "url" to customButtonUrl,
            "backgroundColor" to "#1976D2",
            "textColor" to "#FFFFFF",
            "enabled" to true
        )
    )
} else {
    emptyList<Map<String, Any>>()
},
            "published" to true,
            "postType" to if (
                imageUrl.isNotBlank() || fileUrl.isNotBlank()
            ) "media" else "text",
            "category" to "शिक्षा रोजगार चैनल",
            "imageUrl" to imageUrl,
            "fileUrl" to fileUrl,
            "fileName" to fileName,
            "imageMime" to (
                selectedImage?.let {
                    contentResolver.getType(it)
                } ?: "image/jpeg"
            ),
            "fileMime" to fileMime,
            "createdAt" to System.currentTimeMillis(),
            "commentsEnabled" to true,
            "likeCount" to 0L,
            "commentCount" to 0L,
            "viewCount" to 0L,
            "shareCount" to 0L
        )

        ChannelRepository.createPost(channelData) {
            channelSuccess, channelMessage ->
            runOnUiThread {
                if (channelSuccess) {
                    commonPostImageUri = null
                    commonPostFileUri = null
                    dialog.dismiss()
                    toast("चुनी गई कैटेगरी और Channel पर पोस्ट प्रकाशित हो गई")
                } else {
                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = true
                    toast(
                        "कैटेगरी में पोस्ट सेव हुई, लेकिन Channel पर नहीं: ${
                            channelMessage ?: "Channel error"
                        }"
                    )
                }
            }
        }
    }

    fun saveCategoryAt(index: Int) {
        if (index >= selectedCategories.size) {
            finishPublishing()
            return
        }

        val category = selectedCategories[index]

        CategoryPostRepository.savePost(
            
            CategoryPost(
                id = existingPost?.id.orEmpty(),
                category = category,
                title = title,
                body = body,
                description = description,
                websiteUrl = websiteUrl,
                createdAt = existingPost?.createdAt ?: 0L,
                buttons = if (
                    customButtonLabel.isNotBlank() &&
                    customButtonUrl.isNotBlank()
                ) {
                    listOf(
                        CategoryPostButton(
                            label = customButtonLabel,
                            url = customButtonUrl,
                            backgroundColor = existingButton?.backgroundColor
                                ?: "#1976D2",
                            textColor = existingButton?.textColor
                                ?: "#FFFFFF",
                            enabled = existingButton?.enabled ?: true
                        )
                    )
                } else {
                    emptyList()
                },
                imageUrl = imageUrl.ifBlank {
                    existingPost?.imageUrl.orEmpty()
                },
                imageMime = selectedImage
                    ?.let { contentResolver.getType(it) }
                    ?: existingPost?.imageMime
                    ?: "image/jpeg",
                fileUrl = fileUrl.ifBlank {
                    existingPost?.fileUrl.orEmpty()
                },
                fileName = fileName.ifBlank {
                    existingPost?.fileName.orEmpty()
                },
                fileMime = if (selectedFile != null) {
                    fileMime
                } else {
                    existingPost?.fileMime ?: fileMime
                },
                pinned = existingPost?.pinned ?: false,
                pinOrder = existingPost?.pinOrder ?: 0L,
                enabled = existingPost?.enabled ?: true
            )

        ) { success, message ->
            runOnUiThread {
                if (success) {
                    saveCategoryAt(index + 1)
                } else {
                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = true
                    toast(
                        "कैटेगरी में पोस्ट सेव नहीं हुई: ${
                            message ?: "Firestore error"
                        }"
                    )
                }
            }
        }
    }

    saveCategoryAt(0)
}


                        fun uploadSelectedFile(
                            imageUrl: String
                        ) {
                            if (selectedFile == null) {
                                savePostWithMedia(imageUrl)
                                return
                            }

                            ChannelRepository.upload(
                                selectedFile,
                                "category_documents"
                            ) { uploadedUrl, error ->
                                if (uploadedUrl != null) {
                                    savePostWithMedia(
                                        imageUrl,
                                        uploadedUrl
                                    )
                                } else {
                                    runOnUiThread {
                                        dialog.getButton(
                                            AlertDialog.BUTTON_POSITIVE
                                        ).isEnabled = true
                                        toast(
                                            "PDF/File अपलोड नहीं हुई: ${
                                                error ?: "Upload error"
                                            }"
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedImage != null) {
                            ChannelRepository.upload(
                                selectedImage,
                                "category_images"
                            ) { uploadedUrl, error ->
                                if (uploadedUrl != null) {
                                    uploadSelectedFile(uploadedUrl)
                                } else {
                                    runOnUiThread {
                                        dialog.getButton(
                                            AlertDialog.BUTTON_POSITIVE
                                        ).isEnabled = true
                                        toast(
                                            "फोटो अपलोड नहीं हुई: ${
                                                error ?: "Upload error"
                                            }"
                                        )
                                    }
                                }
                            }
                        } else {
                            uploadSelectedFile("")
                        }
           
                                
                            
                        
                    }
                }

                dialog.show()
            }
    }

private fun showCategoryPostManager(
    category: String,
    categoryLabel: String,
    deleteOnly: Boolean = false
) {
    val listBox = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }

    val scroll = ScrollView(this).apply {
        isFillViewport = true
        addView(listBox)
    }

        val dialog = AlertDialog.Builder(this)
        .setTitle(
            if (deleteOnly) {
                "🗑️ $categoryLabel — पोस्ट डिलीट करें"
            } else {
                "📋 $categoryLabel — पोस्ट मैनेजमेंट"
            }
        )
        .setView(scroll)
        .setNegativeButton("बंद करें", null)
        .create()

    var registration:
        com.google.firebase.firestore.ListenerRegistration? = null

    dialog.setOnDismissListener {
        registration?.remove()
        registration = null
    }

    listBox.addView(
        TextView(this).apply {
            text = "पोस्ट लोड हो रही हैं…"
            textSize = 15f
            setPadding(0, dp(8), 0, dp(8))
        }
    )

    dialog.show()

    registration = CategoryPostRepository.observePosts(
        category,
        { posts ->
            runOnUiThread {
                if (!dialog.isShowing) return@runOnUiThread

                listBox.removeAllViews()

                if (posts.isEmpty()) {
                    listBox.addView(
                        TextView(this).apply {
                            text = "इस कैटेगरी में अभी कोई पोस्ट नहीं है।"
                            textSize = 15f
                            setPadding(0, dp(12), 0, dp(12))
                        }
                    )
                    return@runOnUiThread
                }

                listBox.addView(
                    TextView(this).apply {
                        text = "कुल पोस्ट: ${posts.size}"
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                    }
                )

                posts.forEach { post ->
                    val itemBox = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(
                            dp(8), dp(10), dp(8), dp(10)
                        )
                        setBackgroundColor(
                            Color.rgb(245, 248, 250)
                        )
                    }

                    itemBox.addView(
                        TextView(this).apply {
                            text = post.title.ifBlank {
                                "बिना शीर्षक वाली पोस्ट"
                            }
                            textSize = 16f
                            setTypeface(null, Typeface.BOLD)
                        }
                    )

                    if (post.body.isNotBlank()) {
                        itemBox.addView(
                            TextView(this).apply {
                                text = post.body
                                textSize = 14f
                                maxLines = 5
                            }
                        )
                    }

                    
                    if (post.imageUrl.isNotBlank()) {
                        val postImagePreview = ImageView(this).apply {
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setBackgroundColor(Color.WHITE)
                        }

                        itemBox.addView(
                            postImagePreview,
                            LinearLayout.LayoutParams(
                                -1,
                                dp(200)
                            ).apply {
                                topMargin = dp(8)
                                bottomMargin = dp(8)
                            }
                        )

                        ChannelRepository.loadImage(
                            post.imageUrl
                        ) { bitmap, _ ->
                            runOnUiThread {
                                if (bitmap != null) {
                                    postImagePreview.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }


                    if (post.fileUrl.isNotBlank()) {
                        itemBox.addView(
                            TextView(this).apply {
                                text = "📄 ${post.fileName.ifBlank { "PDF / File उपलब्ध है" }}"
                                textSize = 13f
                            }
                        )
                    }
                    
                                        if (!deleteOnly) {
                        val editButton = Button(this).apply {
                            text = "✏️ पोस्ट एडिट करें"
                            setOnClickListener {
                                dialog.dismiss()
                                showCommonPostCreator(
                                    initialCategory = category,
                                    existingPost = post
                                )
                            }
                        }

                        itemBox.addView(editButton)
                    }


                    val deleteButton = Button(this).apply {
                        text = "🗑️ पोस्ट डिलीट करें"
                        setOnClickListener {
                            AlertDialog.Builder(this@AdminActivity)
                                .setTitle("पोस्ट डिलीट करें?")
                                .setMessage(
                                    "“${post.title}” पोस्ट और उससे जुड़ी मीडिया हटाने की पुष्टि करें।"
                                )
                                .setNegativeButton("रद्द करें", null)
                                .setPositiveButton("डिलीट करें") { _, _ ->
                                    CategoryPostRepository.deletePost(
                                        post.id
                                    ) { success, error ->
                                        runOnUiThread {
                                            if (success) {
                                                toast("पोस्ट और उसकी मीडिया हटाने का अनुरोध सफल हुआ")
                                            } else {
                                                toast(
                                                    "पोस्ट डिलीट नहीं हुई: ${
                                                        error ?: "त्रुटि"
                                                    }"
                                                )
                                            }
                                        }
                                    }
                                }
                                .show()
                        }
                    }

                    itemBox.addView(deleteButton)

                    listBox.addView(
                        itemBox,
                        LinearLayout.LayoutParams(
                            -1,
                            -2
                        ).apply {
                            topMargin = dp(6)
                            bottomMargin = dp(6)
                        }
                    )
                }
            }
        },
        { error ->
            runOnUiThread {
                if (dialog.isShowing) {
                    listBox.removeAllViews()
                    listBox.addView(
                        TextView(this).apply {
                            text = "पोस्ट लोड नहीं हुई: ${error.message}"
                            textSize = 14f
                        }
                    )
                }
            }
        }
    )
}

// =========================================================
// NOTICE MANAGER
// =========================================================

private fun showNoticeManager() {

    val box =
        LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(6),
                dp(4),
                dp(6),
                dp(6)
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

    box.addView(
        scroll,
        LinearLayout.LayoutParams(
            -1,
            dp(430)
        )
    )

    val addButton =
        Button(this).apply {

            text =
                "➕ ADD NEW NOTICE"

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
                    76,
                    175,
                    80
                )
            )

            minHeight = 0

            setOnClickListener {

                showNoticeEditDialog(
                    null
                )
            }
        }

    box.addView(
        addButton,
        LinearLayout.LayoutParams(
            -1,
            dp(44)
        ).apply {

            topMargin = dp(5)
        }
    )

    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                "📢 Manage Notices"
            )
            .setView(box)
            .setPositiveButton(
                "CLOSE",
                null
            )
            .create()

    dialog.show()

    db.collection(
        "home_notices"
    )
        .get()
        .addOnSuccessListener { snapshot ->

            list.removeAllViews()

            val docs =
                snapshot.documents.sortedByDescending {

                    it.getLong(
                        "createdAt"
                    ) ?: 0L
                }

            if (docs.isEmpty()) {

                list.addView(
                    TextView(this).apply {

                        text =
                            "अभी कोई Notice नहीं है।"

                        textSize = 14f

                        gravity =
                            Gravity.CENTER

                        setPadding(
                            0,
                            dp(25),
                            0,
                            dp(25)
                        )
                    }
                )

                return@addOnSuccessListener
            }

            docs.forEach { doc ->

                val title =
                    doc.getString(
                        "title"
                    ).orEmpty()

                val enabled =
                    doc.getBoolean(
                        "enabled"
                    ) ?: true

                val startAt =
                    doc.getLong(
                        "startAt"
                    ) ?: 0L

                val endAt =
                    doc.getLong(
                        "endAt"
                    ) ?: 0L

                val card =
                    LinearLayout(this).apply {

                        orientation =
                            LinearLayout.VERTICAL

                        setPadding(
                            dp(8),
                            dp(6),
                            dp(8),
                            dp(6)
                        )

                        setBackgroundColor(
                            Color.rgb(
                                245,
                                248,
                                250
                            )
                        )
                    }

                val info =
                    TextView(this).apply {

                        text =
                            "📢 $title\n" +
                            "Enabled: " +
                            if (enabled) {
                                "YES"
                            } else {
                                "NO"
                            } +
                            "\nStart: $startAt" +
                            "\nEnd: $endAt"

                        textSize = 12f

                        setTextColor(
                            Color.DKGRAY
                        )
                    }

                card.addView(
                    info,
                    LinearLayout.LayoutParams(
                        -1,
                        -2
                    )
                )

                val edit =
                    Button(this).apply {

                        text =
                            "✏️ EDIT"

                        textSize = 10f

                        minHeight = 0

                        setOnClickListener {

                            showNoticeEditDialog(
                                doc
                            )
                        }
                    }

                card.addView(
                    edit,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(36)
                    )
                )

                val delete =
                    Button(this).apply {

                        text =
                            "🗑 DELETE"

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

                        setOnClickListener {

                            AlertDialog.Builder(
                                this@AdminActivity
                            )
                                .setTitle(
                                    "Delete Notice?"
                                )
                                .setMessage(
                                    title
                                )
                                .setNegativeButton(
                                    "CANCEL",
                                    null
                                )
                                .setPositiveButton(
                                    "DELETE"
                                ) { _, _ ->

                                    db.collection(
                                        "home_notices"
                                    )
                                        .document(
                                            doc.id
                                        )
                                        .delete()
                                        .addOnSuccessListener {

                                            toast(
                                                "Notice deleted"
                                            )

                                            showNoticeManager()
                                        }
                                        .addOnFailureListener { e ->

                                            toast(
                                                e.message
                                                    ?: "Delete failed"
                                            )
                                        }
                                }
                                .show()
                        }
                    }

                card.addView(
                    delete,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(36)
                    )
                )

                list.addView(
                    card,
                    LinearLayout.LayoutParams(
                        -1,
                        -2
                    ).apply {

                        bottomMargin =
                            dp(6)
                    }
                )
            }
        }
        .addOnFailureListener { e ->

            toast(
                "Notice पढ़ने में समस्या: ${
                    e.message
                        ?: "Firestore error"
                }"
            )
        }
}
// =========================================================
// ADD / EDIT NOTICE
// =========================================================

private fun showNoticeEditDialog(
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

    val titleField =
        EditText(this).apply {

            hint =
                "Notice Title"

            textSize = 14f

            setSingleLine(false)

            setText(
                existing?.getString(
                    "title"
                ) ?: ""
            )
        }

    box.addView(
        titleField,
        LinearLayout.LayoutParams(
            -1,
            dp(55)
        )
    )

    val bodyField =
        EditText(this).apply {

            hint =
                "पूरा Notice / Update Text"

            textSize = 14f

            gravity =
                Gravity.TOP

            minLines = 6

            setSingleLine(false)

            setText(
                existing?.getString(
                    "body"
                ) ?: ""
            )

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

    box.addView(
        bodyField,
        LinearLayout.LayoutParams(
            -1,
            dp(150)
        )
    )
// =========================================================
// NOTICE PHOTO
// =========================================================

val noticePhotoButton =
    Button(this).apply {

        text = "📷 NOTICE PHOTO चुनें"

        textSize = 12f

        setOnClickListener {

            pickNoticeImage.launch(
                "image/*"
            )
        }
    }

box.addView(
    noticePhotoButton,
    LinearLayout.LayoutParams(
        -1,
        dp(44)
    ).apply {

        topMargin = dp(4)
        bottomMargin = dp(4)
    }
)
// =========================================================
// HOME FLOATING PHOTO
// =========================================================

val floatingPhotoSwitch =
    Switch(this).apply {

        text = "🏠 Home Page पर Floating Photo दिखाएँ"

        textSize = 13f

        isChecked =
            existing?.getBoolean(
                "floatingPhoto"
            ) ?: false
    }

box.addView(
    floatingPhotoSwitch,
    LinearLayout.LayoutParams(
        -1,
        dp(50)
    )
)
    val linkLabelField =
        EditText(this).apply {

            hint =
                "Link का नाम — जैसे Official Website"

            textSize = 13f

            setText(
                existing?.getString(
                    "linkLabel"
                ) ?: ""
            )
        }

    box.addView(
        linkLabelField,
        LinearLayout.LayoutParams(
            -1,
            dp(50)
        )
    )

    val linkUrlField =
        EditText(this).apply {

            hint =
                "Clickable URL — https://..."

            textSize = 13f

            setText(
                existing?.getString(
                    "linkUrl"
                ) ?: ""
            )

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

    box.addView(
        linkUrlField,
        LinearLayout.LayoutParams(
            -1,
            dp(50)
        )
    )

    val startField =
        EditText(this).apply {

            hint =
                "Start Time — खाली = तुरंत"

            textSize = 13f

            setText(
                (
                    existing?.getLong(
                        "startAt"
                    ) ?: 0L
                ).toString()
            )

            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
        }

    box.addView(
        startField,
        LinearLayout.LayoutParams(
            -1,
            dp(50)
        )
    )

    val endField =
        EditText(this).apply {

            hint =
                "End Time — खाली/0 = हमेशा"

            textSize = 13f

            setText(
                (
                    existing?.getLong(
                        "endAt"
                    ) ?: 0L
                ).toString()
            )

            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
        }

    box.addView(
        endField,
        LinearLayout.LayoutParams(
            -1,
            dp(50)
        )
    )

    val enabledSwitch =
        Switch(this).apply {

            text =
                "🏠 Home पर Notice दिखाएँ"

            textSize = 13f

            isChecked =
                existing?.getBoolean(
                    "enabled"
                ) ?: true
        }

    box.addView(
        enabledSwitch,
        LinearLayout.LayoutParams(
            -1,
            dp(48)
        )
    )

    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                if (existing == null) {
                    "➕ Add Notice"
                } else {
                    "✏️ Edit Notice"
                }
            )
            .setView(box)
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "SAVE",
                null
            )
            .create()

    dialog.setOnShowListener {

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener {

            val title =
                titleField.text
                    .toString()
                    .trim()

            val body =
                bodyField.text
                    .toString()
                    .trim()

            val linkLabel =
                linkLabelField.text
                    .toString()
                    .trim()

            val linkUrl =
                linkUrlField.text
                    .toString()
                    .trim()

            val startAt =
                startField.text
                    .toString()
                    .trim()
                    .toLongOrNull()
                    ?: 0L

            val endAt =
                endField.text
                    .toString()
                    .trim()
                    .toLongOrNull()
                    ?: 0L

            if (title.isBlank()) {

                titleField.error =
                    "Title जरूरी है"

                return@setOnClickListener
            }

            if (body.isBlank()) {

                bodyField.error =
                    "Notice का पूरा Text लिखें"

                return@setOnClickListener
            }

            if (
                linkUrl.isNotBlank() &&
                !linkUrl.startsWith(
                    "http://"
                ) &&
                !linkUrl.startsWith(
                    "https://"
                )
            ) {

                linkUrlField.error =
                    "URL https:// से शुरू करें"

                return@setOnClickListener
            }

            if (
                endAt > 0L &&
                startAt > 0L &&
                endAt < startAt
            ) {

                endField.error =
                    "End Time, Start Time से बाद होना चाहिए"

                return@setOnClickListener
            }
val selectedNoticeImage =
    noticeImageUri

val existingImageUrl =
    existing?.getString(
        "imageUrl"
    ).orEmpty()

dialog.getButton(
    AlertDialog.BUTTON_POSITIVE
).isEnabled = false

fun saveNotice(
    imageUrl: String?
) {

    val data =
        hashMapOf<String, Any>(

            "title" to title,

            "body" to body,

            "linkLabel" to linkLabel,

            "linkUrl" to linkUrl,

            "startAt" to startAt,

            "endAt" to endAt,

            "enabled" to
                enabledSwitch.isChecked,
            "floatingPhoto" to
    floatingPhotoSwitch.isChecked,

            "createdAt" to (
                existing?.getLong(
                    "createdAt"
                ) ?: System.currentTimeMillis()
            )
        )

    if (!imageUrl.isNullOrBlank()) {

        data["imageUrl"] =
            imageUrl
    }

    val task =
        if (existing == null) {

            db.collection(
                "home_notices"
            )
                .add(data)

        } else {

            db.collection(
                "home_notices"
            )
                .document(
                    existing.id
                )
                .set(data)
        }

    task.addOnSuccessListener {

        noticeImageUri = null

        toast(
            if (existing == null) {
                "Notice Added"
            } else {
                "Notice Updated"
            }
        )

        dialog.dismiss()
    }
        .addOnFailureListener { e ->

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).isEnabled = true

            toast(
                "Notice save नहीं हुआ: ${
                    e.message
                        ?: "Firestore error"
                }"
            )
        }
}

if (selectedNoticeImage != null) {

    ChannelRepository.upload(
        selectedNoticeImage,
        "images"
    ) { imageUrl, error ->

        if (!imageUrl.isNullOrBlank()) {

            saveNotice(
                imageUrl
            )

        } else {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).isEnabled = true

            toast(
                error
                    ?: "Notice photo upload failed"
            )
        }
    }

} else {

    saveNotice(
        if (existingImageUrl.isNotBlank()) {
            existingImageUrl
        } else {
            null
        }
    )
}

                    
        }
    }

    dialog.show()
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
    Color.rgb(76, 175, 80)
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
                setOnClickListener {

    showHomeSubCategoryManager(
        doc.id
    )
                }
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

    val dialog = AlertDialog.Builder(this)
        .setTitle("💾 Shiksha Rojgar Storage")
        .setMessage("Storage की जानकारी पढ़ी जा रही है…")
        .setPositiveButton("OK", null)
        .create()

    dialog.show()

    val mediaTask = db.collection("channel_media").get()
    val channelTask = db.collection("channel_posts").get()
    val categoryTask = db.collection("category_posts").get()
    val groupTask = db.collection("group_posts").get()
    val statsTask = db.collection("media_storage_stats")
        .document("summary")
        .get()

    Tasks.whenAll(
        mediaTask,
        channelTask,
        categoryTask,
        groupTask,
        statsTask
    )
        .addOnSuccessListener {

            val mediaSnapshot = mediaTask.result
            val channelSnapshot = channelTask.result
            val categorySnapshot = categoryTask.result
            val groupSnapshot = groupTask.result
            val statsSnapshot = statsTask.result

            val mediaByCategory =
                mutableMapOf<String, MutableSet<String>>()

            val categoryPostCounts =
                mutableMapOf<String, Int>()

            val mediaReferences =
                mutableMapOf<String, MutableSet<String>>()

            fun registerPostMedia(
                post: com.google.firebase.firestore.DocumentSnapshot,
                defaultCategory: String
            ) {
                val category = (
                    post.getString("category")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: defaultCategory
                    )

                categoryPostCounts[category] =
                    (categoryPostCounts[category] ?: 0) + 1

                listOf("imageUrl", "fileUrl").forEach { field ->

                    val url = post.getString(field).orEmpty()

                    if (url.startsWith("firestore-media://")) {

                        val mediaId =
                            url.removePrefix("firestore-media://")

                        if (mediaId.isNotBlank()) {

                            mediaReferences
                                .getOrPut(mediaId) {
                                    mutableSetOf()
                                }
                                .add(category)
                        }
                    }
                }
            }

            channelSnapshot.documents.forEach {
                registerPostMedia(it, "Channel")
            }

            categorySnapshot.documents.forEach {
                registerPostMedia(it, "अन्य श्रेणियाँ")
            }

            groupSnapshot.documents.forEach {
                registerPostMedia(it, "शिक्षा रोजगार ग्रुप")
            }

            var totalBytes = 0L
            var imageCount = 0
            var videoCount = 0
            var pdfCount = 0
            var otherFileCount = 0
            var sharedMediaCount = 0

            mediaSnapshot.documents.forEach { media ->

                val size = (media.getLong("size") ?: 0L)
                    .coerceAtLeast(0L)

                totalBytes += size

                val mime = media.getString("mimeType")
                    .orEmpty()
                    .lowercase(Locale.ROOT)

                val fileName = media.getString("fileName")
                    .orEmpty()
                    .lowercase(Locale.ROOT)

                when {
                    mime.startsWith("image/") ||
                        media.getString("folder") == "images" -> {
                        imageCount++
                    }

                    mime.startsWith("video/") -> {
                        videoCount++
                    }

                    mime == "application/pdf" ||
                        fileName.endsWith(".pdf") -> {
                        pdfCount++
                    }

                    else -> {
                        otherFileCount++
                    }
                }

                val labels = mediaReferences[media.id]
                    ?: mutableSetOf("अन्य / बिना संदर्भ")

                if (labels.size > 1) {
                    sharedMediaCount++
                }

                val assignedCategory = labels.sorted().first()

                mediaByCategory
                    .getOrPut(assignedCategory) {
                        mutableSetOf()
                    }
                    .add(media.id)
            }

            val mediaSizes = mediaSnapshot.documents.associate {
                it.id to ((it.getLong("size") ?: 0L).coerceAtLeast(0L))
            }

            val totalFiles = mediaSnapshot.size()

            val deletedBytes =
                (statsSnapshot.getLong("deletedBytes") ?: 0L)
                    .coerceAtLeast(0L)

            val deletedFiles =
                (statsSnapshot.getLong("deletedFiles") ?: 0L)
                    .coerceAtLeast(0L)

            val limitBytes = 1024L * 1024L * 1024L

            val remainingBytes =
                (limitBytes - totalBytes).coerceAtLeast(0L)

            val usagePercent =
                totalBytes.toDouble() / limitBytes.toDouble() * 100.0

            fun formatBytes(bytes: Long): String {
                val mb = bytes.toDouble() / (1024.0 * 1024.0)

                return if (mb >= 1024.0) {
                    String.format(
                        Locale.US,
                        "%.3f GB",
                        mb / 1024.0
                    )
                } else {
                    String.format(
                        Locale.US,
                        "%.2f MB",
                        mb
                    )
                }
            }

            val categoryLines = mutableListOf<String>()

            val allCategories =
                (categoryPostCounts.keys + mediaByCategory.keys)
                    .toSortedSet()

            allCategories.forEach { category ->

                val ids = mediaByCategory[category]
                    ?: emptySet<String>()

                val categoryBytes = ids.sumOf {
                    mediaSizes[it] ?: 0L
                }

                val postCount =
                    categoryPostCounts[category] ?: 0

                categoryLines.add(
                    "• $category\n" +
                        "  पोस्ट: $postCount | मीडिया: ${ids.size} फाइलें\n" +
                        "  मीडिया आकार: ${formatBytes(categoryBytes)}"
                )
            }

            val categoryDetails =
                if (categoryLines.isEmpty()) {
                    "अभी किसी श्रेणी में मीडिया नहीं मिला।"
                } else {
                    categoryLines.joinToString("\n\n")
                }

            val message = """
                💾 SHIKSHA ROJGAR STORAGE

                📦 वर्तमान अपलोडेड मीडिया
                कुल फाइलें: $totalFiles
                कुल आकार: ${formatBytes(totalBytes)}

                🖼️ फोटो: $imageCount
                🎬 वीडियो: $videoCount
                📕 PDF: $pdfCount
                📎 अन्य फाइलें: $otherFileCount

                📊 1 GiB के हिसाब से अनुमानित उपयोग
                इस्तेमाल: ${String.format(Locale.US, "%.2f%%", usagePercent)}
                शेष अनुमानित जगह: ${formatBytes(remainingBytes)}

                🗑️ दर्ज किया गया डिलीट हिसाब
                हटाई गई मीडिया फाइलें: $deletedFiles
                हटाई गई मीडिया का आकार: ${formatBytes(deletedBytes)}

                🔗 एक से अधिक पोस्ट में लगी मीडिया:
                $sharedMediaCount फाइलें

                📂 श्रेणी के अनुसार विवरण

                $categoryDetails

                ℹ️ जरूरी जानकारी:
                श्रेणी का आकार मीडिया फाइलों के आधार पर
                दिखाया जाता है। एक ही मीडिया कई पोस्ट में
                इस्तेमाल हो तो उसका आकार केवल एक श्रेणी
                में दिखाया जाता है, ताकि श्रेणियों का
                योग कुल आकार से अधिक न हो।

                टेक्स्ट और बाहरी लिंक का अपना Firestore
                मीडिया फाइल आकार नहीं होता।

                यह ऐप में दर्ज मीडिया फाइलों के आकार का
                अनुमान है, Firebase Console का वास्तविक
                कुल Firestore उपयोग नहीं।

                1 GiB यहाँ तुलना के लिए रखा गया है।
                Firebase Spark की वास्तविक सीमाएँ और
                अन्य Firestore डेटा/इंडेक्स का उपयोग
                इससे अलग हो सकता है।
            """.trimIndent()

            dialog.setMessage(message)
        }
        .addOnFailureListener { error ->

    val failures = mutableListOf<String>()

    if (!mediaTask.isSuccessful) {
        failures.add(
            "Media: ${mediaTask.exception?.message ?: "अज्ञात त्रुटि"}"
        )
    }

    if (!channelTask.isSuccessful) {
        failures.add(
            "Channel: ${channelTask.exception?.message ?: "अज्ञात त्रुटि"}"
        )
    }

    if (!categoryTask.isSuccessful) {
        failures.add(
            "Category Posts: ${categoryTask.exception?.message ?: "अज्ञात त्रुटि"}"
        )
    }

    if (!groupTask.isSuccessful) {
        failures.add(
            "Group Posts: ${groupTask.exception?.message ?: "अज्ञात त्रुटि"}"
        )
    }

    if (!statsTask.isSuccessful) {
        failures.add(
            "Storage Stats: ${statsTask.exception?.message ?: "अज्ञात त्रुटि"}"
        )
    }

    val details = if (failures.isEmpty()) {
        error.message ?: "अज्ञात Firestore त्रुटि"
    } else {
        failures.joinToString("\n\n")
    }

    dialog.setMessage(
        "Storage की जानकारी नहीं पढ़ी जा सकी।\n\n" +
            details +
            "\n\nFirestore Rules और Admin permission जाँचें।"
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

        val websiteUrlField = EditText(this).apply {
            hint = "🌐 वेबसाइट लिंक (वैकल्पिक)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        val customButtonLabelField = EditText(this).apply {
            hint = "🔘 कस्टम बटन का नाम (वैकल्पिक)"
            setSingleLine(true)
        }

        val customButtonUrlField = EditText(this).apply {
            hint = "🔗 कस्टम बटन का URL (वैकल्पिक)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        val extraUrlField = EditText(this).apply {
            hint = "🔗 अतिरिक्त URL / लिंक (वैकल्पिक)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        
                box.addView(
            title
        )

        box.addView(
            body
        )

        addFormattingToolbar(
            box,
            body
        )

        
        box.addView(cat)
        box.addView(websiteUrlField)
        box.addView(customButtonLabelField)
        box.addView(customButtonUrlField)
        box.addView(extraUrlField)


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

                        val websiteUrl = websiteUrlField.text.toString().trim()
                        val customButtonLabel =
                            customButtonLabelField.text.toString().trim()
                        val customButtonUrl =
                            customButtonUrlField.text.toString().trim()
                        val extraUrl = extraUrlField.text.toString().trim()

                        val postButtons = if (
                            customButtonLabel.isNotBlank() &&
                            customButtonUrl.isNotBlank()
                        ) {
                            listOf(
                                mapOf(
                                    "label" to customButtonLabel,
                                    "url" to customButtonUrl,
                                    "backgroundColor" to "#1976D2",
                                    "textColor" to "#FFFFFF",
                                    "enabled" to true
                                )
                            )
                        } else {
                            emptyList<Map<String, Any>>()
                        }

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
                                        
                                        "websiteUrl" to websiteUrl,
                                        "buttons" to postButtons,
                                        "extraUrl" to extraUrl,
                                        "extraUrls" to if (extraUrl.isNotBlank()) listOf(extraUrl) else emptyList<String>(),

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
                                commentsEnabled,
                                websiteUrl,
                                postButtons,
                                extraUrl
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
        websiteUrl: String,
        postButtons: List<Map<String, Any>>,
        extraUrl: String,
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
                        "websiteUrl" to websiteUrl,
                        "buttons" to postButtons,
                        "extraUrl" to extraUrl,
                        "extraUrls" to if (extraUrl.isNotBlank()) listOf(extraUrl) else emptyList<String>(),
                        
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
                    listOf(p.id),
                    previewPost = p
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
        val previewImage = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.WHITE)
        }

        box.addView(
            previewImage,
            LinearLayout.LayoutParams(
                -1,
                dp(240)
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        )

        ChannelRepository.loadImage(p.imageUrl) { bitmap, _ ->
            runOnUiThread {
                if (bitmap != null) {
                    previewImage.setImageBitmap(bitmap)

                    previewImage.setOnClickListener {
                        val largeImage = ImageView(this).apply {
                            setImageBitmap(bitmap)
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }

                        AlertDialog.Builder(this)
                            .setTitle("🖼️ Post Photo")
                            .setView(largeImage)
                            .setPositiveButton("बंद करें", null)
                            .show()
                    }
                } else {
                    previewImage.setImageDrawable(null)
                    previewImage.contentDescription =
                        "फोटो लोड नहीं हो सकी"
                }
            }
        }
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
        ids: List<String>,
        previewPost: ChannelPost? = null
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

        
        val deleteContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        deleteContent.addView(
            TextView(this).apply {
                text = "${clean.size} post delete की जाएंगी। यह action वापस नहीं होगा।"
                textSize = 16f
            }
        )

        val imageUrl = previewPost?.imageUrl.orEmpty()

        if (imageUrl.isNotBlank()) {
            val deletePreviewImage = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.WHITE)
            }

            deleteContent.addView(
                deletePreviewImage,
                LinearLayout.LayoutParams(
                    -1,
                    dp(220)
                ).apply {
                    topMargin = dp(8)
                }
            )

            ChannelRepository.loadImage(imageUrl) { bitmap, _ ->
                runOnUiThread {
                    if (bitmap != null) {
                        deletePreviewImage.setImageBitmap(bitmap)
                        deletePreviewImage.setOnClickListener {
                            val largePhoto = ImageView(this).apply {
                                setImageBitmap(bitmap)
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }

                            AlertDialog.Builder(this)
                                .setTitle("🖼️ Post Photo")
                                .setView(largePhoto)
                                .setPositiveButton("बंद करें", null)
                                .show()
                        }
                    }
                }
            }
        }

        
        AlertDialog.Builder(this)
            .setTitle("🗑 Delete Posts")
            .setView(deleteContent)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("DELETE", null)
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

        val title = EditText(this).apply {
            hint = "Post title"
            setText(p.title)
        }

        val body = EditText(this).apply {
            hint = "Post text / details"
            setText(p.body)
            minLines = 6
            gravity = Gravity.TOP
        }

        val cat = EditText(this).apply {
            hint = "Category"
            setText(p.category)
        }

        val websiteUrlField = EditText(this).apply {
            hint = "🌐 वेबसाइट लिंक"
            setSingleLine(true)
            setText(p.websiteUrl)
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        val firstButton = p.buttons.firstOrNull()

        val customButtonLabelField = EditText(this).apply {
            hint = "🔘 कस्टम बटन का नाम"
            setSingleLine(true)
            setText(firstButton?.label.orEmpty())
        }

        val customButtonUrlField = EditText(this).apply {
            hint = "🔗 कस्टम बटन URL"
            setSingleLine(true)
            setText(firstButton?.url.orEmpty())
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        val extraUrlField = EditText(this).apply {
            hint = "🔗 अतिरिक्त URL"
            setSingleLine(true)
            setText(p.extraUrl)
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        val comments = Switch(this).apply {
            text = "Comments ON"
            isChecked = p.commentsEnabled
        }


                box.addView(
            title
        )

        box.addView(
            body
        )

        addFormattingToolbar(
            box,
            body
        )

        box.addView(
            cat
        )

        box.addView(websiteUrlField)
        box.addView(customButtonLabelField)
        box.addView(customButtonUrlField)
        box.addView(extraUrlField)
        
        box.addView(
            comments
        )

        
        if (p.imageUrl.isNotBlank()) {
            val existingPhoto = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.WHITE)
            }

            box.addView(
                existingPhoto,
                LinearLayout.LayoutParams(
                    -1,
                    dp(220)
                ).apply {
                    topMargin = dp(6)
                    bottomMargin = dp(6)
                }
            )

            ChannelRepository.loadImage(p.imageUrl) { bitmap, _ ->
                runOnUiThread {
                    if (bitmap != null) {
                        existingPhoto.setImageBitmap(bitmap)
                        existingPhoto.setOnClickListener {
                            val largePhoto = ImageView(this).apply {
                                setImageBitmap(bitmap)
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }

                            AlertDialog.Builder(this)
                                .setTitle("🖼️ Channel Post Photo")
                                .setView(largePhoto)
                                .setPositiveButton("बंद करें", null)
                                .show()
                        }
                    }
                }
            }
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


                        val buttonLabel =
                            customButtonLabelField.text.toString().trim()

                        val buttonUrl =
                            customButtonUrlField.text.toString().trim()

                        val updatedButtons: List<Map<String, Any>> =
                            if (buttonUrl.isBlank()) {
                                emptyList()
                            } else {
                                listOf(
                                    mapOf(
                                        "label" to buttonLabel.ifBlank {
                                            "लिंक खोलें"
                                        },
                                        "url" to buttonUrl,
                                        "enabled" to true,
                                        "backgroundColor" to
                                            (firstButton?.backgroundColor ?: "#1976D2"),
                                        "textColor" to
                                            (firstButton?.textColor ?: "#FFFFFF")
                                    )
                                )
                            }

                        val data: Map<String, Any> =
                            mapOf(
                                "title" to title.text.toString().trim(),
                                "body" to body.text.toString(),
                                "category" to cat.text.toString().trim(),
                                "commentsEnabled" to comments.isChecked,
                                "websiteUrl" to websiteUrlField.text.toString().trim(),
                                "extraUrl" to extraUrlField.text.toString().trim(),
                                "buttons" to updatedButtons,
                                "updatedAt" to System.currentTimeMillis()
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
