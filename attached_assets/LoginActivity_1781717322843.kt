package com.tuempresa.wolfiptv

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.app.AlertDialog
import android.widget.GridLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("iptv", MODE_PRIVATE)

        val etHost = findViewById<EditText>(R.id.etHost)
        val etUser = findViewById<EditText>(R.id.etUser)
        val layoutUser =findViewById<LinearLayout>(R.id.layoutUser)
        val etPass = findViewById<EditText>(R.id.etPass)
        val layoutPass =findViewById<LinearLayout>(R.id.layoutPass)
        val etM3u = findViewById<EditText>(R.id.etM3u)
        val etName = findViewById<EditText>(R.id.etName)

        val tabXtream = findViewById<LinearLayout>(R.id.tabXtream)
        val tabM3u = findViewById<LinearLayout>(R.id.tabM3u)

        var isM3uMode = false

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnUsuarios = findViewById<View>(R.id.btnUsuarios)

        val eye = findViewById<ImageView>(R.id.eyeIcon)
        val btnAvatar =
            findViewById<Button>(R.id.btnAvatar)
        val btnBuscarM3u =
            findViewById<Button>(R.id.btnBuscarM3u)

        // =========================
// 🔥 BUSCAR ARCHIVO M3U
// =========================

        btnBuscarM3u.setOnClickListener {

            val intent = Intent(Intent.ACTION_GET_CONTENT)

            intent.type = "*/*"

            intent.addCategory(Intent.CATEGORY_OPENABLE)

            startActivityForResult(

                Intent.createChooser(
                    intent,
                    "Seleccionar M3U"
                ),

                1001
            )
        }

        // 🔥 AVATAR SELECCIONADO
        var selectedAvatar = "avatar_1"

        // 🔥 MODO EDITAR
        val isEdit =
            intent.getBooleanExtra(
                "edit",
                false
            )

        if (isEdit) {

            val type =
                intent.getStringExtra("type")
                    ?: "xtream"

            etName.setText(
                intent.getStringExtra("name")
                    ?: ""
            )

            selectedAvatar =
                intent.getStringExtra("avatar")
                    ?: "avatar_1"

            // =========================
            // 🔥 MODO M3U
            // =========================
            if (type == "m3u") {

                isM3uMode = true

                tabM3u.setBackgroundResource(
                    R.drawable.bg_tab_selected
                )

                tabXtream.setBackgroundColor(
                    android.graphics.Color.TRANSPARENT
                )

                etHost.visibility = View.GONE
                layoutUser.visibility = View.GONE
                layoutPass.visibility = View.GONE
                etM3u.visibility = View.VISIBLE

                eye.visibility = View.GONE

                etM3u.setText(
                    intent.getStringExtra("url")
                        ?: ""
                )

            } else {

                // =========================
                // 🔥 MODO XTREAM
                // =========================

                isM3uMode = false

                tabXtream.setBackgroundResource(
                    R.drawable.bg_tab_selected
                )

                tabM3u.setBackgroundColor(
                    android.graphics.Color.TRANSPARENT
                )

                etHost.visibility = View.VISIBLE
                etUser.visibility = View.VISIBLE
                layoutPass.visibility = View.VISIBLE
                etM3u.visibility = View.GONE

                btnBuscarM3u.visibility = View.GONE

                eye.visibility = View.VISIBLE

                etHost.setText(
                    intent.getStringExtra("host")
                        ?: ""
                )

                etUser.setText(
                    intent.getStringExtra("user")
                        ?: ""
                )

                etPass.setText(
                    intent.getStringExtra("pass")
                        ?: ""
                )
            }

            btnLogin.text =
                "GUARDAR CAMBIOS"
        }

        btnAvatar.setOnClickListener {

            val avatars = arrayOf(

                "avatar_1",
                "avatar_2",
                "avatar_3",
                "avatar_4",
                "avatar_5",

                "avatar_6",
                "avatar_7",
                "avatar_8",
                "avatar_9",
                "avatar_10",
                "avatar_11",
                "avatar_12",
                "avatar_13",
                "avatar_14",
                "avatar_15",
                "avatar_16",
                "avatar_17",
                "avatar_18",
                "avatar_19",
                "avatar_20",
                "avatar_21",
                "avatar_22"
            )

            val layout =
                GridLayout(this)

            layout.columnCount = 3

            layout.setPadding(
                30,
                30,
                30,
                30
            )

            lateinit var dialog: AlertDialog
            avatars.forEach { avatar ->

                val img =
                    ImageView(this)

                val resId =
                    resources.getIdentifier(
                        avatar,
                        "drawable",
                        packageName
                    )

                img.setImageResource(resId)

                val params =
                    GridLayout.LayoutParams()

                params.width = 220
                params.height = 220

                params.setMargins(
                    20,
                    20,
                    20,
                    20
                )

                img.layoutParams =
                    params

                img.setOnClickListener {

                    selectedAvatar =
                        avatar

                    btnAvatar.text =
                        "✅ Avatar Seleccionado"

                    Toast.makeText(
                        this,
                        "Avatar elegido",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }

                layout.addView(img)
            }

            dialog =

                AlertDialog.Builder(this)

                    .setTitle("Selecciona un Avatar")

                    .setView(layout)

                    .setNegativeButton(
                        "Cerrar",
                        null
                    )

                    .create()

            dialog.show()
        }

        // =========================
        // 🔥 BOTÓN USUARIOS
        // =========================
        btnUsuarios.setOnClickListener {
            startActivity(Intent(this, AccountsActivity::class.java))
        }

        // =========================
// 🔥 NUEVO SISTEMA TABS
// =========================

        tabXtream.setBackgroundResource(
            R.drawable.bg_tab_selected
        )

        etHost.visibility = View.VISIBLE

        layoutUser.visibility = View.VISIBLE

        layoutPass.visibility = View.VISIBLE

        etM3u.visibility = View.GONE

        btnBuscarM3u.visibility = View.GONE

        eye.visibility = View.VISIBLE

// =========================
// 🔥 XTREAM
// =========================

        tabXtream.setOnClickListener {

            isM3uMode = false

            tabXtream.setBackgroundResource(
                R.drawable.bg_tab_selected
            )

            tabM3u.background = null

            etHost.visibility = View.VISIBLE

            layoutUser.visibility = View.VISIBLE

            layoutPass.visibility = View.VISIBLE

            etM3u.visibility = View.GONE

            btnBuscarM3u.visibility = View.GONE

            eye.visibility = View.VISIBLE
        }

// =========================
// 🔥 M3U
// =========================

        tabM3u.setOnClickListener {

            isM3uMode = true

            tabM3u.setBackgroundResource(
                R.drawable.bg_tab_selected
            )

            tabXtream.background = null

            etHost.visibility = View.GONE

            layoutUser.visibility = View.GONE

            layoutPass.visibility = View.GONE

            etM3u.visibility = View.VISIBLE

            btnBuscarM3u.visibility = View.VISIBLE

            eye.visibility = View.GONE
        }

        // =========================
        // 👁️ CONTRASEÑA
        // =========================
        var visible = false
        eye.setOnClickListener {
            visible = !visible

            etPass.inputType =
                if (visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            etPass.setSelection(etPass.text.length)
        }

        // =========================
        // 🔥 LOGIN
        // =========================
        btnLogin.setOnClickListener {

            btnLogin.isEnabled = false
            btnLogin.text = "Conectando..."

            val arr = JSONArray(prefs.getString("accounts", "[]"))

            // =========================
            // 🔥 M3U
            // =========================
            if (isM3uMode) {

                val url = etM3u.text.toString().trim()
                val name = etName.text.toString().trim()

                if (url.isEmpty()) {
                    toast("Ingresa URL M3U")
                    reset(btnLogin)
                    return@setOnClickListener
                }

                var indexExistente = -1

                for (i in 0 until arr.length()) {
                    val existing = arr.getJSONObject(i)
                    if (existing.optString("url") == url) {
                        indexExistente = i
                        break
                    }
                }

                if (indexExistente != -1) {
                    prefs.edit().putInt("current_account", indexExistente).apply()
                    toast("Entrando con lista guardada")
                    goHome()
                    return@setOnClickListener
                }

                val obj = JSONObject()
                obj.put("type", "m3u")
                obj.put("url", url)
                obj.put("name", if (name.isEmpty()) "M3U User" else name)
                // 🔥 AVATAR
                obj.put(
                    "avatar",
                    selectedAvatar
                )

                arr.put(obj)

                prefs.edit()
                    .putString("accounts", arr.toString())
                    .putInt("current_account", arr.length() - 1)
                    .apply()

                toast("Lista añadida correctamente")
                goHome()
                return@setOnClickListener
            }

            // =========================
            // 🔥 XTREAM
            // =========================
            val host = etHost.text.toString().trim()
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                toast("Completa todos los campos")
                reset(btnLogin)
                return@setOnClickListener
            }

            var indexExistente = -1

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("user") == user && o.optString("host") == host) {
                    indexExistente = i
                    break
                }
            }

            if (indexExistente != -1) {
                prefs.edit().putInt("current_account", indexExistente).apply()
                toast("Entrando con usuario guardado")
                goHome()
                return@setOnClickListener
            }

            val obj = JSONObject()
            obj.put("type", "xtream")
            obj.put("host", host)
            obj.put("user", user)
            obj.put("pass", pass)

            // 🔥 NOMBRE PERSONALIZADO
            obj.put(
                "name",
                etName.text.toString()
            )
            // 🔥 AVATAR
            obj.put(
                "avatar",
                selectedAvatar
            )

            arr.put(obj)

            prefs.edit()
                .putString("accounts", arr.toString())
                .putInt("current_account", arr.length() - 1)
                .apply()

            toast("Usuario añadido correctamente")
            goHome()
        }
    }

    private fun goHome() {
        // Leer la cuenta activa recién guardada
        val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
        val accounts = JSONArray(prefs.getString("accounts", "[]"))
        val index = prefs.getInt("current_account", 0)
        if (accounts.length() > index) {
            val cuenta = accounts.getJSONObject(index)
            // Crear un ID único por cuenta (host+user para Xtream, url para M3U)
            val userId = if (cuenta.optString("type") == "m3u")
                cuenta.optString("url")
            else
                "${cuenta.optString("host")}__${cuenta.optString("user")}"
            val cacheValida = DataRepository.setCurrentUser(this, userId)
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("cache_valida", cacheValida)  // HomeActivity sabrá si cargar API o no
            startActivity(intent)
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }

    private fun reset(btn: Button) {
        btn.isEnabled = true
        btn.text = "INICIAR SESIÓN"
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

// =========================
// 🔥 RESULTADO ARCHIVO M3U
// =========================

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == 1001 &&
            resultCode == RESULT_OK
        ) {

            val uri = data?.data ?: return

            val etM3u =
                findViewById<EditText>(R.id.etM3u)

            etM3u.setText(uri.toString())

            toast("Archivo M3U seleccionado")
        }
    }
}