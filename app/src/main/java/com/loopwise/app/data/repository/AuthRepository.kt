package com.loopwise.app.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AuthUser(val id: String, val email: String?)

class AuthRepository(private val client: SupabaseClient) {

    /** Emits the signed-in user, or null when there's no active session. */
    val currentUser: Flow<AuthUser?> = client.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.let {
            AuthUser(id = it.id, email = it.email)
        }
    }

    fun currentUserIdOrNull(): String? = client.auth.currentUserOrNull()?.id

    suspend fun signUpWithEmail(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null) {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
