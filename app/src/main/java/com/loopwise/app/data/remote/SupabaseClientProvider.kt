package com.loopwise.app.data.remote

import com.loopwise.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Single Supabase client shared across repositories (registered as a Koin singleton
 * in di/AppModule.kt). URL/key come from local.properties -> BuildConfig, never hardcoded.
 */
fun buildSupabaseClient() = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}
