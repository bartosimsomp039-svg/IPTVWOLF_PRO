package com.tuempresa.wolfiptv

import android.content.Context

object DataRepository {

    // ── Claves internas de caché ───────────────────────────────────────────
    // NO son account-specific aquí: la account-specificity la da el
    // parámetro "usuario" que ya incluye el índice de cuenta en MainActivity.
    private const val KEY_LIVE           = "live"
    private const val KEY_MOVIES         = "movies"
    private const val KEY_SERIES         = "series"
    private const val KEY_M3U_LIVE       = "m3u_live"
    private const val KEY_M3U_MOVIES     = "m3u_movies"
    private const val KEY_M3U_SERIES     = "m3u_series"
    private const val KEY_M3U_EPISODES   = "m3u_series_episodes"

    // ── Usuario actual ─────────────────────────────────────────────────────
    private var _currentUser = ""
    val currentUser: String get() = _currentUser
    var currentSeriesId = ""

    // ── Datos Xtream en memoria ────────────────────────────────────────────
    var liveData:   Map<String, List<Channel>>? = null
    var moviesData: Map<String, List<Channel>>? = null
    var seriesData: Map<String, List<Channel>>? = null

    // ── Datos M3U en memoria ───────────────────────────────────────────────
    var m3uLiveData    = mutableMapOf<String, MutableList<Channel>>()
    var m3uMoviesData  = mutableMapOf<String, MutableList<Channel>>()
    var m3uSeriesData  = mutableMapOf<String, MutableList<Channel>>()
    var m3uSeriesUnique = mutableListOf<Channel>()

    // ── Cache de series parseadas ──────────────────────────────────────────
    var cachedTemporadas  = emptyList<Temporada>()
    var seriesInfoCache   = mutableMapOf<String, List<Temporada>>()
    var parsedSeriesCache = mutableMapOf<String, MutableList<Serie>>()

    // ── Info de la serie activa ────────────────────────────────────────────
    var currentSeriesTitle       = ""
    var currentSeriesDescription = ""
    var currentSeriesPoster      = ""
    var currentSeriesYear        = ""
    var currentSeriesGenre       = ""
    var currentSeriesRating      = ""
    var currentSeriesCast        = ""
    var currentSeriesDirector    = ""

    var posterCache = mutableMapOf<String, String>()
    var isLoaded    = false

    // ══════════════════════════════════════════════════════════════════════
    // API PÚBLICA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Cambia el usuario activo.
     * Devuelve TRUE si el caché en disco era válido y ya fue restaurado
     * (no necesitas llamar a la API).
     * Devuelve FALSE si debes hacer fetch y luego llamar a saveCache().
     *
     * ⚠️ Llama esto desde un hilo IO (Dispatchers.IO) — lee archivos.
     */
    fun setCurrentUser(context: Context, usuario: String): Boolean {
        if (_currentUser == usuario) return isLoaded
        clearMemory()
        _currentUser = usuario
        return if (usuario.isNotBlank()) loadCache(context) else false
    }
    fun setCurrentUserDirect(usuario: String) {
        if (_currentUser == usuario) return
        clearMemory()
        _currentUser = usuario
    }


    /**
     * Guarda todos los datos actuales en disco.
     * ⚠️ Llama esto desde un hilo IO (Dispatchers.IO) — escribe archivos.
     */
    fun saveCache(context: Context) {
        if (_currentUser.isBlank()) return
        liveData?.let   { CacheManager.guardar(context, _currentUser, KEY_LIVE,   channelMapToJson(it)) }
        moviesData?.let { CacheManager.guardar(context, _currentUser, KEY_MOVIES, channelMapToJson(it)) }
        seriesData?.let { CacheManager.guardar(context, _currentUser, KEY_SERIES, channelMapToJson(it)) }
        if (m3uLiveData.isNotEmpty())
            CacheManager.guardar(context, _currentUser, KEY_M3U_LIVE,   channelMapToJson(m3uLiveData))
        if (m3uMoviesData.isNotEmpty())
            CacheManager.guardar(context, _currentUser, KEY_M3U_MOVIES, channelMapToJson(m3uMoviesData))
        if (m3uSeriesData.isNotEmpty())
            CacheManager.guardar(context, _currentUser, KEY_M3U_SERIES, channelMapToJson(m3uSeriesData))
    }

    /**
     * Carga datos desde disco.
     * ⚠️ Llama esto desde un hilo IO (Dispatchers.IO) — lee archivos.
     * Devuelve TRUE si se cargó correctamente.
     */
    fun loadCache(context: Context): Boolean {
        if (_currentUser.isBlank()) return false
        return try {
            // Xtream — opcionales, no fallan si no existen
            CacheManager.obtener(context, _currentUser, KEY_LIVE)
                ?.let { liveData   = jsonToChannelMap(it) }
            CacheManager.obtener(context, _currentUser, KEY_MOVIES)
                ?.let { moviesData = jsonToChannelMap(it) }
            CacheManager.obtener(context, _currentUser, KEY_SERIES)
                ?.let { seriesData = jsonToChannelMap(it) }
            // M3U — opcionales, no fallan si no existen
            CacheManager.obtener(context, _currentUser, KEY_M3U_LIVE)
                ?.let { m3uLiveData   = jsonToChannelMap(it).toMutableChannelMap() }
            CacheManager.obtener(context, _currentUser, KEY_M3U_MOVIES)
                ?.let { m3uMoviesData = jsonToChannelMap(it).toMutableChannelMap() }
            CacheManager.obtener(context, _currentUser, KEY_M3U_SERIES)
                ?.let { m3uSeriesData = jsonToChannelMap(it).toMutableChannelMap() }
            // isLoaded = true solo si al menos un tipo cargó datos reales
            isLoaded = liveData != null || m3uLiveData.isNotEmpty()
            isLoaded
        } catch (e: Exception) {
            e.printStackTrace()
            clearMemory()
            false
        }
    }

    /** Horas que le quedan al caché de live antes de expirar. */
    fun horasRestantes(context: Context) =
        CacheManager.horasRestantes(context, _currentUser, KEY_LIVE)

    /** Limpia solo la memoria (no el disco). */
    fun clearMemory() {
        liveData = null; moviesData = null; seriesData = null
        m3uLiveData.clear(); m3uMoviesData.clear(); m3uSeriesData.clear()
        m3uSeriesUnique.clear(); parsedSeriesCache.clear()
        seriesInfoCache.clear(); cachedTemporadas = emptyList()
        posterCache.clear(); isLoaded = false; currentSeriesId = ""
        currentSeriesTitle = ""; currentSeriesDescription = ""
        currentSeriesPoster = ""; currentSeriesYear = ""
        currentSeriesGenre = ""; currentSeriesRating = ""
        currentSeriesCast = ""; currentSeriesDirector = ""
    }

    /** Limpia memoria + disco para el usuario actual. */
    fun clearAll(context: Context) {
        CacheManager.limpiarUsuario(context, _currentUser)
        clearMemory()
    }

    // ══════════════════════════════════════════════════════════════════════
    // SERIALIZACIÓN — usa solo name/url/logo (campos reales de Channel)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Convierte un mapa de canales a JSON usando solo los campos que
     * existen en tu clase Channel: name, url, logo.
     *
     * Si tu Channel tiene campos extra (streamId, epg, etc.), agrégalos
     * aquí dentro del bloque apply { }.
     */
    private fun channelMapToJson(map: Map<String, List<Channel>>): String {
        val sb = StringBuilder("{")
        var firstCat = true
        for ((cat, channels) in map) {
            if (!firstCat) sb.append(",")
            firstCat = false
            sb.append("\"").append(cat.replace("\"", "\\\"")).append("\":[")
            var firstCh = true
            for (ch in channels) {
                if (!firstCh) sb.append(",")
                firstCh = false
                sb.append("{")
                sb.append("\"name\":\"").append(ch.name.replace("\"", "\\\"")).append("\",")
                sb.append("\"url\":\"").append(ch.url.replace("\"", "\\\"")).append("\",")
                sb.append("\"logo\":\"").append(ch.logo.replace("\"", "\\\"")).append("\"")
                // ← Si Channel tiene más campos, agrégalos aquí:
                // sb.append(",\"streamId\":\"").append(ch.streamId.orEmpty()).append("\"")
                sb.append("}")
            }
            sb.append("]")
        }
        sb.append("}")
        return sb.toString()
    }

    /**
     * Restaura un mapa de canales desde JSON.
     * Solo usa los campos name/url/logo. Ajusta si tienes más campos.
     */
    private fun jsonToChannelMap(json: String): Map<String, MutableList<Channel>> {
        val gson     = com.google.gson.Gson()
        val typeToken = object : com.google.gson.reflect.TypeToken<Map<String, List<Map<String, String>>>>() {}.type
        val raw: Map<String, List<Map<String, String>>> = gson.fromJson(json, typeToken)
        return raw.mapValues { (_, channels) ->
            channels.mapTo(mutableListOf()) { fields ->
                Channel(
                    name = fields["name"].orEmpty(),
                    url  = fields["url"].orEmpty(),
                    logo = fields["logo"].orEmpty()
                    // ← Si Channel tiene más campos, descomenta y agrega:
                    // streamId = fields["streamId"].orEmpty(),
                    // epg      = fields["epg"].orEmpty()
                )
            }
        }
    }

    private fun Map<String, List<Channel>>.toMutableChannelMap() =
        entries.associateTo(mutableMapOf()) { (k, v) -> k to v.toMutableList() }
}
