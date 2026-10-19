package com.shiksharojgar.app

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GroupAuthActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var titleView: TextView
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var passwordToggleButton: Button
private var passwordVisible = false
    private lateinit var mobileInput: EditText
    private lateinit var submitButton: Button
    private lateinit var switchButton: Button
    private lateinit var resetButton: Button

    private var registerMode = true
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildScreen()
        showMode()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            setBackgroundColor(Color.WHITE)
        }

        titleView = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.rgb(20, 75, 130))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        val description = TextView(this).apply {
            text = "शिक्षा रोजगार ग्रुप"
            textSize = 17f
            setTextColor(Color.DKGRAY)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        nameInput = makeInput(
            "यूज़रनेम",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )

        emailInput = makeInput(
            "ईमेल पता",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        passwordInput = makeInput(
            "पासवर्ड (कम-से-कम 6 अक्षर)",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

                mobileInput = makeInput(
            "10 अंकों का मोबाइल नंबर (अनिवार्य)",
            InputType.TYPE_CLASS_PHONE
        ).apply {
            filters = arrayOf(
                android.text.InputFilter.LengthFilter(10)
            )
        }

        submitButton = Button(this)
        switchButton = Button(this)
        passwordToggleButton = Button(this).apply {
    text = "👁️ पासवर्ड दिखाएँ"

    setOnClickListener {
        passwordVisible = !passwordVisible

        val cursorPosition = passwordInput.selectionStart

        passwordInput.inputType = if (passwordVisible) {
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        passwordInput.setSelection(
            cursorPosition.coerceIn(0, passwordInput.text.length)
        )

        text = if (passwordVisible) {
            "🙈 पासवर्ड छिपाएँ"
        } else {
            "👁️ पासवर्ड दिखाएँ"
        }
    }
}
        resetButton = Button(this).apply {
            text = "पासवर्ड भूल गए?"
            setOnClickListener { resetPassword() }
        }

        switchButton.setOnClickListener {
            if (!busy) {
                registerMode = !registerMode
                showMode()
            }
        }

        submitButton.setOnClickListener {
            if (!busy) {
                if (registerMode) {
                    registerUser()
                } else {
                    loginUser()
                }
            }
        }

        root.addView(titleView)
        root.addView(description)
        root.addView(nameInput)
        root.addView(emailInput)
        root.addView(passwordInput)
root.addView(passwordToggleButton)
root.addView(mobileInput)
        root.addView(submitButton)
        root.addView(switchButton)
        root.addView(resetButton)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun makeInput(
        hintText: String,
        inputType: Int
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            this.inputType = inputType
            textSize = 16f
            isSingleLine = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
    }

    private fun showMode() {
                titleView.text = if (registerMode) {
            "नया अकाउंट बनाएँ"
        } else {
            "यूज़र लॉगिन"
        }

                val authPrefs = getSharedPreferences(
            "shiksha_auth",
            MODE_PRIVATE
        )

        val signedInUser = auth.currentUser
            ?.takeIf { !it.isAnonymous }

        val savedName = authPrefs
            .getString("last_username", "")
            .orEmpty()
            .ifBlank {
                signedInUser?.displayName.orEmpty()
            }

        val savedEmail = authPrefs
            .getString("last_email", "")
            .orEmpty()
            .ifBlank {
                signedInUser?.email.orEmpty()
            }

        val savedMobile = authPrefs
            .getString("last_mobile", "")
            .orEmpty()

        if (nameInput.text.isBlank() && savedName.isNotBlank()) {
            nameInput.setText(savedName)
        }

        if (emailInput.text.isBlank() && savedEmail.isNotBlank()) {
            emailInput.setText(savedEmail)
        }

        if (mobileInput.text.isBlank() && savedMobile.isNotBlank()) {
            mobileInput.setText(savedMobile)
        }
        }
        
        
        nameInput.visibility =
            if (registerMode) View.VISIBLE else View.GONE

        mobileInput.visibility =
            if (registerMode) View.VISIBLE else View.GONE
        
passwordToggleButton.visibility = View.VISIBLE
passwordVisible = false

passwordInput.inputType =
    InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_VARIATION_PASSWORD

passwordToggleButton.text = "👁️ पासवर्ड दिखाएँ"
        
        submitButton.text = if (registerMode) {
            "रजिस्ट्रेशन करें"
        } else {
            "लॉगिन करें"
        }

        switchButton.text = if (registerMode) {
            "पहले से अकाउंट है? लॉगिन करें"
        } else {
            "नया अकाउंट बनाना है?"
        }

        resetButton.visibility =
            if (registerMode) View.GONE else View.VISIBLE
    }

    private fun registerUser() {
        val displayName = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim().lowercase()
        val password = passwordInput.text.toString()
        val mobile = mobileInput.text.toString().trim()

        if (displayName.isBlank() || displayName.length > 40) {
            toast("यूज़रनेम 1 से 40 अक्षरों के बीच रखें।")
            return
        }

                if (
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches() ||
            !email.endsWith("@gmail.com")
        ) {
            toast("कृपया सही Gmail पता दर्ज करें।")
            return
        }

        if (password.length < 6) {
            toast("पासवर्ड कम-से-कम 6 अक्षरों का रखें।")
            return
        }
        if (!mobile.matches(Regex("^[0-9]{10}$"))) {
            toast("मोबाइल नंबर ठीक 10 अंकों का दर्ज करें।")
            return
        }

        val credential =
            EmailAuthProvider.getCredential(email, password)

        setBusy(true)

        val currentUser = auth.currentUser

        if (currentUser?.isAnonymous == true) {
            currentUser.linkWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user

                    if (user == null) {
                        setBusy(false)
                        toast("अकाउंट की जानकारी प्राप्त नहीं हुई।")
                    } else {
                        sendVerificationThenSaveProfile(user, displayName, mobile)
                    }
                }
                .addOnFailureListener { error ->
                    setBusy(false)

                    val message = if (
                        error is com.google.firebase.auth
                            .FirebaseAuthUserCollisionException
                    ) {
                        "यह ईमेल पहले से पंजीकृत है। लॉगिन करें।"
                    } else {
                        error.localizedMessage
                            ?: "अकाउंट लिंक नहीं हो सका।"
                    }

                    toast(message)
                }
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user

                    if (user == null) {
                        setBusy(false)
                        toast("अकाउंट की जानकारी प्राप्त नहीं हुई।")
                    } else {
                        sendVerificationThenSaveProfile(user, displayName, mobile)
                    }
                }
                .addOnFailureListener { error ->
                    setBusy(false)
                    toast(
                        error.localizedMessage
                            ?: "रजिस्ट्रेशन नहीं हो सका।"
                    )
                }
        }
    }

        private fun loginUser() {
        val email = emailInput.text.toString().trim().lowercase()
        val password = passwordInput.text.toString()

        if (email.isBlank() || password.isBlank()) {
            toast("ईमेल और पासवर्ड दोनों दर्ज करें।")
            return
        }

        setBusy(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    setBusy(false)
                    toast("यूज़र की जानकारी प्राप्त नहीं हुई।")
                    return@addOnSuccessListener
                }

                val authPrefs = getSharedPreferences(
                    "shiksha_auth",
                    MODE_PRIVATE
                )

                authPrefs.edit()
                    .putString("last_email", email)
                    .remove("last_username")
                    .remove("last_mobile")
                    .apply()

                db.collection("group_profiles")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { profile ->
                        val savedName = profile
                            .getString("displayName")
                            ?: user.displayName
                            ?: ""

                        val savedMobile = profile
                            .getString("mobileNumber")
                            .orEmpty()

                        authPrefs.edit()
                            .putString("last_email", email)
                            .putString("last_username", savedName)
                            .putString("last_mobile", savedMobile)
                            .apply()

                        finishLogin(user)
                    }
                    .addOnFailureListener {
                        toast(
                            "प्रोफाइल जानकारी लोड नहीं हुई। " +
                                "आप लॉगिन जारी रख सकते हैं।"
                        )
                        finishLogin(user)
                    }
            }
            .addOnFailureListener { error ->
                setBusy(false)
                toast(
                    error.localizedMessage
                        ?: "लॉगिन नहीं हो सका।"
                )
            }
    }

    private fun finishLogin(user: FirebaseUser) {
        user.getIdToken(true)
            .addOnSuccessListener {
                setBusy(false)
                openGroup()
            }
            .addOnFailureListener {
                setBusy(false)
                toast(
                    "लॉगिन हो गया, लेकिन सुरक्षा टोकन " +
                        "रीफ़्रेश नहीं हुआ। दोबारा प्रयास करें।"
                )
            }
    }
    

    private fun sendVerificationThenSaveProfile(
    user: FirebaseUser,
    displayName: String,
    mobile: String
) {
    user.sendEmailVerification()
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                toast(
                    "Gmail पर सत्यापन लिंक भेज दिया गया है। " +
                        "अपना Gmail खोलकर लिंक पर क्लिक करें।"
                )
            } else {
                toast(
                    "सत्यापन ईमेल नहीं भेजा जा सका। " +
                        "बाद में दोबारा प्रयास करें।"
                )
            }

            saveProfile(user, displayName, mobile)
        }
}
    private fun saveProfile(
        user: FirebaseUser,
        displayName: String,
        mobile: String
    ) {
        user.getIdToken(true)
            .addOnSuccessListener {
                val profileRef =
                    db.collection("group_profiles")
                        .document(user.uid)

                profileRef.get()
                    .addOnSuccessListener { document ->
                        val data = hashMapOf<String, Any>(
                            "displayName" to displayName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )

                        if (mobile.isNotBlank()) {
                            data["mobileNumber"] = mobile
                        }

                        if (document.exists()) {
                            profileRef.update(data)
                                                                .addOnSuccessListener {
                                    getSharedPreferences(
                                        "shiksha_auth",
                                        MODE_PRIVATE
                                    ).edit()
                                        .putString(
                                            "last_email",
                                            user.email ?: ""
                                        )
                                        .putString(
                                            "last_username",
                                            displayName
                                        )
                                        .putString(
                                            "last_mobile",
                                            mobile
                                        )
                                        .apply()

                                    setBusy(false)
                                    openGroup()
                                }
                                                                
                                .addOnFailureListener { error ->
                                    setBusy(false)
                                    toast(
                                        error.localizedMessage
                                            ?: "प्रोफ़ाइल अपडेट नहीं हुई।"
                                    )
                                }
                        } else {
                            val newProfile =
                                hashMapOf<String, Any>(
                                    "uid" to user.uid,
                                    "displayName" to displayName,
                                    "email" to (user.email ?: ""),
                                    "createdAt" to
                                        FieldValue.serverTimestamp(),
                                    "updatedAt" to
                                        FieldValue.serverTimestamp()
                                )

                            if (mobile.isNotBlank()) {
                                newProfile["mobileNumber"] = mobile
                            }

                                                        profileRef.set(newProfile)
                                .addOnSuccessListener {
                                    getSharedPreferences(
                                        "shiksha_auth",
                                        MODE_PRIVATE
                                    ).edit()
                                        .putString(
                                            "last_email",
                                            user.email ?: ""
                                        )
                                        .putString(
                                            "last_username",
                                            displayName
                                        )
                                        .putString(
                                            "last_mobile",
                                            mobile
                                        )
                                        .apply()

                                    setBusy(false)
                                    openGroup()
                                }
                                
                                .addOnFailureListener { error ->
                                    setBusy(false)
                                    toast(
                                        error.localizedMessage
                                            ?: "प्रोफ़ाइल सेव नहीं हुई।"
                                    )
                                }
                        }
                    }
                    .addOnFailureListener { error ->
                        setBusy(false)
                        toast(
                            error.localizedMessage
                                ?: "प्रोफ़ाइल की जाँच नहीं हो सकी।"
                        )
                    }
            }
            .addOnFailureListener { error ->
                setBusy(false)
                toast(
                    error.localizedMessage
                        ?: "सुरक्षा टोकन रीफ़्रेश नहीं हुआ।"
                )
            }
    }

    private fun resetPassword() {
        val email = emailInput.text.toString().trim().lowercase()

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()
        ) {
            toast("पहले सही ईमेल पता दर्ज करें।")
            return
        }

        setBusy(true)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                setBusy(false)
                toast("पासवर्ड रीसेट ईमेल भेज दिया गया है।")
            }
            .addOnFailureListener {
                setBusy(false)

                // अकाउंट की मौजूदगी का खुलासा न करें।
                toast(
                    "यदि इस ईमेल से अकाउंट उपलब्ध है, " +
                        "तो रीसेट निर्देश भेजे जाएँगे।"
                )
            }
    }

    private fun openGroup() {
        setResult(RESULT_OK)
        finish()
    }

    private fun setBusy(value: Boolean) {
        busy = value
        submitButton.isEnabled = !value
        switchButton.isEnabled = !value
        resetButton.isEnabled = !value

        submitButton.text = if (value) {
            "कृपया प्रतीक्षा करें…"
        } else if (registerMode) {
            "रजिस्ट्रेशन करें"
        } else {
            "लॉगिन करें"
        }
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
