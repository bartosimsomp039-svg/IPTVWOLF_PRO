package com.tuempresa.wolfiptv

import android.content.Context
import java.io.File

object CacheManager {

    private const val PREFS_META = "cache_meta"
    private const val TTL_MS     = 2L * 24 * 60 * 60 * 1000   // 48 horas
    private const val TS_SUFFIX  = "_ts_"

    fun guardar(context: Context, usuario: String, key: String, json: String) {
        val (su, sk) = safe(usuario, key)
        try {
            val file = cacheFile(context, su, sk)
            file.parentFile?.mkdirs()
            file.writeText(json, Charsets.UTF_8)
            prefs(context).edit()
                .putLong(tsKey(su, sk), System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "guardar error: ${e.message}")
        }
    }

    fun obtener(context: Context, usuario: String, key: String): String? {
        val (su, sk) = safe(usuario, key)
        val savedAt = prefs(context).getLong(tsKey(su, sk), 0L)
        if (savedAt == 0L || System.currentTimeMillis() - savedAt > TTL_MS) {
            cacheFile(context, su, sk).delete()
            prefs(context).edit().remove(tsKey(su, sk)).apply()
            return null
        }
        return try {
            val file = cacheFile(context, su, sk)
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        } catch (_: Exception) { null }
    }

    fun estaVigente(context: Context, usuario: String, key: String): Boolean {
        val (su, sk) = safe(usuario, key)
        val savedAt = prefs(context).getLong(tsKey(su, sk), 0L)
        return savedAt > 0L && System.currentTimeMillis() - savedAt <= TTL_MS
    }

    fun horasRestantes(context: Context, usuario: String, key: String): Long {
        val (su, sk) = safe(usuario, key)
        val savedAt = prefs(context).getLong(tsKey(su, sk), 0L)
        if (savedAt == 0L) return 0L
        val restante = TTL_MS - (System.currentTimeMillis() - savedAt)
        return if (restante > 0L) restante / (1000L * 60L * 60L) else 0L
    }

    fun limpiarUsuario(context: Context, usuario: String) {
        val (su, _) = safe(usuario, "")
        File(context.cacheDir, "iptv_cache/$su").deleteRecursively()
        val p = prefs(context)
        val e = p.edit()
        p.all.keys.filter { it.startsWith("$su::") }.forEach { e.remove(it) }
        e.apply()
    }

    fun limpiarTodo(context: Context) {
        File(context.cacheDir, "iptv_cache").deleteRecursively()
        prefs(context).edit().clear().apply()
    }

    private fun cacheFile(context: Context, usuario: String, key: String): File {
        val dir = File(context.cacheDir, "iptv_cache/$usuario")
        return File(dir, "$key.json")
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_META, Context.MODE_PRIVATE)

    private fun tsKey(usuario: String, key: String) = "$usuario::$key$TS_SUFFIX"

    private fun safe(usuario: String, key: String): Pair<String, String> {
        val rx = Regex("[^a-zA-Z0-9_\\-]")
        return Pair(
            usuario.replace(rx, "_").ifEmpty { "anon" },
            key.replace(rx, "_").ifEmpty { "data" }
        )
    }
}