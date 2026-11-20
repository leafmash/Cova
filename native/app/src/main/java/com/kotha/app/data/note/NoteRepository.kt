package com.kotha.app.data.note

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kotha.app.core.AppConfig
import com.kotha.app.data.auth.AuthRepository
import com.kotha.app.data.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

fun UserProfile.activeNote(now: Long): String? =
    if (note.isNotBlank() && noteAtMs > 0L && now - noteAtMs < AppConfig.NOTE_TTL_MS) note else null

@Singleton
class NoteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {

    suspend fun set(text: String) {
        val user = authRepository.user ?: error("signed out")
        firestore.collection("users").document(user.uid)
            .update(mapOf("note" to text.trim().take(AppConfig.NOTE_MAX), "noteAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun clear() {
        val user = authRepository.user ?: error("signed out")
        firestore.collection("users").document(user.uid)
            .update(mapOf("note" to FieldValue.delete(), "noteAt" to FieldValue.delete()))
            .await()
    }
}
