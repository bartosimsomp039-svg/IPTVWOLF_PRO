package com.tuempresa.wolfiptv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.ImageView
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class AccountsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AccountAdapter
    private val list = mutableListOf<Account>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        recycler = findViewById(R.id.recyclerAccounts)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.setHasFixedSize(true)

        val btnAdd = findViewById<Button>(R.id.btnAdd)

        btnAdd.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        adapter = AccountAdapter(
            list,
            this,

            // 🔥 ENTRAR (MEJORADO)
            onClick = { account ->
                val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
                val index = list.indexOf(account)
                if (index != -1) {
                    // 🔥 RESET UPDATE MANAGER + CAMBIAR USUARIO
                    prefs.edit()
                        .remove("last_update")
                        .putInt("current_account", index)
                        .apply()
                    // 🔥 LIMPIAR MEMORIA (cache en disco se conserva por usuario)
                    DataRepository.clearMemory()
                    val intent =
                        Intent(
                            this,
                            HomeActivity::class.java
                        )
                    // 🔥 LIMPIA ACTIVITIES VIEJAS
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
            },

            // 🔥 ELIMINAR
            onDelete = { pos ->
                val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
                // ✅ Borrar caché de esa cuenta sin tocar las demás
                val cacheUsuario = pos.toString()
                Thread {
                    CacheManager.limpiarUsuario(this, cacheUsuario)
                }.start()
                list.removeAt(pos)

                val newArray = JSONArray()

                list.forEach {
                    val obj = JSONObject()
                    obj.put("type", it.type)
                    obj.put("host", it.host)
                    obj.put("user", it.user)
                    obj.put("pass", it.pass)
                    obj.put("url", it.url)
                    obj.put("name", it.name)
                    obj.put("avatar", it.avatar)
                    newArray.put(obj)
                }

                prefs.edit()
                    .putString("accounts", newArray.toString())
                    .apply()

                adapter.notifyItemRemoved(pos)
            },

            // 🔥 EDITAR
            onEdit = { account ->

                val dialogView =

                    layoutInflater.inflate(
                        R.layout.dialog_edit_account,
                        null
                    )

                val dialog =

                    android.app.AlertDialog.Builder(this)

                        .setView(dialogView)
                        .create()

                // 🔥 INPUTS
                val etName =
                    dialogView.findViewById<EditText>(
                        R.id.etNameEdit
                    )

                val etHost =
                    dialogView.findViewById<EditText>(
                        R.id.etHostEdit
                    )

                val etUser =
                    dialogView.findViewById<EditText>(
                        R.id.etUserEdit
                    )

                val etPass =
                    dialogView.findViewById<EditText>(
                        R.id.etPassEdit
                    )

                val etUrl =
                    dialogView.findViewById<EditText>(
                        R.id.etUrlEdit
                    )

                val btnSave =
                    dialogView.findViewById<Button>(
                        R.id.btnSaveEdit
                    )

                val btnAvatar =
                    dialogView.findViewById<Button>(
                        R.id.btnAvatarEdit
                    )

                // 🔥 RELLENAR DATOS
                etName.setText(account.name)

                etHost.setText(account.host)

                etUser.setText(account.user)

                etPass.setText(account.pass)

                etUrl.setText(account.url)

                // 🔥 OCULTAR SEGÚN TIPO
                if (account.type == "m3u") {

                    etHost.visibility = View.GONE
                    etUser.visibility = View.GONE
                    etPass.visibility = View.GONE

                } else {

                    etUrl.visibility = View.GONE
                }

                // 🔥 AVATAR
                var selectedAvatar =
                    account.avatar

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

                    lateinit var avatarDialog:
                            android.app.AlertDialog

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

                            Toast.makeText(
                                this,
                                "Avatar cambiado",
                                Toast.LENGTH_SHORT
                            ).show()

                            avatarDialog.dismiss()
                        }

                        layout.addView(img)
                    }

                    avatarDialog =

                        android.app.AlertDialog.Builder(this)

                            .setTitle("Elegir Avatar")

                            .setView(layout)

                            .setNegativeButton(
                                "Cerrar",
                                null
                            )

                            .create()

                    avatarDialog.show()
                }

                // 🔥 GUARDAR
                btnSave.setOnClickListener {

                    account.name =
                        etName.text.toString()

                    account.host =
                        etHost.text.toString()

                    account.user =
                        etUser.text.toString()

                    account.pass =
                        etPass.text.toString()

                    account.url =
                        etUrl.text.toString()

                    account.avatar =
                        selectedAvatar

                    val prefs =
                        getSharedPreferences(
                            "iptv",
                            MODE_PRIVATE
                        )

                    val newArray =
                        JSONArray()

                    list.forEach {

                        val obj =
                            JSONObject()

                        obj.put(
                            "type",
                            it.type
                        )

                        obj.put(
                            "host",
                            it.host
                        )

                        obj.put(
                            "user",
                            it.user
                        )

                        obj.put(
                            "pass",
                            it.pass
                        )

                        obj.put(
                            "url",
                            it.url
                        )

                        obj.put(
                            "name",
                            it.name
                        )

                        obj.put(
                            "avatar",
                            it.avatar
                        )

                        newArray.put(obj)
                    }

                    prefs.edit()

                        .putString(
                            "accounts",
                            newArray.toString()
                        )

                        .apply()

                    adapter.notifyDataSetChanged()
                    // ✅ Limpiar caché de esa cuenta porque las credenciales cambiaron
                    val pos = list.indexOf(account)
                    if (pos != -1) {
                        Thread {
                            CacheManager.limpiarUsuario(this, pos.toString())
                        }.start()
                    }
                    Toast.makeText(
                        this,
                        "Cuenta actualizada",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }

                dialog.show()
            }
        )

        recycler.adapter = adapter

        loadAccounts()
    }

    override fun onResume() {
        super.onResume()
        loadAccounts()
    }

    private fun loadAccounts() {

        val prefs = getSharedPreferences("iptv", MODE_PRIVATE)
        val json = prefs.getString("accounts", "[]") ?: "[]"

        list.clear()

        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            list.add(
                Account(
                    type = obj.optString("type"),
                    name = obj.optString("name"),
                    host = obj.optString("host"),
                    user = obj.optString("user"),
                    pass = obj.optString("pass"),
                    url = obj.optString("url"),
                    avatar = obj.optString("avatar")
                )
            )
        }

        adapter.notifyDataSetChanged()
    }
}