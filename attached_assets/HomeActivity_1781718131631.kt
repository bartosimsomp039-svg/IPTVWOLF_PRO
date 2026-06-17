package com.tuempresa.wolfiptv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import com.google.android.material.button.MaterialButton
import android.os.Handler
import android.os.Looper

class HomeActivity : AppCompatActivity() {

    lateinit var btnLive: View
    lateinit var btnMovies: View
    lateinit var btnSeries: View
    private lateinit var imgLivePoster: ImageView
    private lateinit var imgFeatured: ImageView

    private lateinit var imgMoviePoster: ImageView

    private lateinit var imgSeriesPoster: ImageView

    lateinit var txtStatusLive: TextView
    lateinit var txtStatusMovies: TextView
    lateinit var txtStatusSeries: TextView
    lateinit var loader: ProgressBar
    private lateinit var continueSection: View
    private lateinit var recyclerContinue: RecyclerView
    private lateinit var txtContinueTitle: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home_tv)
        // 🔥 FULL IPTV MODE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        imgSeriesPoster =
            findViewById(R.id.imgSeriesPoster)

        imgFeatured =
            findViewById(R.id.imgFeatured)

        loadDynamicHomePosters()

        // 🔥 VISTAS
        loader = findViewById(R.id.progressLoader)

        continueSection = findViewById(R.id.continueSection)
        recyclerContinue = findViewById(R.id.recyclerContinue)
        txtContinueTitle = findViewById(R.id.txtContinueTitle)
        recyclerContinue.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        txtStatusLive = findViewById(R.id.txtStatusLive)
        txtStatusMovies = findViewById(R.id.txtStatusMovies)
        txtStatusSeries = findViewById(R.id.txtStatusSeries)

        val txtUser = findViewById<TextView>(R.id.txtUser)
        val txtClock =
            findViewById<TextView>(R.id.txtClock)

        val time =
            java.text.SimpleDateFormat(
                "hh:mm a",
                java.util.Locale.getDefault()
            )

        txtClock.text =
            time.format(java.util.Date())

        btnLive = findViewById(R.id.btnLive)
        btnMovies = findViewById(R.id.btnMovies)
        btnSeries = findViewById(R.id.btnSeries)

        val btnUpdateLive =
            findViewById<MaterialButton>(R.id.btnUpdateLive)

        val btnUpdateMovies =
            findViewById<MaterialButton>(R.id.btnUpdateMovies)

        val btnUpdateSeries =
            findViewById<MaterialButton>(R.id.btnUpdateSeries)

        val recyclerMovies =
            findViewById<RecyclerView>(R.id.recyclerMovies)

        val recyclerSeries =
            findViewById<RecyclerView>(R.id.recyclerSeries)

        recyclerMovies.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )

        recyclerSeries.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )

        val btnChangeUser = findViewById<View>(R.id.btnChangeUser)
        val btnLogout = findViewById<View>(R.id.btnLogout)
        imgLivePoster =
            findViewById(R.id.imgLivePoster)

        imgMoviePoster =
            findViewById(R.id.imgMoviePoster)

        imgSeriesPoster =
            findViewById(R.id.imgSeriesPoster)

        val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
        val json = prefs.getString("accounts", null)
        val index = prefs.getInt("current_account", 0)

        if (json.isNullOrEmpty()) {
            startActivity(Intent(this, AccountsActivity::class.java))
            finish()
            return
        }

        val arr = JSONArray(json)

        if (arr.length() == 0) {
            startActivity(Intent(this, AccountsActivity::class.java))
            finish()
            return
        }

        val obj = arr.getJSONObject(index)

        val type = obj.optString("type")

        // ✅ NUEVO: Establecer usuario activo y cargar cache

        DataRepository.setCurrentUser(this, index.toString())

// Si isLoaded=true, el Thread de abajo NO llamará a la API ✅

        val user =

            obj.optString(
                "name",
                "Usuario"
            )

        txtUser.text = "Bienvenido $user"

        // ✅ NUEVO: mostrar horas de caché al arrancar
        actualizarHorasCache()

        // 🔥 UPDATE LIVE
        btnUpdateLive.setOnClickListener {

            animarBoton(it)

            actualizarSolo("live")
        }

// 🔥 UPDATE MOVIES
        btnUpdateMovies.setOnClickListener {

            animarBoton(it)

            actualizarSolo("movies")
        }

// 🔥 UPDATE SERIES
        btnUpdateSeries.setOnClickListener {

            animarBoton(it)

            actualizarSolo("series")
        }

        // 🔘 ENTRAR (SMART LOAD)
        btnLive.setOnClickListener {

            if (UpdateManager.necesitaActualizar(this)) {
                actualizarTodo { openMain("live") }
            } else {
                openMain("live")
            }
        }

        btnMovies.setOnClickListener {

            if (UpdateManager.necesitaActualizar(this)) {
                actualizarTodo { openMain("movies") }
            } else {
                openMain("movies")
            }
        }

        btnSeries.setOnClickListener {

            if (UpdateManager.necesitaActualizar(this)) {
                actualizarTodo { openMain("series") }
            } else {
                openMain("series")
            }
        }

        btnChangeUser.setOnClickListener {
            // 🔥 RESET UPDATE MANAGER
            getSharedPreferences(
                "iptv",
                MODE_PRIVATE
            )
                .edit()
                .remove("last_update")
                .apply()
            // 🔥 LIMPIAR MEMORIA (una sola línea reemplaza todo lo anterior)
            DataRepository.clearMemory()
            startActivity(
                Intent(
                    this,
                    AccountsActivity::class.java
                )
            )
            finish()
        }

        btnLogout.setOnClickListener {
            ContinueManager.clear(this)
            // 🔥 LIMPIAR MEMORIA + CACHE DEL USUARIO ACTUAL
            DataRepository.clearAll(this)
            // 🔥 ELIMINAR SOLO SESIÓN ACTUAL
            prefs.edit()
                .remove("current_account")
                .remove("last_update")
                .apply()
            Toast.makeText(
                this,
                "Sesión cerrada",
                Toast.LENGTH_SHORT
            ).show()
            goLogin()
            finish()
        }

        // 🔥 HOME RÁPIDA IPTV PREMIUM

        txtStatusLive.text = "⚡ Disponible"

        txtStatusMovies.text = "⚡ Disponible"

        txtStatusSeries.text = "⚡ Disponible"


        // 🔥 CARGA INTELIGENTE IPTV
        Thread {

            try {

                // 🔥 SI NO HAY DATA → CARGAR
                if (!DataRepository.isLoaded) {

                    preloadContent()
                }

                // 🔥 SI PASARON 2 DÍAS → ACTUALIZAR
                else if (
                    UpdateManager.necesitaActualizar(this)
                ) {

                    preloadContent()

                    UpdateManager
                        .guardarUltimaActualizacion(this)
                }

                // ✅ Procesar listas en hilo de fondo (NO en UI thread)
                val movies = try {
                    DataRepository.moviesData
                        ?.values
                        ?.flatten()
                        ?.shuffled()
                        ?.take(10)
                        ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val series = try {
                    DataRepository.seriesData
                        ?.values
                        ?.flatten()
                        ?.shuffled()
                        ?.take(10)
                        ?: emptyList()
                } catch (_: Exception) { emptyList() }

                runOnUiThread {

                    // 🔥 POSTERS DINÁMICOS
                    loadDynamicHomePosters()

                    try {

                        recyclerMovies.adapter =
                            RecentAdapter(movies)

                        recyclerSeries.adapter =
                            RecentAdapter(series)

                    } catch (_: Exception) {
                    }
                }

            } catch (_: Exception) {
            }


        }.start()
    }

    // 🔄 LOADER
    fun mostrarLoader(mostrar: Boolean) {
        loader.visibility = if (mostrar) View.VISIBLE else View.GONE
    }

    private fun openMain(type: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("type", type)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    // 🔁 ACTUALIZAR TODO
    fun actualizarTodo(onFinish: () -> Unit) {

        btnLive.isEnabled = false
        btnMovies.isEnabled = false
        btnSeries.isEnabled = false

        mostrarLoader(true)

        Thread(Runnable {

            try {

                val prefs =
                    getSharedPreferences(
                        "iptv",
                        MODE_PRIVATE
                    )

                val json =
                    prefs.getString(
                        "accounts",
                        null
                    ) ?: return@Runnable

                val typeToken =
                    object :
                        com.google.gson.reflect.TypeToken<
                                MutableList<Account>
                                >() {}.type

                val list: MutableList<Account> =
                    com.google.gson.Gson()
                        .fromJson(json, typeToken)

                val acc =
                    list[
                        prefs.getInt(
                            "current_account",
                            0
                        )
                    ]

                val isM3U =
                    acc.type == "m3u"

                // 🔥 M3U
                if (isM3U) {

                    val parsed =
                        M3UParser.parse(acc.url)

                    M3UContentResolver
                        .resolve(parsed)

                    Log.e(
                        "RESOLVER",
                        "LIVE=" + DataRepository.m3uLiveData.size
                    )

                    Log.e(
                        "RESOLVER",
                        "MOVIES=" + DataRepository.m3uMoviesData.size
                    )

                    Log.e(
                        "RESOLVER",
                        "SERIES=" + DataRepository.m3uSeriesData.size
                    )
                    // ✅ FIX: Guardar caché M3U después de actualizar
                    guardarCacheM3U()
                    // ✅ FIX SERIES: marcador liviano para que MainActivity no fuerce recarga
                    val accountIdx = prefs.getInt("current_account", 0)
                    if (DataRepository.m3uSeriesData.isNotEmpty()) {
                        CacheManager.guardar(this, DataRepository.currentUser, "series_episodes", "ready")
                    }
                } else {

                    // 🔥 XTREAM
                    DataRepository.liveData =
                        XtreamLoader.load(
                            acc.host,
                            acc.user,
                            acc.pass,
                            "live"
                        )

                    DataRepository.moviesData =
                        XtreamLoader.load(
                            acc.host,
                            acc.user,
                            acc.pass,
                            "movies"
                        )

                    DataRepository.seriesData =
                        XtreamLoader.load(
                            acc.host,
                            acc.user,
                            acc.pass,
                            "series"
                        )

                    // ✅ Guardar cache Xtream en disco
                    guardarCacheXtream()
                }

                // ✅ Marcar como cargado para que el Thread de inicio no re-descargue
                DataRepository.isLoaded = true

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Error al actualizar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            runOnUiThread {

                mostrarLoader(false)

                btnLive.isEnabled = true
                btnMovies.isEnabled = true
                btnSeries.isEnabled = true

                UpdateManager
                    .guardarUltimaActualizacion(this)

                actualizarHorasCache()
                loadDynamicHomePosters()

                onFinish()
            }

        }).start()
    }

    // 🔄 ACTUALIZAR SOLO UNA SECCIÓN
    fun actualizarSolo(tipo: String) {

        // ── Mapear tipo → vistas de esa tarjeta ──────────────────────────────
        val btnUpdate: MaterialButton = when (tipo) {
            "live"   -> findViewById(R.id.btnUpdateLive)
            "movies" -> findViewById(R.id.btnUpdateMovies)
            else     -> findViewById(R.id.btnUpdateSeries)
        }
        val txtStatus: TextView = when (tipo) {
            "live"   -> txtStatusLive
            "movies" -> txtStatusMovies
            else     -> txtStatusSeries
        }

        // ── Deshabilitar solo ESE botón ───────────────────────────────────────
        btnUpdate.isEnabled = false
        val textoOriginal = btnUpdate.text.toString()

        // ── Animación de puntos en el botón ──────────────────────────────────
        val dotHandler = Handler(Looper.getMainLooper())
        var dotCount = 0
        val dotRunnable = object : Runnable {
            override fun run() {
                dotCount = (dotCount + 1) % 4
                btnUpdate.text = "Actualizando" + ".".repeat(dotCount)
                dotHandler.postDelayed(this, 400)
            }
        }
        dotHandler.post(dotRunnable)

        Thread(Runnable {
            var isM3U = false
            var huboError = false

            try {
                val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
                val json  = prefs.getString("accounts", null) ?: return@Runnable
                val tt    = object : com.google.gson.reflect.TypeToken<MutableList<Account>>() {}.type
                val list: MutableList<Account> = com.google.gson.Gson().fromJson(json, tt)
                val acc   = list[prefs.getInt("current_account", 0)]
                isM3U     = acc.type == "m3u"

                if (isM3U) {
                    // ══ M3U: re-parsear el archivo completo (no hay forma de cargar solo una sección)

                    // Paso 1 — Descargando
                    runOnUiThread {
                        txtStatusLive.text   = "⬇ Descargando M3U..."
                        txtStatusMovies.text = "⬆ En cola..."
                        txtStatusSeries.text = "⬆ En cola..."
                    }

                    val parsed = M3UParser.parse(acc.url)

                    // Paso 2 — Clasificando
                    runOnUiThread {
                        txtStatusLive.text   = "🔀 Clasificando..."
                        txtStatusMovies.text = "🔀 Clasificando..."
                        txtStatusSeries.text = "🔀 Clasificando..."
                    }

                    M3UContentResolver.resolve(parsed)

                    // Paso 3 — Guardando cache
                    runOnUiThread {
                        txtStatusLive.text   = "💾 Guardando..."
                        txtStatusMovies.text = "💾 Guardando..."
                        txtStatusSeries.text = "💾 Guardando..."
                    }

                    guardarCacheM3U()
                    // ✅ FIX SERIES: marcador liviano para que MainActivity no fuerce recarga
                    val accountIdx = prefs.getInt("current_account", 0)
                    if (DataRepository.m3uSeriesData.isNotEmpty()) {
                        CacheManager.guardar(this, DataRepository.currentUser,
                            "${accountIdx}_series_episodes", "ready")
                    }

                    // Paso 4 — Mostrar conteos reales
                    val cLive   = DataRepository.m3uLiveData.values.sumOf { it.size }
                    val cMovies = DataRepository.m3uMoviesData.values.sumOf { it.size }
                    val cSeries = DataRepository.m3uSeriesData.values.sumOf { it.size }
                    runOnUiThread {
                        txtStatusLive.text   = "✅ $cLive canales"
                        txtStatusMovies.text = "✅ $cMovies películas"
                        txtStatusSeries.text = "✅ $cSeries series"
                    }

                } else {
                    // ══ XTREAM: cargar SOLO la sección que pidió el usuario

                    runOnUiThread { txtStatus.text = "⬇ Descargando..." }

                    when (tipo) {
                        "live" -> {
                            DataRepository.liveData =
                                XtreamLoader.load(acc.host, acc.user, acc.pass, "live")
                            val count = DataRepository.liveData?.values?.sumOf { it.size } ?: 0
                            runOnUiThread { txtStatus.text = "✅ $count canales" }
                        }
                        "movies" -> {
                            DataRepository.moviesData =
                                XtreamLoader.load(acc.host, acc.user, acc.pass, "movies")
                            val count = DataRepository.moviesData?.values?.sumOf { it.size } ?: 0
                            runOnUiThread { txtStatus.text = "✅ $count películas" }
                        }
                        "series" -> {
                            DataRepository.seriesData =
                                XtreamLoader.load(acc.host, acc.user, acc.pass, "series")
                            val count = DataRepository.seriesData?.values?.sumOf { it.size } ?: 0
                            runOnUiThread { txtStatus.text = "✅ $count series" }
                        }
                    }

                    guardarCacheXtream()
                }

            } catch (e: Exception) {
                huboError = true
                runOnUiThread { txtStatus.text = "❌ Error al actualizar" }
            }

            // ── Restaurar botón siempre, haya error o no ─────────────────────
            runOnUiThread {
                dotHandler.removeCallbacksAndMessages(null)
                btnUpdate.text      = textoOriginal
                btnUpdate.isEnabled = true

                if (!huboError) {
                    UpdateManager.guardarUltimaActualizacion(this)
                    actualizarHorasCache()
                    loadDynamicHomePosters()
                }
            }

        }).start()
    }

    // 🔄 ANIMACIÓN 🔥
    fun animarBoton(view: View) {
        view.animate().rotationBy(360f).setDuration(500).start()
    }

    fun preloadContent() {
        // Helper para actualizar estado de cada tarjeta desde hilo background
        fun setStatus(txt: TextView, msg: String) = runOnUiThread { txt.text = msg }

        try {
            val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
            val json  = prefs.getString("accounts", null) ?: return
            val type  = object : com.google.gson.reflect.TypeToken<MutableList<Account>>() {}.type
            val list: MutableList<Account> = com.google.gson.Gson().fromJson(json, type)
            if (list.isEmpty()) return
            val acc   = list[prefs.getInt("current_account", 0)]
            val isM3U = acc.type == "m3u"

            if (isM3U) {
                // ══ M3U — una sola descarga que clasifica los 3 tipos ══════════

                // 1. Indicar que los 3 están descargando
                setStatus(txtStatusLive,   "⬇ Descargando M3U...")
                setStatus(txtStatusMovies, "⬇ Descargando M3U...")
                setStatus(txtStatusSeries, "⬇ Descargando M3U...")

                // 2. Parsear (bloqueante — es la descarga real)
                val parsed = M3UParser.parse(acc.url)

                // 3. Clasificar en live / movies / series
                setStatus(txtStatusLive,   "🔀 Clasificando...")
                setStatus(txtStatusMovies, "🔀 Clasificando...")
                setStatus(txtStatusSeries, "🔀 Clasificando...")
                M3UContentResolver.resolve(parsed)

                // 4. Guardar cache
                setStatus(txtStatusLive,   "💾 Guardando...")
                setStatus(txtStatusMovies, "💾 Guardando...")
                setStatus(txtStatusSeries, "💾 Guardando...")
                guardarCacheM3U()
                // ✅ FIX SERIES: marcador liviano para que MainActivity no fuerce recarga
                val accountIdx = prefs.getInt("current_account", 0)
                if (DataRepository.m3uSeriesData.isNotEmpty()) {
                    CacheManager.guardar(this, DataRepository.currentUser,
                        "${accountIdx}_series_episodes", "ready")
                }

                // 5. Mostrar conteo real por tarjeta
                val liveCount   = DataRepository.m3uLiveData.values.sumOf { it.size }
                val moviesCount = DataRepository.m3uMoviesData.values.sumOf { it.size }
                val seriesCount = DataRepository.m3uSeriesData.values.sumOf { it.size }
                setStatus(txtStatusLive,   "✅ $liveCount canales")
                setStatus(txtStatusMovies, "✅ $moviesCount películas")
                setStatus(txtStatusSeries, "✅ $seriesCount series")

            } else {
                // ══ XTREAM — cada sección carga y reporta por separado ═════════

                // ── LIVE ──
                setStatus(txtStatusLive, "⬇ Cargando canales...")
                if (DataRepository.liveData == null) {
                    DataRepository.liveData = XtreamLoader.load(acc.host, acc.user, acc.pass, "live")
                }
                val liveCount = DataRepository.liveData?.values?.sumOf { it.size } ?: 0
                setStatus(txtStatusLive, "✅ $liveCount canales")

                // ── PELÍCULAS ──
                setStatus(txtStatusMovies, "⬇ Cargando películas...")
                if (DataRepository.moviesData == null) {
                    DataRepository.moviesData = XtreamLoader.load(acc.host, acc.user, acc.pass, "movies")
                }
                val moviesCount = DataRepository.moviesData?.values?.sumOf { it.size } ?: 0
                setStatus(txtStatusMovies, "✅ $moviesCount películas")

                // Precarga de posters en background (no bloquea)
                preloadXtreamPosters()

                // ── SERIES ──
                setStatus(txtStatusSeries, "⬇ Cargando series...")
                if (DataRepository.seriesData == null) {
                    DataRepository.seriesData = XtreamLoader.load(acc.host, acc.user, acc.pass, "series")
                }
                val seriesCount = DataRepository.seriesData?.values?.sumOf { it.size } ?: 0
                setStatus(txtStatusSeries, "✅ $seriesCount series")

                // Guardar cache Xtream
                guardarCacheXtream()
            }

            // ── Finalizar ──────────────────────────────────────────────────────
            DataRepository.isLoaded = true
            UpdateManager.guardarUltimaActualizacion(this)
            runOnUiThread {
                loadDynamicHomePosters()
                actualizarHorasCache()
            }

        } catch (e: Exception) {
            setStatus(txtStatusLive,   "❌ Error al cargar")
            setStatus(txtStatusMovies, "❌ Error al cargar")
            setStatus(txtStatusSeries, "❌ Error al cargar")
        }
    }

    fun preloadXtreamPosters() {
        runOnUiThread {
            try {
                DataRepository.moviesData?.values?.flatten()?.take(40)?.forEach { channel ->
                    val logo = channel.logo ?: ""
                    if (logo.startsWith("http")) {
                        Glide.with(this)
                            .load(logo)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload(90, 135)
                    }
                }
                DataRepository.seriesData?.values?.flatten()?.take(40)?.forEach { channel ->
                    val logo = channel.logo ?: ""
                    if (logo.startsWith("http")) {
                        Glide.with(this)
                            .load(logo)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload(90, 135)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadDynamicHomePosters() {

        try {

            val prefs =
                getSharedPreferences(
                    "iptv",
                    MODE_PRIVATE
                )

            val json =
                prefs.getString(
                    "accounts",
                    null
                ) ?: return

            val arr =
                JSONArray(json)

            val index =
                prefs.getInt(
                    "current_account",
                    0
                )

            val obj =
                arr.getJSONObject(index)

            val isM3U =
                obj.optString("type") == "m3u"

            // 🔥 MOVIES
            val movie =

                if (isM3U)

                    DataRepository.m3uMoviesData
                        .values
                        .flatten()
                        .randomOrNull()
                else

                    DataRepository.moviesData
                        ?.values
                        ?.flatten()
                        ?.randomOrNull()

            Log.d(
                "FEATURED_DEBUG",
                "Movie = ${movie?.name} | Logo = ${movie?.logo}"
            )

            // 🔥 FEATURED GRANDE
            movie?.logo?.let { logo ->

                if (logo.startsWith("http")) {

                    Glide.with(this)
                        .load(logo)
                        .centerCrop()
                        .into(imgFeatured)
                }
            }

            // 🔥 CARD MOVIES
            movie?.logo?.let { logo ->

                if (logo.startsWith("http")) {

                    Glide.with(this)
                        .load(logo)
                        .centerCrop()
                        .into(imgMoviePoster)
                }
            }

            // 🔥 SERIES
            val series =

                if (isM3U)

                    DataRepository.m3uSeriesData
                        .values
                        .flatten()
                        .randomOrNull()
                else

                    DataRepository.seriesData
                        ?.values
                        ?.flatten()
                        ?.randomOrNull()

            series?.logo?.let { logo ->

                if (logo.startsWith("http")) {

                    Glide.with(this)
                        .load(logo)
                        .centerCrop()
                        .into(imgSeriesPoster)
                }
            }

            // 🔥 LIVE
            val live =

                if (isM3U)

                    DataRepository.m3uLiveData
                        .values
                        .flatten()
                        .randomOrNull()
                else

                    DataRepository.liveData
                        ?.values
                        ?.flatten()
                        ?.randomOrNull()

            live?.logo?.let { logo ->

                if (logo.startsWith("http")) {

                    Glide.with(this)
                        .load(logo)
                        .centerCrop()
                        .into(imgLivePoster)
                }
            }

        } catch (_: Exception) {
        }
    }

    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // Pequeño delay para que VideoPlayerActivity termine de guardar antes de leer
        recyclerContinue.postDelayed({ loadContinueSection() }, 300)
        // Actualizar horas de caché al volver a HomeActivity
        actualizarHorasCache()
    }
    private fun loadContinueSection() {
        val items = ContinueManager.getAll(this).toMutableList()
        if (items.isEmpty()) {
            continueSection.visibility = View.GONE
        } else {
            continueSection.visibility = View.VISIBLE
            txtContinueTitle.text = "▶ CONTINUAR VIENDO (${items.size})"
            recyclerContinue.adapter = ContinueAdapter(this, items) { count ->
                txtContinueTitle.text = "▶ CONTINUAR VIENDO ($count)"
                if (count == 0) continueSection.visibility = View.GONE
            }
        }
    }

    // ⏳ HORAS DE CACHÉ RESTANTES
    fun actualizarHorasCache() {
        val user = DataRepository.currentUser
        if (user.isEmpty()) return
        // ✅ Detectar tipo de cuenta para usar la clave correcta
        val isM3U = try {
            val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
            val json  = prefs.getString("accounts", null)
            val type  = object : com.google.gson.reflect.TypeToken<MutableList<Account>>() {}.type
            val list: MutableList<Account> = com.google.gson.Gson().fromJson(json, type)
            list[prefs.getInt("current_account", 0)].type == "m3u"
        } catch (e: Exception) { false }
        val liveKey   = if (isM3U) "m3u_live"   else "live"
        val moviesKey = if (isM3U) "m3u_movies" else "movies"
        val seriesKey = if (isM3U) "m3u_series" else "series"
        txtStatusLive.text = if (CacheManager.estaVigente(this, user, liveKey))
            "✅ Caché: ${CacheManager.horasRestantes(this, user, liveKey)}h"
        else "⚡ Sin caché"
        txtStatusMovies.text = if (CacheManager.estaVigente(this, user, moviesKey))
            "✅ Caché: ${CacheManager.horasRestantes(this, user, moviesKey)}h"
        else "⚡ Sin caché"
        txtStatusSeries.text = if (CacheManager.estaVigente(this, user, seriesKey))
            "✅ Caché: ${CacheManager.horasRestantes(this, user, seriesKey)}h"
        else "⚡ Sin caché"
    }

    // 💾 Guarda los 3 tipos M3U
    private fun guardarCacheM3U() {
        val gson = com.google.gson.Gson()
        val user = DataRepository.currentUser
        // ✅ Claves distintas para M3U — no pisamos las de Xtream
        CacheManager.guardar(this, user, "m3u_live",   gson.toJson(DataRepository.m3uLiveData))
        CacheManager.guardar(this, user, "m3u_movies", gson.toJson(DataRepository.m3uMoviesData))
        CacheManager.guardar(this, user, "m3u_series", gson.toJson(DataRepository.m3uSeriesData))
    }

    // 💾 Guarda los 3 tipos Xtream
    private fun guardarCacheXtream() {
        val gson = com.google.gson.Gson()
        val user = DataRepository.currentUser
        DataRepository.liveData?.let   { CacheManager.guardar(this, user, "live",   gson.toJson(it)) }
        DataRepository.moviesData?.let { CacheManager.guardar(this, user, "movies", gson.toJson(it)) }
        DataRepository.seriesData?.let { CacheManager.guardar(this, user, "series", gson.toJson(it)) }
    }
    }
