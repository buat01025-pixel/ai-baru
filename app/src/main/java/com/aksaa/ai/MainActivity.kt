package com.aksaa.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Bg = Color(0xFF090B14)
private val Panel = Color(0xFF151A2A)
private val Accent = Color(0xFF8B7CFF)
private val TextMain = Color(0xFFF4F4FA)
private val TextMuted = Color(0xFFA8ADC2)

data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aksaa_chat", MODE_PRIVATE)
        setContent {
            var messages by remember {
                mutableStateOf(
                    prefs.getString("history", null)?.let { saved ->
                        runCatching {
                            val a = JSONArray(saved)
                            List(a.length()) { i ->
                                val o = a.getJSONObject(i)
                                ChatMessage(o.getString("text"), o.getBoolean("user"))
                            }
                        }.getOrNull()
                    } ?: listOf(ChatMessage("Halo! Aku AKSAA AI. Ada yang bisa aku bantu?", false))
                )
            }
            var input by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            fun persist(list: List<ChatMessage>) {
                val a = JSONArray()
                list.forEach { m ->
                    a.put(JSONObject().put("text", m.text).put("user", m.fromUser))
                }
                prefs.edit().putString("history", a.toString()).apply()
            }

            MaterialTheme(colorScheme = darkColorScheme(
                background = Bg, surface = Panel, primary = Accent,
                onBackground = TextMain, onSurface = TextMain
            )) {
                Column(
                    Modifier.fillMaxSize().background(Bg).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).background(Accent, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AKSAA AI", color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("Asisten pribadi • Online", color = TextMuted, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            messages = listOf(ChatMessage("Percakapan baru. Ada yang bisa aku bantu?", false))
                            persist(messages)
                            error = null
                        }) { Text("Baru", color = Accent) }
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { msg ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                            ) {
                                Column(
                                    Modifier.widthIn(max = 310.dp)
                                        .background(if (msg.fromUser) Color(0xFF302B59) else Panel,
                                            RoundedCornerShape(18.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(if (msg.fromUser) "Kamu" else "AKSAA AI",
                                        color = if (msg.fromUser) Color(0xFFD7D1FF) else Accent,
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(5.dp))
                                    Text(msg.text, color = TextMain, fontSize = 15.sp, lineHeight = 21.sp)
                                }
                            }
                        }
                        if (loading) item {
                            Text("AKSAA AI sedang berpikir…", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                    error?.let {
                        Text(it, color = Color(0xFFFF8A8A), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = input, onValueChange = { input = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp),
                            textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                            decorationBox = { inner ->
                                if (input.isEmpty()) Text("Tulis pesan...", color = TextMuted)
                                inner()
                            }
                        )
                        Button(
                            enabled = input.isNotBlank() && !loading,
                            onClick = {
                                val prompt = input.trim()
                                input = ""
                                val updated = messages + ChatMessage(prompt, true)
                                messages = updated
                                persist(updated)
                                loading = true
                                error = null
                                Thread {
                                    val result = callAi(updated)
                                    runOnUiThread {
                                        messages = messages + ChatMessage(result.first, false)
                                        persist(messages)
                                        if (!result.second.isNullOrBlank()) error = result.second
                                        loading = false
                                    }
                                }.start()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                        ) { Text("Kirim", color = Color.White) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Jawaban AI memerlukan internet. Jangan kirim kata sandi atau data rahasia.",
                        color = TextMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }

    private fun callAi(history: List<ChatMessage>): Pair<String, String?> {
        val client = OkHttpClient.Builder()
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
        return try {
            val msgs = JSONArray()
            msgs.put(JSONObject().put("role", "system").put(
                "content",
                "Kamu adalah AKSAA AI, asisten pribadi yang ramah, cerdas, jujur, dan membantu. " +
                    "Jawab dalam bahasa yang digunakan pengguna, utamakan bahasa Indonesia. " +
                    "Jangan mengaku memiliki kemampuan yang tidak tersedia."
            ))
            history.takeLast(20).forEach {
                msgs.put(JSONObject()
                    .put("role", if (it.fromUser) "user" else "assistant")
                    .put("content", it.text))
            }
            val body = JSONObject()
                .put("model", "auto")
                .put("messages", msgs)
                .put("temperature", 0.7)
                .put("max_tokens", 1200)
                .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.llmfaucet.dev/v1/chat/completions")
                .addHeader("Authorization", "Bearer free")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(raw).optString("error") }.getOrNull()
                    return Pair("Maaf, layanan AI sedang tidak tersedia. Coba lagi beberapa saat.", 
                        "HTTP ${response.code}${if (!detail.isNullOrBlank()) ": $detail" else ""}")
                }
                val answer = JSONObject(raw).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content")
                Pair(answer, null)
            }
        } catch (e: IOException) {
            Pair("Tidak bisa terhubung ke layanan AI. Periksa internet lalu coba lagi.", e.message)
        } catch (e: Exception) {
            Pair("Terjadi kesalahan saat membaca jawaban AI. Coba lagi.", e.message)
        }
    }
}
