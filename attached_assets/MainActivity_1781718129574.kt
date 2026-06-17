package com.tuempresa.wolfiptv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import java.io.BufferedReader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var searchView: SearchView

    private var fullMap = mapOf<String, List<Channel>>()

    private val client by lazy {

        // 🔥 TRUST ALL SSL
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(

            object : javax.net.ssl.X509TrustManager {

                override fun checkClientTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<java.security.cert.X509Certificate>,
                    authType: String
                ) {
                }

                override fun getAcceptedIssuers():
                        Array<java.security.cert.X509Certificate> {

                    return arrayOf()
                }
            }
        )

        val sslContext =
            javax.net.ssl.SSLContext.getInstance("TLS")

        sslContext.init(
            null,
            trustAllCerts,
            java.security.SecureRandom()
        )

        val sslSocketFactory =
            sslContext.socketFactory

        OkHttpClient.Builder()

            .sslSocketFactory(
                sslSocketFactory,
                trustAllCerts[0] as javax.net.ssl.X509TrustManager
            )

            .hostnameVerifier { _, _ -> true }

            .connectTimeout(
                10,
                TimeUnit.SECONDS
            )

            .readTimeout(
                15,
                TimeUnit.SECONDS
            )

            .retryOnConnectionFailure(true)

            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        recycler =
            findViewById(R.id.recyclerMain)

        recycler.setHasFixedSize(true)

        recycler.setItemViewCacheSize(30)

// 🔥 GRID PRINCIPAL
        recycler.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(
                this,
                5
            )

        recycler.setHasFixedSize(true)

        recycler.itemAnimator = null

// 🔥 ADAPTER VACÍO INICIAL
        recycler.adapter =

            ChannelNetflixAdapter(
                emptyList(),
                this
            ) {}

        recyclerCategories =
            findViewById(R.id.recyclerCategories)

        progress =
            findViewById(R.id.progress)

        searchView =
            findViewById(R.id.searchView)

        recycler.setHasFixedSize(true)

        recycler.itemAnimator = null

        recycler.setItemViewCacheSize(20)

        recyclerCategories.layoutManager = LinearLayoutManager(this)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val currentType = this@MainActivity.intent.getStringExtra("type") ?: "live"
                val query = newText.orEmpty().lowercase()

                val filtered = fullMap.values
                    .flatten()
                    .filter { it.name.lowercase().contains(query) }

                // ✅ playlist pre-calculada una sola vez, no en cada click
                val playlist = ArrayList(filtered.map { it.url })

                recycler.adapter = ChannelNetflixAdapter(filtered, this@MainActivity) { channel ->
                    val intent = Intent(this@MainActivity, VideoPlayerActivity::class.java)
                    intent.putExtra("url", channel.url)
                    intent.putExtra("type", currentType)
                    intent.putExtra("name", channel.name)
                    intent.putExtra("logo", channel.logo)
                    intent.putStringArrayListExtra("playlist", playlist)
                    intent.putExtra("index", filtered.indexOf(channel))
                    startActivity(intent)
                }

                return true
            }
        })
        val type = intent.getStringExtra("type") ?: "live"

        val prefs =
            getSharedPreferences(
                "iptv",
                MODE_PRIVATE
            )

        val accountIndex =
            prefs.getInt(
                "current_account",
                0
            )

        val accountCacheKey = type   // "live", "movies" o "series"
        DataRepository.setCurrentUserDirect(accountIndex.toString()) // ✅ NUEVO

// 🔥 1. CACHE EN MEMORIA

        val json = prefs.getString("accounts", null)

        val isM3U = try {

            val typeToken =
                object : com.google.gson.reflect.TypeToken<MutableList<Account>>() {}.type

            val list: MutableList<Account> =
                com.google.gson.Gson().fromJson(json, typeToken)

            val index = prefs.getInt("current_account", 0)

            list[index].type == "m3u"

        } catch (e: Exception) {
            false
        }
        // ── Toda la lógica de caché dentro de una coroutine ──────────────
        lifecycleScope.launch {
            var cacheJson: String? = withContext(Dispatchers.IO) {
                CacheManager.obtener(this@MainActivity, DataRepository.currentUser, accountCacheKey)
            }

            // ── Solo para M3U series: verificar cache de episodios ──
            if (isM3U && type == "series") {
                val hasEpCache = withContext(Dispatchers.IO) {
                    !CacheManager.obtener(this@MainActivity, DataRepository.currentUser, "series_episodes").isNullOrEmpty()
                }
                if (!hasEpCache && DataRepository.m3uSeriesData.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        CacheManager.guardar(
                            this@MainActivity,
                            DataRepository.currentUser,
                            accountCacheKey,
                            ""
                        )
                    }
                    cacheJson = null
                }
            }

            // ── Corre para TODOS los tipos: live, movies, series ──────────
            if (cacheJson != null) {
                try {
                    val restored: Map<String, List<Channel>> = withContext(Dispatchers.IO) {
                        val typeToken = object :
                            com.google.gson.reflect.TypeToken<Map<String, List<Channel>>>() {}.type
                        com.google.gson.Gson().fromJson(cacheJson, typeToken)
                    }
                    if (isM3U) {
                        when (type) {
                            "live"   -> DataRepository.m3uLiveData   = restored.mapValues { it.value.toMutableList() }.toMutableMap()
                            "movies" -> DataRepository.m3uMoviesData = restored.mapValues { it.value.toMutableList() }.toMutableMap()
                            "series" -> DataRepository.m3uSeriesData = restored.mapValues { it.value.toMutableList() }.toMutableMap()
                        }
                    } else {
                        when (type) {
                            "live"   -> DataRepository.liveData   = restored
                            "movies" -> DataRepository.moviesData = restored
                            "series" -> DataRepository.seriesData = restored
                        }
                    }
                } catch (_: Exception) {}
            }

            val cached = when (type) {
                "live"   -> if (isM3U) DataRepository.m3uLiveData.takeIf   { it.isNotEmpty() } else DataRepository.liveData
                "movies" -> if (isM3U) DataRepository.m3uMoviesData.takeIf { it.isNotEmpty() } else DataRepository.moviesData
                "series" -> if (isM3U) DataRepository.m3uSeriesData.takeIf { it.isNotEmpty() } else DataRepository.seriesData
                else -> null
            }

            val restoredMap = cached as? Map<String, List<Channel>>
            if (restoredMap != null && restoredMap.isNotEmpty() && restoredMap.values.flatten().isNotEmpty()) {
                progress.visibility = View.GONE
                setupSidebar(restoredMap, type)

                if (isM3U && type == "series" && DataRepository.parsedSeriesCache.isEmpty()) {
                    launch(Dispatchers.IO) {
                        try {
                            // Primero intentar el caché completo (guarda todos los formatos: S01E01, Cap N, etc.)
                            val fullJson = CacheManager.obtener(this@MainActivity, DataRepository.currentUser, "series_episodes")
                            if (!fullJson.isNullOrEmpty()) {
                                val tt = object : com.google.gson.reflect.TypeToken<List<Serie>>() {}.type
                                val allParsed: List<Serie> = com.google.gson.Gson().fromJson(fullJson, tt)
                                if (allParsed.isNotEmpty()) {
                                    DataRepository.parsedSeriesCache["__M3U_ALL__"] = allParsed.toMutableList()
                                    return@launch
                                }
                            }
                            // Fallback: caché de episodios con regex (solo S01E01/NxN, menos completo)
                            val ej = CacheManager.obtener(
                                this@MainActivity,
                                DataRepository.currentUser,
                                "${accountIndex}_series_episodes"
                            )
                            if (!ej.isNullOrEmpty() && ej != "ready") {
                                val tt = object : com.google.gson.reflect.TypeToken<List<Channel>>() {}.type
                                val eps: List<Channel> = com.google.gson.Gson().fromJson(ej, tt)
                                val parsedSeries = M3USeriesParser.parse(eps)
                                if (parsedSeries.isNotEmpty()) {
                                    DataRepository.parsedSeriesCache["__M3U_ALL__"] = parsedSeries
                                }
                            }
                            // ✅ ELIMINADO: el else que parseaba m3uSeriesData.values.flatten()
                            // (esos son canales ya procesados 1-por-serie, no episodios crudos)
                        } catch (_: Exception) {}
                    }
                }

                return@launch
            }

            progress.visibility = View.VISIBLE
            loadUserContent(type)
        }
    }

        private fun loadUserContent(type: String, background: Boolean = false) {
            // Al cargar en background, NO mostrar spinner ni bloquear UI
            if (!background) progress.visibility = View.VISIBLE

            val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
            val json = prefs.getString("accounts", null)

            if (json.isNullOrEmpty()) {
                goLogin()
                return
            }

            try {

                val typeToken =
                    object : com.google.gson.reflect.TypeToken<MutableList<Account>>() {}.type
                val list: MutableList<Account> = com.google.gson.Gson().fromJson(json, typeToken)

                if (list.isEmpty()) {
                    goLogin()
                    return
                }

                var index = prefs.getInt("current_account", 0)
                if (index >= list.size) index = 0

                val acc = list[index]

                // =========================
                // 🔥 M3U (modo Smarters)
                // =========================
                if (acc.type == "m3u") {

                    val cached = when (type) {

                        "live" ->
                            DataRepository.m3uLiveData.isNotEmpty()

                        "movies" ->
                            DataRepository.m3uMoviesData.isNotEmpty()

                        "series" ->
                            DataRepository.m3uSeriesData.isNotEmpty()

                        else -> false
                    }

                    if (cached) {

                        progress.visibility = View.GONE

                        setupSidebar(
                            when (type) {
                                "live" -> DataRepository.m3uLiveData
                                "movies" -> DataRepository.m3uMoviesData
                                else -> DataRepository.m3uSeriesData
                            },
                            type
                        )

                        return
                    }

                    loadM3U(acc.url, type)

                    return
                }

                // =========================
                // 🔥 XTREAM
                // =========================
                loadXtream(acc.host, acc.user, acc.pass, type)

            } catch (e: Exception) {
                goLogin()
            }
        }

        // =========================
        // 🔥 XTREAM MEJORADO
        // =========================

        private fun loadXtream(host: String, user: String, pass: String, type: String) {

            // ── Cache en memoria (más rápido) ──
            if (type == "live" && DataRepository.liveData != null) {
                progress.visibility = View.GONE
                setupSidebar(DataRepository.liveData!!, type)
                return
            }
            if (type == "movies" && DataRepository.moviesData != null) {
                progress.visibility = View.GONE
                setupSidebar(DataRepository.moviesData!!, type)
                return
            }
            if (type == "series" && DataRepository.seriesData != null) {
                progress.visibility = View.GONE
                setupSidebar(DataRepository.seriesData!!, type)
                return
            }

            progress.visibility = View.VISIBLE

            // FIX 1: clave de caché única por cuenta (no comparte slot entre M3U y Xtream)
            val accountIndex =
                getSharedPreferences("iptv", MODE_PRIVATE).getInt("current_account", 0)
            val accountCacheKey = "${accountIndex}_$type"
            DataRepository.setCurrentUserDirect(accountIndex.toString()) // ✅ NUEVO

            lifecycleScope.launch(Dispatchers.IO) {
                TMDBHelper.loadDiskCache(this@MainActivity)  // ← añade esta línea
                try {

                    try {

                        val actionCat = when (type) {
                            "live" -> "get_live_categories"
                            "movies" -> "get_vod_categories"
                            "series" -> "get_series_categories"
                            else -> "get_live_categories"
                        }

                        val actionStreams = when (type) {
                            "live" -> "get_live_streams"
                            "movies" -> "get_vod_streams"
                            "series" -> "get_series"
                            else -> "get_live_streams"
                        }

                        val cleanHost = host.removeSuffix("/")

                        // ── Categorías ──
                        val catUrl =
                            "$cleanHost/player_api.php?username=$user&password=$pass&action=$actionCat"
                        val catBody =
                            client.newCall(Request.Builder().url(catUrl).build()).execute()
                                .body?.string().orEmpty()

                        val categoryMap = mutableMapOf<String, String>()
                        if (catBody.isNotBlank()) {
                            val arr = JSONArray(catBody)
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                categoryMap[o.optString("category_id")] =
                                    o.optString("category_name")
                            }
                        }

                        // ── Streams ──
                        val streamsUrl =
                            "$cleanHost/player_api.php?username=$user&password=$pass&action=$actionStreams"
                        val res = client.newCall(
                            Request.Builder()
                                .url(streamsUrl)
                                .addHeader("User-Agent", "Mozilla/5.0")
                                .addHeader("Accept", "*/*")
                                .build()
                        ).execute()
                        val body = res.body?.string().orEmpty()
                        val array = JSONArray(body)

                        val map = linkedMapOf<String, MutableList<Channel>>()

                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue

                            val name = item.optString("name")
                            var logo = item.optString("stream_icon")

                            // 🔥 LOGOS VACÍOS
                            if (logo.isBlank() || logo == "null" || logo == "N/A") {
                                logo = TMDBHelper.getPosterCached(name, type == "series")
                            }

                            val id = when (type) {
                                "series" -> item.optString("series_id", item.optString("stream_id"))
                                else -> item.optString("stream_id")
                            }
                            val catId = when (type) {
                                "series" -> item.optString(
                                    "category_id",
                                    item.optString("series_id")
                                )

                                else -> item.optString("category_id")
                            }

                            if (id.isEmpty()) continue

                            val playUrl = when (type) {
                                "live" -> "$cleanHost/live/$user/$pass/$id.ts"
                                "movies" -> {
                                    val container = item.optString("container_extension", "mp4")
                                    "$cleanHost/movie/$user/$pass/$id.$container"
                                }

                                "series" -> {
                                    val seriesId =
                                        item.optString("series_id", item.optString("stream_id"))
                                    "SERIES_$seriesId"
                                }

                                else -> ""
                            }

                            val cat = when {
                                categoryMap.containsKey(catId) -> categoryMap[catId]!!
                                catId.isNotEmpty() -> catId
                                else -> name.take(1).uppercase()
                            }

                            map.getOrPut(cat) { mutableListOf() }.add(Channel(name, playUrl, logo))
                        }

                        // FIX 3: serializar en hilo IO (no bloquear la UI con JSON grande)
                        val jsonCache = com.google.gson.Gson().toJson(map)

                        // FIX 2 + FIX 3: guardar en disco en hilo IO, una sola vez, con clave correcta
                        // (se elimina el bloque duplicado que guardaba m3uLiveData/moviesData vacíos)
                        CacheManager.guardar(
                            this@MainActivity,
                            DataRepository.currentUser,
                            accountCacheKey,
                            jsonCache
                        )
                        SmartCache.guardarCategorias(this@MainActivity, type, map.keys.toList())
                        UpdateManager.guardarUltimaActualizacion(this@MainActivity)
                        TMDBHelper.saveDiskCache(this@MainActivity) // ✅ NUEVO

                        // ── Actualizar UI en Main ──
                        withContext(Dispatchers.Main) {
                            when (type) {
                                "live" -> DataRepository.liveData = map
                                "movies" -> DataRepository.moviesData = map
                                "series" -> DataRepository.seriesData = map
                            }
                            progress.visibility = View.GONE
                            setupSidebar(map, type)
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            progress.visibility = View.GONE
                            toast("Error Xtream: ${e.message}")
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun getWorkingUrl(host: String, user: String, pass: String, id: String): String {
            return "$host/live/$user/$pass/$id.ts"
        }

        // =========================
        // 🔥 M3U MEJORADO
        // =========================

        private fun loadM3U(url: String, type: String) {

            progress.visibility = View.VISIBLE

            // FIX 1: clave de caché única por cuenta
            val accountIndex =
                getSharedPreferences("iptv", MODE_PRIVATE).getInt("current_account", 0)
            DataRepository.setCurrentUserDirect(accountIndex.toString()) // ✅ NUEVO

            lifecycleScope.launch(Dispatchers.IO) {
                TMDBHelper.loadDiskCache(this@MainActivity)  // ← añade esta línea
                try {

                    try {

                        // 1. Parsear el M3U completo
                        val parsed = M3UParser.parse(url)

                        // 2. Clasificar correctamente Live / Películas / Series
                        M3UContentResolver.resolve(parsed)

                        // 3. Guardar en caché en hilo IO con clave por cuenta
                        val gson = com.google.gson.Gson()
                        val user = DataRepository.currentUser

                        when (type) {
                            "live" -> {
                                CacheManager.guardar(
                                    this@MainActivity, user,
                                    "${accountIndex}_live",
                                    gson.toJson(DataRepository.m3uLiveData)
                                )
                            }

                            "movies" -> {
                                CacheManager.guardar(
                                    this@MainActivity, user,
                                    "${accountIndex}_movies",
                                    gson.toJson(DataRepository.m3uMoviesData)
                                )
                            }

                            "series" -> {
                                CacheManager.guardar(
                                    this@MainActivity, user,
                                    "${accountIndex}_series",
                                    gson.toJson(DataRepository.m3uSeriesData)
                                )

                                // FIX 6: cachear episodios desde el M3U COMPLETO (no solo categorías de series)
                                val episodeRx = Regex(
                                    """[Ss]\d{1,2}[\s._\-\[|]*[Ee]\d{1,3}|\d{1,2}x\d{1,3}|[Tt]\d{1,2}[\s._\-\[|]*[Ee]\d{1,3}|[Tt]emporada\s+\d|[Ss]eason\s+\d""",
                                    RegexOption.IGNORE_CASE
                                )
                                val todosEpisodios = parsed.values.flatten()
                                    .filter { episodeRx.containsMatchIn(it.name) }
                                if (todosEpisodios.isNotEmpty()) {
                                    // ✅ Marcador liviano — evita guardar 10-20MB en SharedPreferences
                                    CacheManager.guardar(
                                        this@MainActivity, user,
                                        "${accountIndex}_series_episodes",
                                        gson.toJson(todosEpisodios)
                                    )
                                    // Parsear en memoria para que SeriesActivity tenga datos inmediatos
                                    val allParsed = M3USeriesParser.parse(todosEpisodios)
                                    if (allParsed.isNotEmpty()) {
                                        DataRepository.parsedSeriesCache["__M3U_ALL__"] = allParsed
                                    }
                                }

                                // ✅ FIX: guardar el parsedSeriesCache completo en disco
                                // (incluye series con Cap/Capitulo/Ep que no pasan el episodeRx)
                                val fullParsedSeries =
                                    DataRepository.parsedSeriesCache.values.flatten()
                                if (fullParsedSeries.isNotEmpty()) {
                                    CacheManager.guardar(
                                        this@MainActivity, user,
                                        "${accountIndex}_parsed_series_full",
                                        gson.toJson(fullParsedSeries)
                                    )
                                }
                            }
                        }

                                SmartCache.guardarCategorias(
                            this@MainActivity,
                            type,
                            when (type) {
                                "live" -> DataRepository.m3uLiveData.keys.toList()
                                "movies" -> DataRepository.m3uMoviesData.keys.toList()
                                else -> DataRepository.m3uSeriesData.keys.toList()
                            }
                        )

                        UpdateManager.guardarUltimaActualizacion(this@MainActivity)

                        TMDBHelper.saveDiskCache(this@MainActivity)
                        withContext(Dispatchers.Main) {
                            progress.visibility = View.GONE

                            val map: Map<String, List<Channel>> = when (type) {
                                "live" -> DataRepository.m3uLiveData
                                "movies" -> DataRepository.m3uMoviesData
                                else -> DataRepository.m3uSeriesData
                            }

                            setupSidebar(map, type)
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            progress.visibility = View.GONE
                            toast("Error M3U: ${e.message}")
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun setupSidebar(
            map: Map<String, List<Channel>>,
            type: String
        ) {

            Log.e(
                "SIDEBAR_DEBUG",
                "TYPE=$type"
            )

            Log.e(
                "SIDEBAR_DEBUG",
                "CATEGORIES=${map.keys.take(5)}"
            )

            fullMap = map

            // 🔥 SOLO XTREAM USA CACHE COMPLETO
            if (type != "m3u") {

                when (type) {

                    "live" ->
                        DataRepository.liveData = map

                    "movies" ->
                        DataRepository.moviesData = map

                    "series" ->
                        DataRepository.seriesData = map
                }
            }

            val keys =

                if (type == "series") {

                    map.keys.filterNot {

                        val n =
                            it.lowercase()

                        n.contains("24/7") ||
                                n.contains("24x7") ||
                                n.contains("247") ||

                                n.contains("cine") ||
                                n.contains("canales") ||
                                n.contains("vod") ||
                                n.contains("tv") ||
                                n.contains("hbo") ||
                                n.contains("pluto") ||
                                n.contains("axn")
                    }

                } else {

                    map.keys.toList()
                }

            recyclerCategories.adapter =

                CategorySidebarAdapter(

                    listOf(

                        Pair(
                            "TODO",
                            map.values
                                .flatten()

                                .distinctBy {

                                    it.name
                                        .substringBefore(" S")
                                        .trim()
                                }

                                .size
                        ),

                        Pair(
                            "CONTINUAR VIENDO",
                            ContinueManager.getAll(this).filter { it.type == type }.size
                        )

                    ) +

                            keys.map {

                                Pair(

                                    it,

                                    map[it]

                                        ?.distinctBy { channel ->

                                            channel.name
                                                .substringBefore(" S")
                                                .trim()
                                        }

                                        ?.size ?: 0
                                )
                            }

                ) { selected ->

                    // 🔥 TODO
                    if (
                        selected == "TODO"
                    ) {

                        val allSeries =

                            map.values
                                .flatten()

                                .distinctBy {

                                    it.name
                                        .substringBefore(" S")
                                        .trim()
                                }

                        recycler.adapter =

                            ChannelNetflixAdapter(
                                allSeries,
                                this
                            ) { channel ->

                                val cleanName =

                                    channel.name
                                        .substringBefore(" S")
                                        .trim()

                                val intent =
                                    Intent(
                                        this,
                                        SeriesActivity::class.java
                                    )

                                intent.putExtra(
                                    "is_m3u",
                                    true
                                )

                                intent.putExtra(
                                    "serie_name",
                                    cleanName
                                )

                                startActivity(intent)
                            }

                        return@CategorySidebarAdapter
                    }

// 🔥 CONTINUAR VIENDO

                    if (
                        selected == "CONTINUAR VIENDO"
                    ) {

                        val continueItems =

                            ContinueManager
                                .getAll(this)
                                .filter { it.type == type }

                        recycler.adapter =

                            ChannelNetflixAdapter(

                                continueItems.map {

                                    Channel(
                                        it.name,
                                        it.url,
                                        it.logo
                                    )
                                },

                                this,
                                true

                            ) { channel ->

                                val intent =

                                    Intent(
                                        this,
                                        VideoPlayerActivity::class.java
                                    )

                                intent.putExtra(
                                    "url",
                                    channel.url
                                )
                                intent.putExtra(
                                    "type",

                                    continueItems.firstOrNull {
                                        it.url == channel.url
                                    }?.type ?: ""
                                )

                                intent.putExtra(
                                    "name",
                                    channel.name
                                )

                                intent.putExtra(
                                    "logo",
                                    channel.logo
                                )

                                startActivity(intent)
                            }

                        return@CategorySidebarAdapter
                    }

                    val list =
                        map[selected] ?: emptyList()

                    // ✅ SERIES M3U — ya viene agrupado de M3UContentResolver
                    if (
                        type == "series" &&
                        map === DataRepository.m3uSeriesData
                    ) {
                        val lowerCategory =
                            selected.lowercase()
                        // 🔥 IGNORAR 24/7
                        if (
                            lowerCategory.contains("24/7") ||
                            lowerCategory.contains("24x7") ||
                            lowerCategory.contains("247")
                        ) {
                            toast("Canal 24/7 detectado")
                            return@CategorySidebarAdapter
                        }
                        recycler.adapter =
                            ChannelNetflixAdapter(
                                list,
                                this
                            ) { channel ->
                                val intent =
                                    Intent(
                                        this,
                                        SeriesActivity::class.java
                                    )
                                intent.putExtra(
                                    "is_m3u",
                                    true
                                )
                                intent.putExtra(
                                    "serie_name",
                                    channel.name
                                )
                                startActivity(intent)
                            }
                    } else {
                        recycler.adapter =
                            ChannelNetflixAdapter(
                                list,
                                this
                            ) { channel ->
                                // 🔥 XTREAM SERIES
                                if (channel.url.startsWith("SERIES_")) {
                                    val seriesId =
                                        channel.url.removePrefix("SERIES_")
                                    val intent =
                                        Intent(
                                            this,
                                            SeriesActivity::class.java
                                        )
                                    intent.putExtra(
                                        "series_id",
                                        seriesId
                                    )
                                    startActivity(intent)
                                } else {

                                    // 🔥 LIVE / MOVIES
                                    val intent =
                                        Intent(
                                            this,
                                            VideoPlayerActivity::class.java
                                        )

                                    intent.putExtra(
                                        "url",
                                        channel.url
                                    )

                                    intent.putExtra(
                                        "type",
                                        type
                                    )

                                    intent.putExtra(
                                        "name",
                                        channel.name
                                    )

                                    intent.putExtra(
                                        "logo",
                                        channel.logo
                                    )

// 🔥 PLAYLIST COMPLETA
                                    intent.putStringArrayListExtra(

                                        "playlist",

                                        ArrayList(
                                            list.map {
                                                it.url
                                            }
                                        )
                                    )

// 🔥 POSICIÓN ACTUAL
                                    intent.putExtra(

                                        "index",

                                        list.indexOf(channel)
                                    )

                                    startActivity(intent)
                                }
                            }
                    }
                }

            // ❌ YA NO AUTO-CARGA NADA
        }

        private fun cambiarUsuario() {

            // 🔥 LIMPIAR MEMORIA GLIDE
            Glide.get(this).clearMemory()

            Thread {

                // 🔥 LIMPIAR DISK CACHE
                Glide.get(this).clearDiskCache()

            }.start()

            val prefs =
                getSharedPreferences(
                    "iptv",
                    MODE_PRIVATE
                )

            val accountIndex =
                prefs.getInt(
                    "current_account",
                    0
                )

            DataRepository.clearMemory()

            startActivity(
                Intent(
                    this,
                    AccountsActivity::class.java
                )
            )

            finish()
        }

        private fun goLogin() {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        private fun toast(msg: String) {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        fun fakeLoadXtream(
            host: String,
            user: String,
            pass: String,
            type: String
        ): Map<String, List<Channel>> {

            val client = OkHttpClient()

            val actionCat =
                if (type == "movies") "get_vod_categories" else "get_live_categories"
            val actionStreams = if (type == "movies") "get_vod_streams" else "get_live_streams"

            val categoryMap = mutableMapOf<String, String>()

            val catUrl = "$host/player_api.php?username=$user&password=$pass&action=$actionCat"
            val catBody =
                client.newCall(Request.Builder().url(catUrl).build()).execute().body?.string()
                    .orEmpty()

            if (catBody.isNotBlank()) {
                val arr = JSONArray(catBody)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    categoryMap[o.optString("category_id")] =
                        o.optString("category_name")
                }
            }

            val streamsUrl =
                "$host/player_api.php?username=$user&password=$pass&action=$actionStreams"

            val requestStreams = Request.Builder()
                .url(streamsUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Accept", "*/*")
                .build()

            val body = client.newCall(requestStreams)
                .execute()
                .body
                ?.string()
                .orEmpty()

            val array = JSONArray(body)

            val map = linkedMapOf<String, MutableList<Channel>>()

            for (i in 0 until array.length()) {

                val item = array.optJSONObject(i) ?: continue

                val name = item.optString("name")
                var logo =
                    item.optString("stream_icon")

// 🔥 LIMPIAR LOGOS BASURA
                if (
                    logo.isBlank() ||
                    logo == "null" ||
                    logo == "N/A"
                ) {

                    logo =
                        "https://via.placeholder.com/300x450.png?text=${name.replace(" ", "+")}"
                }

                if (logo.isEmpty()) {

                    logo =
                        TMDBHelper.getPoster(

                            name,

                            type == "series"
                        )
                }
                val id = item.optString("stream_id")
                val catId = item.optString("category_id")

                if (id.isEmpty()) continue

                val url = "$host/live/$user/$pass/$id.ts"
                val cat = categoryMap[catId] ?: "OTROS"

                map.getOrPut(cat) { mutableListOf() }
                    .add(Channel(name, url, logo))
            }

            return map
        }

        data class M3UChannel(
            val name: String,
            val url: String,
            val group: String,
            val logo: String
        )

        // 🔥 PONLO AQUÍ (ANTES DEL ÚLTIMO })
        override fun onBackPressed() {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

