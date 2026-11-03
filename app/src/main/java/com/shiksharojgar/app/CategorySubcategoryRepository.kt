package com.shiksharojgar.app

import com.google.firebase.firestore.FirebaseFirestore

data class CategorySubcategory(
    val id: String = "",
    val category: String = "",
    val name: String = "",
    val color: String = "#1976D2",
    val order: Long = 0L,
    val enabled: Boolean = true
)

object CategorySubcategoryRepository {

    private val db = FirebaseFirestore.getInstance()

    private val subcategoriesRef
        get() = db.collection("category_subcategories")

    private fun defaultItems(category: String): List<Pair<String, String>> {
        return when (category) {

            "study_material" -> listOf(
                "general_knowledge" to "सामान्य ज्ञान",
                "hindi" to "हिंदी",
                "english" to "अंग्रेजी",
                "math" to "गणित",
                "science" to "विज्ञान",
                "social_science" to "सामाजिक विज्ञान",
                "syllabus" to "पाठ्यक्रम",
                "pdf_notes" to "PDF नोट्स"
            )

            "career_guide" -> listOf(
                "government_job" to "सरकारी नौकरी",
                "private_job" to "निजी नौकरी",
                "competitive_exam" to "प्रतियोगी परीक्षाएँ",
                "career_guidance" to "करियर मार्गदर्शन",
                "useful_tools" to "उपयोगी टूल्स"
            )

            else -> emptyList()
        }
    }

    fun load(
        category: String,
        onResult: (List<CategorySubcategory>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (
            category != "study_material" &&
            category != "career_guide"
        ) {
            onResult(emptyList())
            return
        }

        subcategoriesRef
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    val batch = db.batch()

                    defaultItems(category).forEachIndexed { index, item ->

                        val documentId = "${category}_${item.first}"

                        val data = mapOf(
                            "category" to category,
                            "name" to item.second,
                            "color" to "#1976D2",
                            "order" to index.toLong(),
                            "enabled" to true
                        )

                        batch.set(
                            subcategoriesRef.document(documentId),
                            data
                        )
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            load(category, onResult, onError)
                        }
                        .addOnFailureListener { error ->
                            onError(error)
                        }

                } else {
                    onResult(
                        snapshot.documents
                            .map { doc ->
                                CategorySubcategory(
                                    id = doc.id,
                                    category = doc.getString("category").orEmpty(),
                                    name = doc.getString("name").orEmpty(),
                                    color = doc.getString("color") ?: "#1976D2",
                                    order = doc.getLong("order") ?: 0L,
                                    enabled = doc.getBoolean("enabled") ?: true
                                )
                            }
                            .filter { it.enabled }
                            .sortedBy { it.order }
                    )
                }
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun add(
        category: String,
        name: String,
        color: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val document = subcategoriesRef.document()

        val data = mapOf(
            "category" to category,
            "name" to name,
            "color" to color,
            "order" to System.currentTimeMillis(),
            "enabled" to true
        )

        document.set(data)
            .addOnSuccessListener {
                onResult(true, document.id)
            }
            .addOnFailureListener { error ->
                onResult(false, error.message)
            }
    }

    fun update(
        id: String,
        name: String,
        color: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        subcategoriesRef.document(id)
            .update(
                mapOf(
                    "name" to name,
                    "color" to color
                )
            )
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { error ->
                onResult(false, error.message)
            }
    }
}
