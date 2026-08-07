package com.rofwin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// ===== Windows 11 design tokens (Rofwin 1.4.0) =====
val Win11Accent = Color(0xFF4CC2FF)
val Win11AccentSolid = Color(0xFF0078D4)
val Win11Taskbar = Color(0xE6202020)
val Win11Card = Color(0xF5282828)
val Win11Stroke = Color(0x33FFFFFF)

data class StartApp(val title: String, val icon: ImageVector, val window: DesktopWindow)

// Naik satu folder (generik — bekerja untuk folder buatan baru juga)
fun parentPath(p: String): String {
    val t = p.trimEnd('\\')
    if (t.length <= 3) return p
    val parent = t.substringBeforeLast('\\', t)
    return if (parent.endsWith(':')) "$parent\\" else parent
}

// Evaluator aritmetika sederhana untuk Python Shell ( + - * / ( ) dan desimal )
private class PyParser(private val expr: String) {
    private var idx = 0
    private fun ws() { while (idx < expr.length && expr[idx] == ' ') idx++ }
    private fun parseFactor(): Double? {
        ws()
        if (expr.getOrNull(idx) == '-') { idx++; return -(parseFactor() ?: return null) }
        if (expr.getOrNull(idx) == '(') {
            idx++
            val v = parseExpr() ?: return null
            ws()
            if (expr.getOrNull(idx) != ')') return null
            idx++
            return v
        }
        val start = idx
        while (idx < expr.length && (expr[idx].isDigit() || expr[idx] == '.')) idx++
        if (start == idx) return null
        return expr.substring(start, idx).toDoubleOrNull()
    }
    private fun parseTerm(): Double? {
        var v = parseFactor() ?: return null
        while (true) {
            ws()
            when (expr.getOrNull(idx)) {
                '*' -> { idx++; v *= parseFactor() ?: return null }
                '/' -> { idx++; v /= parseFactor() ?: return null }
                else -> return v
            }
        }
    }
    private fun parseExpr(): Double? {
        var v = parseTerm() ?: return null
        while (true) {
            ws()
            when (expr.getOrNull(idx)) {
                '+' -> { idx++; v += parseTerm() ?: return null }
                '-' -> { idx++; v -= parseTerm() ?: return null }
                else -> return v
            }
        }
    }
    fun parse(): Double? {
        if (expr.isBlank()) return null
        val v = parseExpr() ?: return null
        ws()
        return if (idx == expr.length) v else null
    }
}

fun evalPy(expr: String): Double? = PyParser(expr).parse()

// Window Type
enum class DesktopWindow {
    NONE, MY_COMPUTER, REGISTRY_EDITOR, TASK_MANAGER, COMMAND_PROMPT, DX_DIAG, BROWSER, GIT_BASH, AI_ROUTE, WINRAR, PYTHON_SHELL, SSH_MANAGER, MT5, MQL5_EDITOR, CODE_EDITOR, WINECFG, WINETRICKS, AI_CHAT, APK_STUDIO, VM_BUILDER, MT5_SETUP
}

// Posisi trading MT5 (live P/L dihitung dari harga sim; sl/tp opsional)
data class Mt5Pos(val ticket: Long, val buy: Boolean, val sym: String, val lots: Double, val open: Double, val sl: Double = 0.0, val tp: Double = 0.0)

// Candle untuk chart MT5
data class Mt5Candle(val o: Float, var h: Float, var l: Float, var c: Float)

// Struktur repo ivansslo/rocd (HEAD, ~217 entries) — container manager (multi-OS)
fun seedRocdFs(): Map<String, List<SimFile>> = mapOf(
    "D:\\Work\\rocd" to listOf(SimFile("CLAUDE.md", false, "3 KB"), SimFile("LICENSE", false, "2 KB"), SimFile("README.md", false, "3 KB"), SimFile("SECURITY.md", false, "3 KB"), SimFile("install.sh", false, "2 KB"), SimFile("pyproject.toml", false, "1 KB"), SimFile("rocd", false, "2 KB"), SimFile("rocd.py", false, "5 KB"), SimFile("rocd_mod", true), SimFile("screenshot.png", false, "2 KB"), SimFile("tests", true)),
    "D:\\Work\\rocd\\rocd_mod" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("arch.py", false, "5 KB"), SimFile("atomic.py", false, "5 KB"), SimFile("cli.py", false, "5 KB"), SimFile("commands", true), SimFile("completions", true), SimFile("constants.py", false, "5 KB"), SimFile("helpers", true), SimFile("l2s.py", false, "5 KB"), SimFile("locking.py", false, "5 KB"), SimFile("message.py", false, "5 KB"), SimFile("names.py", false, "5 KB"), SimFile("parser.py", false, "5 KB"), SimFile("paths.py", false, "5 KB"), SimFile("progress.py", false, "5 KB"), SimFile("session.py", false, "5 KB"), SimFile("sysdata.py", false, "5 KB")),
    "D:\\Work\\rocd\\rocd_mod\\commands" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("backup.py", false, "5 KB"), SimFile("build.py", false, "5 KB"), SimFile("clear_cache.py", false, "5 KB"), SimFile("copy.py", false, "5 KB"), SimFile("help", true), SimFile("install.py", false, "5 KB"), SimFile("install_local.py", false, "5 KB"), SimFile("kill.py", false, "5 KB"), SimFile("list.py", false, "5 KB"), SimFile("login", true), SimFile("ps.py", false, "5 KB"), SimFile("push.py", false, "5 KB"), SimFile("remove.py", false, "5 KB"), SimFile("rename.py", false, "5 KB"), SimFile("reset.py", false, "5 KB"), SimFile("restore.py", false, "5 KB"), SimFile("run.py", false, "5 KB"), SimFile("sync.py", false, "5 KB")),
    "D:\\Work\\rocd\\rocd_mod\\commands\\help" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("pages.py", false, "5 KB"), SimFile("render.py", false, "5 KB")),
    "D:\\Work\\rocd\\rocd_mod\\commands\\login" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("bindings.py", false, "5 KB"), SimFile("detach.py", false, "5 KB"), SimFile("env.py", false, "5 KB"), SimFile("migrate.py", false, "5 KB"), SimFile("passwd.py", false, "5 KB"), SimFile("proot_cmd.py", false, "5 KB"), SimFile("quoting.py", false, "5 KB")),
    "D:\\Work\\rocd\\rocd_mod\\completions" to listOf(SimFile("_proot-distro", false, "2 KB"), SimFile("proot-distro.bash", false, "2 KB"), SimFile("proot-distro.fish", false, "2 KB")),
    "D:\\Work\\rocd\\rocd_mod\\helpers" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("build_cache.py", false, "5 KB"), SimFile("build_engine", true), SimFile("docker", true), SimFile("dockerfile.py", false, "5 KB"), SimFile("download.py", false, "5 KB"), SimFile("layer_diff.py", false, "5 KB"), SimFile("oci_writer.py", false, "5 KB"), SimFile("rootfs.py", false, "5 KB"), SimFile("tar_extract.py", false, "5 KB")),
    "D:\\Work\\rocd\\rocd_mod\\helpers\\build_engine" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("constants.py", false, "5 KB"), SimFile("copy_step.py", false, "5 KB"), SimFile("dockerignore.py", false, "5 KB"), SimFile("engine.py", false, "5 KB"), SimFile("errors.py", false, "5 KB"), SimFile("handlers.py", false, "5 KB"), SimFile("parsing.py", false, "5 KB"), SimFile("run_step.py", false, "5 KB"), SimFile("stage.py", false, "5 KB"), SimFile("users.py", false, "5 KB")),
    "D:\\Work\\rocd\\rocd_mod\\helpers\\docker" to listOf(SimFile("__init__.py", false, "5 KB"), SimFile("cache.py", false, "5 KB"), SimFile("layers.py", false, "5 KB"), SimFile("media.py", false, "5 KB"), SimFile("pull.py", false, "5 KB"), SimFile("push.py", false, "5 KB"), SimFile("refs.py", false, "5 KB"), SimFile("transport.py", false, "5 KB")),
    "D:\\Work\\rocd\\tests" to listOf(SimFile("README.md", false, "3 KB"), SimFile("_builders.py", false, "5 KB"), SimFile("conftest.py", false, "5 KB"), SimFile("integration", true), SimFile("live", true), SimFile("security", true), SimFile("unit", true)),
    "D:\\Work\\rocd\\tests\\integration" to listOf(SimFile("test_backup_restore_roundtrip.py", false, "5 KB"), SimFile("test_build_end_to_end.py", false, "5 KB"), SimFile("test_clear_cache_and_list.py", false, "5 KB"), SimFile("test_cli_dispatch.py", false, "5 KB"), SimFile("test_copy_sync.py", false, "5 KB"), SimFile("test_install_local_oci.py", false, "5 KB"), SimFile("test_install_local_tar.py", false, "5 KB"), SimFile("test_login_get_proot_cmd.py", false, "5 KB"), SimFile("test_pull_offline.py", false, "5 KB"), SimFile("test_remove_rename_reset.py", false, "5 KB")),
    "D:\\Work\\rocd\\tests\\live" to listOf(SimFile("test_live_proot.py", false, "5 KB"), SimFile("test_live_pull.py", false, "5 KB")),
    "D:\\Work\\rocd\\tests\\security" to listOf(SimFile("test_bind_validation.py", false, "5 KB"), SimFile("test_copy_step_traversal.py", false, "5 KB"), SimFile("test_oci_blob_traversal.py", false, "5 KB"), SimFile("test_restore_dest_path.py", false, "5 KB"), SimFile("test_tar_extract_traversal.py", false, "5 KB")),
    "D:\\Work\\rocd\\tests\\unit" to listOf(SimFile("test_arch.py", false, "5 KB"), SimFile("test_atomic.py", false, "5 KB"), SimFile("test_dockerfile.py", false, "5 KB"), SimFile("test_download.py", false, "5 KB"), SimFile("test_handlers.py", false, "5 KB"), SimFile("test_layer_diff.py", false, "5 KB"), SimFile("test_locking.py", false, "5 KB"), SimFile("test_login_env.py", false, "5 KB"), SimFile("test_message.py", false, "5 KB"), SimFile("test_names.py", false, "5 KB"), SimFile("test_parser.py", false, "5 KB"), SimFile("test_pull_offline.py", false, "5 KB")),
)

// Baseline simbol MT5 (dipakai AI untuk mengukur perubahan harga)
val MT5_BASELINE = mapOf(
    "EURUSD" to 1.09250, "GBPUSD" to 1.27510, "USDJPY" to 145.305, "USDCHF" to 0.88120,
    "AUDUSD" to 0.66240, "NZDUSD" to 0.59980, "USDCAD" to 1.36150, "EURGBP" to 0.85680,
    "EURJPY" to 158.720, "GBPJPY" to 185.340, "AUDJPY" to 96.240, "EURAUD" to 1.64850,
    "GBPAUD" to 1.92430, "USDSGD" to 1.34260, "USDIDR" to 16305.0, "XAUUSD" to 2385.40,
    "XAGUSD" to 31.240, "BTCUSD" to 97450.0, "ETHUSD" to 3420.0, "US30" to 41250.0,
    "NAS100" to 18960.0, "SPX500" to 5620.0, "UKOIL" to 82.45, "WTI" to 78.30
)

fun mt5DigitsOf(sym: String) = when {
    sym.endsWith("JPY") -> 3
    sym == "USDIDR" -> 0
    sym == "XAUUSD" || sym == "XAGUSD" || sym == "UKOIL" || sym == "WTI" || sym == "ETHUSD" -> 2
    sym == "BTCUSD" || sym == "US30" || sym == "NAS100" || sym == "SPX500" -> 1
    else -> 5
}
fun mt5Fmt(sym: String, v: Double) = "%.${mt5DigitsOf(sym)}f".format(v)
fun mt5SpreadOf(sym: String) = when {
    sym.endsWith("JPY") -> 0.015; sym == "USDIDR" -> 8.0
    sym == "XAUUSD" -> 0.35; sym == "XAGUSD" -> 0.025
    sym == "BTCUSD" -> 25.0; sym == "ETHUSD" -> 2.0
    sym == "US30" || sym == "NAS100" -> 2.0; sym == "SPX500" -> 0.6
    sym == "UKOIL" || sym == "WTI" -> 0.04
    else -> 0.00015
}
fun mt5PnlFactor(sym: String) = when {
    sym.endsWith("JPY") -> 1000.0; sym == "USDIDR" -> 0.01
    sym == "XAUUSD" -> 100.0; sym == "XAGUSD" -> 5000.0
    sym == "BTCUSD" -> 1.0; sym == "ETHUSD" -> 10.0
    sym == "US30" || sym == "NAS100" || sym == "SPX500" -> 100.0
    sym == "UKOIL" || sym == "WTI" -> 10.0
    else -> 100000.0
}
fun mt5VolOf(sym: String) = when {
    sym == "BTCUSD" || sym == "ETHUSD" || sym == "US30" || sym == "NAS100" || sym == "SPX500" -> 0.0012f
    sym == "XAUUSD" -> 0.0007f
    else -> 0.0004f
}

// ===== HTTP + JSON helpers untuk ROC AI =====
fun httpGet(url: String, bearer: String = ""): Pair<Int, String> = try {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    c.connectTimeout = 9000; c.readTimeout = 9000
    c.setRequestProperty("User-Agent", "Rofwin/1.6")
    c.setRequestProperty("Accept", "application/vnd.github+json")
    if (bearer.isNotEmpty()) c.setRequestProperty("Authorization", "Bearer $bearer")
    val code = c.responseCode
    val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
    c.disconnect()
    code to txt
} catch (e: Exception) { -1 to (e.message ?: "io error") }

fun httpPostJson(url: String, bearer: String, json: String): Pair<Int, String> = try {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    c.requestMethod = "POST"
    c.connectTimeout = 12000; c.readTimeout = 45000
    c.setRequestProperty("Content-Type", "application/json")
    c.setRequestProperty("User-Agent", "Rofwin/1.6")
    if (bearer.isNotEmpty()) c.setRequestProperty("Authorization", "Bearer $bearer")
    c.doOutput = true
    c.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    val code = c.responseCode
    val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
    c.disconnect()
    code to txt
} catch (e: Exception) { -1 to (e.message ?: "io error") }

fun jsonStr(s: String): String = buildString {
    append('"')
    s.forEach { c -> when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\")
        '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
        else -> append(c)
    } }
    append('"')
}

// ===== v1.7.0 — Deteksi Critical text (journal, AI chat, notif, log build & SSH) =====
val CRITICAL_PATTERN = Regex(
    "(critical|fatal|error|exception|margin call|margincall|stopped out|stop out|gagal|disconnect|unauthor|denied|401|403|crash|sl hit|failed)",
    RegexOption.IGNORE_CASE
)
fun detectCritical(text: String): Boolean = CRITICAL_PATTERN.containsMatchIn(text)

// ===== v1.7.0 — Plugin AI yang bisa di-toggle di ROC AI =====
data class AiPlugin(val id: String, val name: String, val desc: String)
val AI_PLUGINS = listOf(
    AiPlugin("eagen", "EA Generator", "AI menulis file .mq5 utuh dari deskripsi"),
    AiPlugin("rocsync", "ROC Sync", "Tarik struktur terbaru repo ivansslo/rocagents"),
    AiPlugin("digest", "Market Digest", "Sisipkan konteks pasar live ke jawaban AI"),
    AiPlugin("critical", "Critical Alert", "Sorot teks kritis (error, margin call, dsb)")
)

// ===== v1.7.0 — SSH NYATA (JSch) — jembatan lokal -> OCI =====
// ===== v1.8.2 — HOST-KEY PINNING (TOFU): fingerprint SHA-256 server disimpan saat koneksi
// pertama (trust-on-first-use), diverifikasi pada setiap koneksi berikutnya. Bila fingerprint
// berubah, koneksi DITOLAK — melindungi dari serangan MITM / server palsu. =====
fun sshExec(host: String, port: Int, user: String, pass: String, cmd: String, timeoutMs: Int = 9000, kh: android.content.SharedPreferences? = null): Pair<Boolean, String> {
    return try {
    val jsch = com.jcraft.jsch.JSch()
    val session = jsch.getSession(user, host, port)
    session.setPassword(pass)
    val cfg = java.util.Properties()
    // Catatan: tetap "no" agar handshake tidak gagal — tapi fingerprint diverifikasi MANUAL
    // di bawah (TOFU), jadi MITM setelah pin tersimpan tetap ketolak.
    cfg["StrictHostKeyChecking"] = "no"
    session.setConfig(cfg)
    session.connect(timeoutMs)
    val hk = session.hostKey
    val fp = hk?.let {
        try {
            val blob = android.util.Base64.decode(it.key, android.util.Base64.NO_WRAP)
            val md = java.security.MessageDigest.getInstance("SHA-256")
            "SHA256:" + android.util.Base64.encodeToString(md.digest(blob), android.util.Base64.NO_WRAP).trimEnd('=')
        } catch (_: Exception) { "?" }
    } ?: "?"
    if (kh != null && hk != null) {
        val k = "ssh_kh_${host}_${port}"
        when (val saved = kh.getString(k, null)) {
            null -> kh.edit().putString(k, fp).apply() // first use -> pin
            fp -> Unit // cocok -> lanjut
            else -> {
                session.disconnect()
                return false to "🔴 HOST KEY BERUBAH — koneksi DITOLAK (kemungkinan MITM/server diganti)\ntersimpan: $saved\nsekarang : $fp\nHapus pin di prefs ${k} untuk mempercayai ulang."
            }
        }
    }
    val ch = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
    ch.setCommand(cmd)
    ch.connect(timeoutMs)
    val raw = ch.inputStream.readBytes().toString(Charsets.UTF_8)
    ch.disconnect()
    session.disconnect()
    true to (raw.trim().ifBlank { "(tanpa output)" } + "\n🔑 host-key $fp ✔")
    } catch (e: Exception) {
        false to "ERROR: ${e.message}"
    }
}

// ===== v1.7.0 — Downloader NYATA (HTTP -> file lokal, dengan progress) =====
fun downloadToFile(url: String, dest: java.io.File, onProgress: (Long) -> Unit): Pair<Boolean, String> = try {
    dest.parentFile?.mkdirs()
    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    conn.connectTimeout = 10000
    conn.readTimeout = 20000
    conn.instanceFollowRedirects = true
    conn.setRequestProperty("User-Agent", "Rofwin/1.7")
    conn.connect()
    if (conn.responseCode !in 200..299) {
        false to "HTTP ${conn.responseCode}"
    } else {
        var total = 0L
        conn.inputStream.use { inp ->
            dest.outputStream().use { out ->
                val buf = ByteArray(16384)
                var r: Int
                while (inp.read(buf).also { r = it } != -1) {
                    out.write(buf, 0, r)
                    total += r
                    onProgress(total)
                }
            }
        }
        conn.disconnect()
        true to total.toString()
    }
} catch (e: Exception) {
    false to "ERROR: ${e.message}"
}

// ===== v1.8.2 — snapshot sesi: memungkinkan parse & rakit JSON dilakukan DI LUAR main thread
// (v1.8.1 ke bawah melakukan parse/serialize di UI thread -> ANR "aplikasi tidak merespons"
// di device low-end seperti CPH1823 saat sesi membesar — penyebab utama 'stuck/FC') =====
class SessionSnap(
    val fs: Map<String, List<SimFile>>, val files: Map<String, String>, val eas: List<String>,
    val balance: Float?, val ticket: Long?, val positions: List<Mt5Pos>,
    val history: List<String>, val journal: List<String>, val activeEAs: Map<String, String>,
    val pinned: List<String>?, val tblock: Boolean?, val plugins: Map<String, Boolean>,
    val bubble: Boolean?, val mt5account: String?, val mt5installed: Boolean?
)

fun parseSessionBlob(blob: String): SessionSnap {
    val root = Json.parseToJsonElement(blob).jsonObject
    val fs = LinkedHashMap<String, List<SimFile>>()
    root["fs"]?.jsonObject?.forEach { (path, arr) ->
        fs[path] = arr.jsonArray.map { el ->
            val o = el.jsonObject
            SimFile(o["n"]!!.jsonPrimitive.content, o["d"]!!.jsonPrimitive.content == "1", o["s"]!!.jsonPrimitive.content)
        }
    }
    val files = LinkedHashMap<String, String>()
    root["files"]?.jsonObject?.forEach { (p, v) -> files[p] = v.jsonPrimitive.content }
    val eas = mutableListOf<String>()
    root["eas"]?.jsonArray?.forEach { e -> eas.add(e.jsonPrimitive.content) }
    val positions = mutableListOf<Mt5Pos>()
    root["positions"]?.jsonArray?.forEach { el ->
        val o = el.jsonObject
        positions.add(Mt5Pos(o["t"]!!.jsonPrimitive.content.toLong(), o["b"]!!.jsonPrimitive.content == "1", o["s"]!!.jsonPrimitive.content, o["l"]!!.jsonPrimitive.content.toDouble(), o["p"]!!.jsonPrimitive.content.toDouble()))
    }
    val history = mutableListOf<String>(); root["history"]?.jsonArray?.forEach { history.add(it.jsonPrimitive.content) }
    val journal = mutableListOf<String>(); root["journal"]?.jsonArray?.forEach { journal.add(it.jsonPrimitive.content) }
    val act = LinkedHashMap<String, String>(); root["activeEAs"]?.jsonObject?.forEach { (ea, sym) -> act[ea] = sym.jsonPrimitive.content }
    val pinned = root["pinned"]?.jsonArray?.map { it.jsonPrimitive.content }
    val plug = LinkedHashMap<String, Boolean>(); root["plugins"]?.jsonObject?.forEach { (k, v) -> plug[k] = v.jsonPrimitive.content == "1" }
    return SessionSnap(
        fs, files, eas,
        root["balance"]?.jsonPrimitive?.content?.toFloatOrNull(),
        root["ticket"]?.jsonPrimitive?.content?.toLongOrNull(),
        positions, history, journal, act, pinned,
        root["tblock"]?.jsonPrimitive?.content?.let { it == "1" }, plug,
        root["bubble"]?.jsonPrimitive?.content?.let { it == "1" },
        root["mt5account"]?.jsonPrimitive?.content,
        root["mt5installed"]?.jsonPrimitive?.content?.let { it == "1" }
    )
}

fun buildSessionJson(
    fs: Map<String, List<SimFile>>, files: Map<String, String>, eas: List<String>,
    balance: Float, ticket: Long, positions: List<Mt5Pos>,
    history: List<String>, journal: List<String>, activeEAs: Map<String, String>,
    pinned: List<String>, locked: Boolean, plugins: Map<String, Boolean>,
    bubble: Boolean, account: String, installed: Boolean
): String {
    val sb = StringBuilder()
    sb.append("{\"fs\":{")
    var first = true
    fs.forEach { (path, list) ->
        if (!first) sb.append(",")
        first = false
        sb.append(jsonStr(path)).append(":[")
        list.forEachIndexed { i, f ->
            if (i > 0) sb.append(",")
            sb.append("{\"n\":").append(jsonStr(f.name)).append(",\"d\":\"").append(if (f.isDirectory) "1" else "0").append("\",\"s\":").append(jsonStr(f.size)).append("}")
        }
        sb.append("]")
    }
    sb.append("},\"files\":{")
    first = true
    files.forEach { (p, c) -> if (!first) sb.append(","); first = false; sb.append(jsonStr(p)).append(":").append(jsonStr(c)) }
    sb.append("},\"eas\":[")
    eas.forEachIndexed { i, ea -> if (i > 0) sb.append(","); sb.append(jsonStr(ea)) }
    sb.append("],\"balance\":\"").append(balance).append("\",\"ticket\":\"").append(ticket).append("\",\"positions\":[")
    positions.forEachIndexed { i, p ->
        if (i > 0) sb.append(",")
        sb.append("{\"t\":\"").append(p.ticket).append("\",\"b\":\"").append(if (p.buy) "1" else "0").append("\",\"s\":\"").append(p.sym).append("\",\"l\":\"").append(p.lots).append("\",\"p\":\"").append(p.open).append("\"}")
    }
    sb.append("],\"history\":[")
    history.forEachIndexed { i, h -> if (i > 0) sb.append(","); sb.append(jsonStr(h)) }
    sb.append("],\"journal\":[")
    journal.forEachIndexed { i, j -> if (i > 0) sb.append(","); sb.append(jsonStr(j)) }
    sb.append("],\"activeEAs\":{")
    first = true
    activeEAs.forEach { (ea, sym) -> if (!first) sb.append(","); first = false; sb.append(jsonStr(ea)).append(":").append(jsonStr(sym)) }
    sb.append("}")
    sb.append(",\"pinned\":[")
    pinned.forEachIndexed { i, w -> if (i > 0) sb.append(","); sb.append(jsonStr(w)) }
    sb.append("],\"tblock\":\"").append(if (locked) "1" else "0").append("\",\"plugins\":{")
    var fpnd = true
    plugins.forEach { (k, v) -> if (!fpnd) sb.append(","); fpnd = false; sb.append(jsonStr(k)).append(":\"").append(if (v) "1" else "0").append("\"") }
    sb.append("},\"bubble\":\"").append(if (bubble) "1" else "0")
    sb.append("\",\"mt5account\":").append(jsonStr(account))
    sb.append(",\"mt5installed\":\"").append(if (installed) "1" else "0").append("\"")
    sb.append("}")
    return sb.toString()
}

// ===== v1.7.0 — helper FS simulasi (folder rekursif + file dengan ukuran) =====
fun ensureSimFolder(fs: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>, folder: String) {
    if (fs.containsKey(folder)) return
    val parent = folder.substringBeforeLast('\\', "")
    val name = folder.substringAfterLast('\\')
    if (parent.isNotEmpty()) ensureSimFolder(fs, parent)
    fs[folder] = fs[folder] ?: emptyList()
    if (parent.isNotEmpty()) {
        val pl = (fs[parent] ?: emptyList()).toMutableList()
        if (pl.none { it.name == name }) pl.add(SimFile(name, true, "Folder"))
        fs[parent] = pl
    }
}

fun putSimFile(fs: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>, folder: String, name: String, size: String) {
    ensureSimFolder(fs, folder)
    val l = (fs[folder] ?: emptyList()).toMutableList()
    val i = l.indexOfFirst { it.name == name }
    val f = SimFile(name, false, size)
    if (i >= 0) l[i] = f else l.add(f)
    fs[folder] = l
}

// Template EA lokal (dipakai AI saat offline — tanpa API key)
const val AI_EA_TEMPLATE = """//+------------------------------------------------------------------+
//| AI-Generated: MA Cross Scalper (template lokal Rofwin)          |
//+------------------------------------------------------------------+
#property copyright "ROC-AI"
#property version   "1.00"
#property strict

input int    FastMA = 7;
input int    SlowMA = 21;
input double Lots   = 0.01;
input int    StopLossPips = 25;
input int    TakeProfitPips = 50;

int OnInit()
  {
   Print("EA AI MA-Cross aktif");
   return(INIT_SUCCEEDED);
  }

void OnTick()
  {
   double fast = iMA(NULL, 0, FastMA, 0, MODE_SMA, PRICE_CLOSE, 0);
   double slow = iMA(NULL, 0, SlowMA, 0, MODE_SMA, PRICE_CLOSE, 0);
   double prevFast = iMA(NULL, 0, FastMA, 0, MODE_SMA, PRICE_CLOSE, 1);
   double prevSlow = iMA(NULL, 0, SlowMA, 0, MODE_SMA, PRICE_CLOSE, 1);

   if(fast > slow && prevFast <= prevSlow)
     {
      // sinyal BUY — golden cross
      Print("Signal BUY @ ", Bid);
     }
   if(fast < slow && prevFast >= prevSlow)
     {
      // sinyal SELL — death cross
      Print("Signal SELL @ ", Ask);
     }
  }
"""
fun seedRocAgentsFs(): Map<String, List<SimFile>> = mapOf(
    "D:\\Work\\rocagents" to listOf(
        SimFile("bin", true), SimFile("dashboard", true), SimFile("oci", true),
        SimFile("server", true), SimFile("sessions", true), SimFile("src", true),
        SimFile(".env.example", false, "1 KB"), SimFile(".gitignore", false, "1 KB"),
        SimFile("README.md", false, "6 KB"), SimFile("agent_install.sh", false, "2 KB"),
        SimFile("bun.lock", false, "210 KB"), SimFile("db.json", false, "3 KB"),
        SimFile("hermes", false, "96 KB"), SimFile("hermes_install.sh", false, "4 KB"),
        SimFile("index.html", false, "1 KB"), SimFile("install.sh", false, "3 KB"),
        SimFile("metadata.json", false, "1 KB"), SimFile("nous_agent.sh", false, "2 KB"),
        SimFile("nous_hermes_agent_install.sh", false, "3 KB"),
        SimFile("package-lock.json", false, "450 KB"), SimFile("package.json", false, "2 KB"),
        SimFile("patch-roc-agentsroute.sh", false, "2 KB"), SimFile("proot_install.sh", false, "3 KB"),
        SimFile("server.ts", false, "8 KB"), SimFile("termux-create-new-codespace.sh", false, "2 KB"),
        SimFile("termux-login-codespace.sh", false, "1 KB"), SimFile("termux-open-codespace-localhost.sh", false, "1 KB"),
        SimFile("tsconfig.json", false, "1 KB"), SimFile("vite.config.ts", false, "1 KB")
    ),
    "D:\\Work\\rocagents\\bin" to listOf(SimFile("codex", false, "44 KB")),
    "D:\\Work\\rocagents\\dashboard" to listOf(SimFile("firebase.json", false, "1 KB"), SimFile("index.html", false, "2 KB")),
    "D:\\Work\\rocagents\\oci" to listOf(
        SimFile("install_oci_cli.sh", false, "3 KB"), SimFile("private-model-install.sh", false, "2 KB"),
        SimFile("setup-cf-tunnel.sh", false, "3 KB"), SimFile("setup-oci-rdp.sh", false, "4 KB")
    ),
    "D:\\Work\\rocagents\\server" to listOf(
        SimFile("db.ts", false, "5 KB"), SimFile("orchestrator.ts", false, "11 KB"),
        SimFile("scheduler.ts", false, "6 KB"), SimFile("tools.ts", false, "9 KB")
    ),
    "D:\\Work\\rocagents\\sessions" to emptyList(),
    "D:\\Work\\rocagents\\src" to listOf(
        SimFile("components", true), SimFile("App.tsx", false, "7 KB"), SimFile("index.css", false, "2 KB"),
        SimFile("main.tsx", false, "1 KB"), SimFile("types.ts", false, "2 KB")
    ),
    "D:\\Work\\rocagents\\src\\components" to listOf(
        SimFile("AppsSyncedManager.tsx", false, "4 KB"), SimFile("ChatInput.tsx", false, "3 KB"),
        SimFile("ChatMessage.tsx", false, "4 KB"), SimFile("ExecutionHistoryModal.tsx", false, "3 KB"),
        SimFile("FileArchive.tsx", false, "5 KB"), SimFile("LiveTerminal.tsx", false, "6 KB"),
        SimFile("SyncDashboard.tsx", false, "5 KB"), SimFile("UpgradePanel.tsx", false, "3 KB")
    )
)

const val ROCAGENTS_README = """# ROCAgents — Unified Hermes AI Agent CLI & Web UI
Version 5.14.0 (Oracle)

Hermes Agent CLI + Local DevAgent Orchestrator Web UI
stack: Express + Vite + React 19 + Gemini API.

Quick start:
  git clone https://github.com/ivansslo/rocagents.git
  npm install
  npm run webui

Folders:
  server/   orchestrator, scheduler, tools, db (Express)
  src/      React 19 front-end: LiveTerminal, SyncDashboard,
            ChatInput/ChatMessage, FileArchive, UpgradePanel
  oci/      Oracle Cloud installer scripts
  bin/      codex launcher
  dashboard firebase host config
"""

const val DEFAULT_EXPERT_MQ5 = """//+------------------------------------------------------------------+
//|                                                      Expert.mq5 |
//|                                      Copyright 2026, MetaQuotes |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2026"
#property link      "https://www.mql5.com"
#property version   "1.00"

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
  {
   Print("Rofwin EA activated!");
   return(INIT_SUCCEEDED);
  }

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
  {
   // strategi di sini
  }
"""

data class SimFile(val name: String, val isDirectory: Boolean = false, val size: String = "1 KB")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
// ===== v1.8.1 — breadcrumb masuk desktop (exception ditangkap Crash Shield JVM -> Panel Crash Dashboard) =====
@Composable
fun WineDesktopSim(
    container: WineContainer,
    profile: InputControlsProfile,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { crumb(context, "D1:composed") }
    WineDesktopSimInner(container, profile, onClose)
}

@Composable
private fun WineDesktopSimInner(
    container: WineContainer,
    profile: InputControlsProfile,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("RofwinDrives", android.content.Context.MODE_PRIVATE) }
    // ===== v1.8.1 — Safe Mode state =====
    val crashPrefs = remember { context.getSharedPreferences("RofwinCrash", android.content.Context.MODE_PRIVATE) }
    val safeMode = remember { crashPrefs.getBoolean("safe_next_mode", false) }
    val dDriveEnabled = prefs.getBoolean("drive_d", true)
    val eDriveEnabled = prefs.getBoolean("drive_e", false)
    val zDriveEnabled = prefs.getBoolean("drive_z", false)

    var isBooting by remember { mutableStateOf(true) }
    var bootProgress by remember { mutableFloatStateOf(0f) }

    // Desktop Window States
    var openWindow by remember { mutableStateOf(DesktopWindow.NONE) }
    var startMenuOpen by remember { mutableStateOf(false) }

    // Window offsets (simple dragging support)
    var windowOffset by remember { mutableStateOf(Offset(50f, 100f)) }
    var minimizedWindows = remember { mutableStateListOf<DesktopWindow>() }

    // Windows-style window state (Rofwin 1.3.0): maximize / restore / true-fullscreen / low-RAM
    var isMaximized by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var lowRamMode by remember { mutableStateOf(prefs.getBoolean("low_ram", false)) }
    val openWin: (DesktopWindow) -> Unit = { w ->
        openWindow = w
        isMaximized = false
        isFullscreen = false
    }

    // Windows 11 panels (widgets / quick settings / notifikasi / power) + jam hidup
    var widgetsOpen by remember { mutableStateOf(false) }
    var qsOpen by remember { mutableStateOf(false) }
    var notifOpen by remember { mutableStateOf(false) }
    var powerOpen by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    // File Explorer State
    var currentPath by remember { mutableStateOf("C:\\") }
    
    val simulatedFiles = remember {
        mutableStateMapOf<String, List<SimFile>>().apply {
            putAll(
                mapOf(
                    "C:\\" to listOf(
                        SimFile("Windows", true),
                        SimFile("Program Files", true),
                        SimFile("MQL5", true),
                        SimFile("users", true),
                        SimFile("boot.ini", false, "256 B")
                    ),
                    "C:\\Windows" to listOf(
                        SimFile("System32", true),
                        SimFile("wine.inf", false, "12 KB"),
                        SimFile("regedit.exe", false, "45 KB")
                    ),
                    "C:\\Windows\\System32" to listOf(
                        SimFile("kernel32.dll", false, "820 KB"),
                        SimFile("user32.dll", false, "640 KB"),
                        SimFile("gdi32.dll", false, "310 KB")
                    ),
                    "C:\\Program Files" to listOf(
                        SimFile("DirectX", true),
                        SimFile("WineD3D", true)
                    ),
                    "C:\\Program Files\\DirectX" to listOf(
                        SimFile("dxgi.dll", false, "120 KB"),
                        SimFile("d3d11.dll", false, "450 KB")
                    ),
                    "C:\\Program Files\\WineD3D" to listOf(
                        SimFile("wined3d.dll", false, "980 KB")
                    ),
                    "C:\\users" to listOf(
                        SimFile("Administrator", true)
                    ),
                    "C:\\users\\Administrator" to listOf(
                        SimFile("My Documents", true),
                        SimFile("Desktop", true)
                    ),
                    "D:\\" to listOf(
                        SimFile("Work", true),
                        SimFile("Downloads", true)
                    ),
                    "D:\\Work" to listOf(
                        SimFile("rocagents", true),
                        SimFile("script.py", false, "2 KB"),
                        SimFile("backup.rar", false, "15 MB"),
                        SimFile("notes.txt", false, "1 KB")
                    ),
                    // Struktur MQL5 (EA hasil Compile mendarat di sini)
                    "C:\\MQL5" to listOf(
                        SimFile("Experts", true),
                        SimFile("Include", true),
                        SimFile("Scripts", true)
                    ),
                    "C:\\MQL5\\Experts" to listOf(
                        SimFile("Expert.mq5", false, "2 KB")
                    ),
                    "C:\\MQL5\\Include" to listOf(
                        SimFile("Trade.mqh", false, "18 KB")
                    ),
                    "C:\\MQL5\\Scripts" to emptyList(),
                    "D:\\Downloads" to listOf(
                        SimFile("winetricks.exe", false, "3 MB"),
                        SimFile("python-3.12.exe", false, "25 MB"),
                        SimFile("lasokamodule.exe", false, "1.2 MB"),
                        SimFile("winrar_full.exe", false, "4.5 MB")
                    ),
                    "E:\\" to listOf(
                        SimFile("Media", true),
                        SimFile("chrome_installer.exe", false, "1.2 MB")
                    ),
                    "Z:\\" to listOf(
                        SimFile("system", true),
                        SimFile("data", true),
                        SimFile("vendor", true),
                        SimFile("build.prop", false, "4 KB")
                    )
                )
            )
        }
    }
    // Tanam repo ivansslo/rocagents + ivansslo/rocd (struktur asli GitHub HEAD)
    LaunchedEffect(Unit) {
        if (!simulatedFiles.containsKey("D:\\Work\\rocagents")) {
            seedRocAgentsFs().forEach { (k, v) -> simulatedFiles[k] = v }
        }
        if (!simulatedFiles.containsKey("D:\\Work\\rocd")) {
            seedRocdFs().forEach { (k, v) -> simulatedFiles[k] = v }
            val list = simulatedFiles["D:\\Work"]?.toMutableList() ?: mutableListOf()
            if (list.none { it.name == "rocd" }) {
                list.add(SimFile("rocd", true))
                simulatedFiles["D:\\Work"] = list
            }
        }
    }

    // Isi file untuk Rofwin Code (editor) — Save menulis ke sini + FS
    val fileContents = remember {
        mutableStateMapOf(
            "C:\\MQL5\\Experts\\Expert.mq5" to DEFAULT_EXPERT_MQ5,
            "D:\\Work\\rocagents\\README.md" to ROCAGENTS_README,
            "D:\\Work\\script.py" to "# Rofwin script\nprint('halo dari python')\nprint(72/5+3*(9-4))\nprint(2**10)\n"
        )
    }
    // EA yang terkompilasi (MQL5) — terbaca MT5 Navigator, bisa di-attach
    val expertAdvisors = remember { mutableStateListOf<String>() }

    // ===== State trading BERSAMA (bot jalan terus walau MT5 di-minimize) =====
    val mt5Mids = remember { mutableStateMapOf<String, Double>() }
    val mt5PrevMids = remember { mutableStateMapOf<String, Double>() }
    val mt5Candles = remember { mutableStateListOf<Mt5Candle>() }
    val mt5Positions = remember { mutableStateListOf<Mt5Pos>() }
    val mt5History = remember { mutableStateListOf<String>() }
    val mt5Journal = remember { mutableStateListOf("Rofwin MT5 sim terminal ready — feed lokal + ROC-AI") }
    val mt5ActiveEAs = remember { mutableStateMapOf<String, String>() }
    var mt5Balance by remember { mutableFloatStateOf(10000f) }
    var mt5Ticket by remember { mutableLongStateOf(53000100L) }

    // ===== v1.7.0 — Taskbar pinned interaktif (move/unpin) + lock, tersimpan di sesi =====
    val taskbarPinned = remember { mutableStateListOf(DesktopWindow.MY_COMPUTER, DesktopWindow.BROWSER, DesktopWindow.CODE_EDITOR, DesktopWindow.AI_CHAT, DesktopWindow.COMMAND_PROMPT, DesktopWindow.MT5, DesktopWindow.TASK_MANAGER) }
    var taskbarLocked by remember { mutableStateOf(false) }
    var pinnedMenuFor by remember { mutableStateOf<DesktopWindow?>(null) }
    // ===== v1.7.0 — AI bubble mengambang + plugins + critical alerts =====
    var aiBubbleOn by remember { mutableStateOf(!safeMode) }
    var bubbleOffset by remember { mutableStateOf(Offset(36f, 980f)) }
    val aiPluginsOn = remember { mutableStateMapOf<String, Boolean>().apply { AI_PLUGINS.forEach { put(it.id, true) } } }
    val criticalAlerts = remember { mutableStateListOf<String>() }
    // ===== v1.7.0 — akun MT5 (login sim + WebTerminal nyata) & status install =====
    var mt5Account by remember { mutableStateOf("") }
    var mt5Installed by remember { mutableStateOf(false) }
    // ===== v1.7.0 — orientasi (tile Landscape di Quick Settings) =====
    var landscapeLocked by remember { mutableStateOf(false) }
    val mt5Rnd = remember { java.util.Random() }

    // ===== Live tick engine GLOBAL (harga + candle + EA auto-trade, 24/7 di desktop) =====
    LaunchedEffect(lowRamMode) {
        if (mt5Mids.isEmpty()) MT5_BASELINE.forEach { (k, v) -> mt5Mids[k] = v; mt5PrevMids[k] = v }
        if (mt5Candles.isEmpty()) {
            var v = 50f
            repeat(60) {
                val o = v
                v = (v + (mt5Rnd.nextFloat() - 0.48f) * 4f).coerceIn(5f, 95f)
                val c = v
                mt5Candles.add(Mt5Candle(o, maxOf(o, c) + mt5Rnd.nextFloat() * 1.5f, minOf(o, c) - mt5Rnd.nextFloat() * 1.5f, c))
            }
        }
        var tick = 0
        while (true) {
            delay(if (lowRamMode) 2200L else 800L)
            tick++
            mt5Mids.keys.toList().forEach { s ->
                val old = mt5Mids[s] ?: 1.0
                mt5PrevMids[s] = old
                mt5Mids[s] = old * (1.0 + (mt5Rnd.nextFloat() - 0.5f) * 2f * mt5VolOf(s))
            }
            if (mt5Candles.isNotEmpty()) {
                val last = mt5Candles.last()
                last.c = (last.c + (mt5Rnd.nextFloat() - 0.48f) * 3f).coerceIn(5f, 95f)
                if (last.c > last.h) last.h = last.c
                if (last.c < last.l) last.l = last.c
                if (tick % 4 == 0) {
                    val o = last.c
                    mt5Candles.removeAt(0)
                    mt5Candles.add(Mt5Candle(o, o + 0.5f, o - 0.5f, o))
                }
            }
            // Auto-close SL/TP (dicek tiap tick — seperti server MT5 asli)
            mt5Positions.toList().forEach { p ->
                if (p.sl > 0.0 || p.tp > 0.0) {
                    val mid = mt5Mids[p.sym] ?: return@forEach
                    val hitSl = p.sl > 0.0 && (if (p.buy) mid <= p.sl else mid >= p.sl)
                    val hitTp = p.tp > 0.0 && (if (p.buy) mid >= p.tp else mid <= p.tp)
                    if (hitSl || hitTp) {
                        val pnl = (if (p.buy) (mid - p.open) else (p.open - mid)) * p.lots * mt5PnlFactor(p.sym)
                        mt5Positions.remove(p)
                        mt5Balance += pnl.toFloat()
                        val tag = if (hitTp) "TP hit" else "SL hit"
                        mt5History.add(0, "#${p.ticket} ${if (p.buy) "buy" else "sell"} ${p.lots} ${p.sym} @ ${mt5Fmt(p.sym, p.open)} → P/L ${"%.2f".format(pnl)} USD [$tag]")
                        mt5Journal.add(0, "close #${p.ticket} ${p.sym} @ ${mt5Fmt(p.sym, mid)} — $tag, profit ${"%.2f".format(pnl)}")
                    }
                }
            }
            // EA auto-trade global — jalan walau jendela MT5 ditutup
            mt5ActiveEAs.forEach { (ea, sym) ->
                val r = mt5Rnd.nextFloat()
                if (r < 0.05f) {
                    val mid = mt5Mids[sym] ?: 0.0
                    if (mid > 0.0) {
                        val buy = mt5Rnd.nextBoolean()
                        val px = if (buy) mid + mt5SpreadOf(sym) else mid
                        mt5Positions.add(Mt5Pos(mt5Ticket++, buy, sym, 0.01, px))
                        mt5Journal.add(0, "open #${mt5Ticket - 1} ${if (buy) "buy" else "sell"} 0.01 $sym @ ${mt5Fmt(sym, px)} [EA $ea]")
                    }
                } else if (r < 0.085f) {
                    mt5Positions.firstOrNull { it.sym == sym }?.let { p ->
                        val mid = mt5Mids[p.sym] ?: p.open
                        val pnl = (if (p.buy) (mid - p.open) else (p.open - mid)) * p.lots * mt5PnlFactor(p.sym)
                        mt5Positions.remove(p)
                        mt5Balance += pnl.toFloat()
                        mt5History.add(0, "#${p.ticket} ${if (p.buy) "buy" else "sell"} ${p.lots} ${p.sym} @ ${mt5Fmt(p.sym, p.open)} → P/L ${"%.2f".format(pnl)} USD [EA $ea]")
                        mt5Journal.add(0, "close #${p.ticket} ${p.sym} profit ${"%.2f".format(pnl)} [EA $ea]")
                    }
                }
            }
            if (mt5History.size > 60) mt5History.removeLast()
            if (mt5Journal.size > 120) mt5Journal.removeLast()
        }
    }

    // ===== Sesi tersave di storage (autosave tiap 5 dtk — unlimited, tahan tutup app) =====
    LaunchedEffect(Unit) {
        crumb(context, "R:start")
        // ---- restore (v1.8.2: baca + parse DI LUAR main thread -> anti-ANR/stuck) ----
        val blob = if (!safeMode) withContext(Dispatchers.IO) {
            try { prefs.getString("rofwin_session_v2", null) } catch (_: Exception) { null }
        } else null
        val snap = blob?.let { b -> withContext(Dispatchers.Default) { try { parseSessionBlob(b) } catch (_: Exception) { null } } }
        snap?.let { s ->
            s.fs.forEach { (path, list) -> simulatedFiles[path] = list }
            fileContents.putAll(s.files)
            s.eas.forEach { n -> if (!expertAdvisors.contains(n)) expertAdvisors.add(n) }
            s.balance?.let { mt5Balance = it }
            s.ticket?.let { mt5Ticket = it }
            mt5Positions.addAll(s.positions)
            mt5History.addAll(s.history)
            mt5Journal.addAll(s.journal)
            mt5ActiveEAs.putAll(s.activeEAs)
            s.pinned?.let { arr ->
                taskbarPinned.clear()
                arr.forEach { el -> try { taskbarPinned.add(DesktopWindow.valueOf(el)) } catch (_: Exception) {} }
                if (taskbarPinned.isEmpty()) taskbarPinned.add(DesktopWindow.MY_COMPUTER)
            }
            s.tblock?.let { taskbarLocked = it }
            aiPluginsOn.putAll(s.plugins)
            s.bubble?.let { aiBubbleOn = it }
            s.mt5account?.let { mt5Account = it }
            s.mt5installed?.let { mt5Installed = it }
            mt5Journal.add(0, "✔ sesi dipulihkan dari storage (file, FS, positions, EA) [io-thread]")
        }
        crumb(context, "R:ok")
        // ---- autosave loop (v1.8.2 anti-ANR: snapshot state di Main, rakit JSON di Default, tulis di IO) ----
        while (true) {
            delay(5000)
            try {
                // 1) snapshot state (cepat, di main thread)
                val fsCopy = simulatedFiles.toMap()
                val filesCopy = fileContents.toMap()
                val easCopy = expertAdvisors.toList()
                val bal = mt5Balance
                val tic = mt5Ticket
                val posCopy = mt5Positions.toList()
                val histCopy = mt5History.take(40)
                val jourCopy = mt5Journal.take(40)
                val actCopy = mt5ActiveEAs.toMap()
                val pinCopy = taskbarPinned.map { it.name }
                val lock = taskbarLocked
                val plugCopy = aiPluginsOn.toMap()
                val bub = aiBubbleOn
                val acc = mt5Account
                val inst = mt5Installed
                // 2) rakit JSON di luar UI thread
                val out = withContext(Dispatchers.Default) {
                    buildSessionJson(fsCopy, filesCopy, easCopy, bal, tic, posCopy, histCopy, jourCopy, actCopy, pinCopy, lock, plugCopy, bub, acc, inst)
                }
                // 3) tulis ke disk di luar UI thread
                withContext(Dispatchers.IO) {
                    prefs.edit().putString("rofwin_session_v2", out).apply()
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); crumb(context, "D2:live") }

    var newFileName by remember { mutableStateOf("") }
    var fileTypeFolder by remember { mutableStateOf(false) }

    // Registry State
    val registryKeys = remember {
        mutableStateListOf(
            Pair("HKCU\\Software\\Wine\\Direct3D\\csmt", "0x00000001 (1)"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\MaxShaderModel", "3"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\OffscreenRenderingMode", "fbo"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\StrictDrawOrdering", "disabled"),
            Pair("HKCU\\Software\\Wine\\Direct3D\\UseGLSL", "enabled")
        )
    }
    var editingRegKey by remember { mutableStateOf<Pair<String, String>?>(null) }
    var regValueInput by remember { mutableStateOf("") }

    // Task Manager State
    val processes = remember {
        mutableStateListOf(
            Triple("explorer.exe", "Active", "12 MB"),
            Triple("services.exe", "Active", "4 MB"),
            Triple("wineserver", "Active", "32 MB"),
            Triple("box64", "Active", "48 MB"),
            Triple("virgl_renderer", "Active", "120 MB")
        )
    }

    // Command Prompt Logs
    val commandLogs = remember {
        mutableStateListOf(
            "Rofwin Wine Environment [Version 1.0.0]",
            "(C) Copyright ivansslo / Rofwin. All rights reserved.",
            "",
            "Type 'help' to list available commands.",
            ""
        )
    }
    var terminalInput by remember { mutableStateOf("") }

    // Booting sequence (bisa di-Restart ulang dari Start Menu → Restart)
    LaunchedEffect(isBooting) {
        if (isBooting) {
            bootProgress = 0f
            while (bootProgress < 1f) {
                delay(90)
                bootProgress += 0.05f
            }
            isBooting = false
        }
    }

    // simulated real-time stats (FPS, Temps)
    LaunchedEffect(openWindow) {
        // Stats logic removed (games only)
    }

    // ===== v1.7.0 — Deteksi Critical: journal MT5 -> Notification Center + badge bubble =====
    LaunchedEffect(mt5Journal.size) {
        val head = mt5Journal.firstOrNull() ?: return@LaunchedEffect
        if (detectCritical(head) && aiPluginsOn["critical"] != false && (criticalAlerts.isEmpty() || criticalAlerts.first() != head)) {
            criticalAlerts.add(0, head)
            if (criticalAlerts.size > 15) criticalAlerts.removeLast()
        }
    }

    if (isBooting) {
        // Immersive Booting Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Laptop,
                    contentDescription = "Rofwin",
                    tint = PrimarySky,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Rofwin",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Recovery & Security Edition 1.8.3 — Build 26200.rofwin.rescue",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(32.dp))
                LinearProgressIndicator(
                    progress = bootProgress,
                    color = SecondaryTeal,
                    trackColor = Color(0xFF1E293B),
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Menyiapkan layanan... ${(bootProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }
    } else {
        // ===== v1.7.0 — adaptasi layar apa pun: sinkronisasi device phone/tablet, portrait/landscape =====
        val cfg = LocalConfiguration.current
        val wideScreen = cfg.screenWidthDp >= 600
        // ===== v1.8.1 — dialog overlay auto DIHAPUS (anti-FC); diganti tile Overlay manual di Quick Settings =====
        // Desktop Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B), // Slate 800
                            Color(0xFF0F172A)  // Slate 900
                        )
                    )
                )
        ) {
            // ===== Latar "Bloom" ala Windows 11 (gradient vektor — ringan, tanpa bitmap) =====
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A2E63), Color(0xFF071B3F), Color(0xFF030B1C))
                    )
                )
                // Cahaya biru kanan-atas
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x553B82F6), Color(0x003B82F6)),
                        center = Offset(size.width * 0.75f, size.height * 0.30f),
                        radius = size.minDimension * 0.9f
                    ),
                    radius = size.minDimension * 0.9f,
                    center = Offset(size.width * 0.75f, size.height * 0.30f)
                )
                // Cahaya ungu kiri-bawah
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x408B5CF6), Color(0x008B5CF6)),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.minDimension * 0.8f
                    ),
                    radius = size.minDimension * 0.8f,
                    center = Offset(size.width * 0.15f, size.height * 0.85f)
                )
            }

            // Auto-save logic (prevent data loss during closures)
            LaunchedEffect(Unit) {
                while(true) {
                    delay(60000)
                    // Simulated serialization to storage every 60s
                    android.util.Log.d("Rofwin", "Auto-saving container ${container.name} state to storage...")
                }
            }
            // Desktop Icon Grid (scroll agar tidak overflow di layar kecil)
            // v1.8.3 — FIX FC: duplikat .verticalScroll() DIHAPUS.
            // Dua scrollable searah dalam satu rantai membuat scrollable dalam
            // diukur dengan max-height = tak hingga -> IllegalStateException (FC tepat setelah boot).
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DesktopIconButton(
                    name = "My Computer",
                    icon = Icons.Default.Computer,
                    onClick = { openWin(DesktopWindow.MY_COMPUTER) }
                )
                DesktopIconButton(
                    name = "Web Browser",
                    icon = Icons.Default.Language,
                    onClick = { openWin(DesktopWindow.BROWSER) }
                )
                DesktopIconButton(
                    name = "Rofwin Code",
                    icon = Icons.Default.Code,
                    onClick = { openWin(DesktopWindow.CODE_EDITOR) }
                )
                DesktopIconButton(
                    name = "ROC AI",
                    icon = Icons.Default.SmartToy,
                    onClick = { openWin(DesktopWindow.AI_CHAT) }
                )
                DesktopIconButton(
                    name = "Git Bash",
                    icon = Icons.Default.Terminal,
                    onClick = { openWin(DesktopWindow.GIT_BASH) }
                )
                DesktopIconButton(
                    name = "AI ROC Route",
                    icon = Icons.Default.Route,
                    onClick = { openWin(DesktopWindow.AI_ROUTE) }
                )
                DesktopIconButton(
                    name = "WinRAR",
                    icon = Icons.Default.FolderZip,
                    onClick = { openWin(DesktopWindow.WINRAR) }
                )
                DesktopIconButton(
                    name = "Python 3",
                    icon = Icons.Default.Code,
                    onClick = { openWin(DesktopWindow.PYTHON_SHELL) }
                )
                DesktopIconButton(
                    name = "SSH Connect",
                    icon = Icons.Default.CloudSync,
                    onClick = { openWin(DesktopWindow.SSH_MANAGER) }
                )
                DesktopIconButton(
                    name = "Task Manager",
                    icon = Icons.Default.AlignVerticalBottom,
                    onClick = { openWin(DesktopWindow.TASK_MANAGER) }
                )
                DesktopIconButton(
                    name = "MT5 Setup",
                    icon = Icons.Default.Download,
                    onClick = { openWin(DesktopWindow.MT5_SETUP) }
                )
                DesktopIconButton(
                    name = "APK Studio",
                    icon = Icons.Default.Android,
                    onClick = { openWin(DesktopWindow.APK_STUDIO) }
                )
                DesktopIconButton(
                    name = "VM Builder",
                    icon = Icons.Default.Dns,
                    onClick = { openWin(DesktopWindow.VM_BUILDER) }
                )
            }

            // ===== v1.8.1 — Safe Mode banner =====
            if (safeMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(40f)
                        .padding(top = 44.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFB26A00))
                        .padding(10.dp)
                ) {
                    Text("SAFE MODE AKTIF — sesi tidak dipulihkan, bubble & panel pinned dibatasi.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("jejak: " + (crashPrefs.getString("crumbs", "") ?: "-"), color = Color(0xFFFFECB3), fontSize = 9.sp)
                    Text(
                        "PULIHKAN NORMAL & MULAI ULANG",
                        color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFCC80))
                            .clickable {
                                crashPrefs.edit().putBoolean("safe_next_mode", false).remove("crumbs").remove("last_crash").apply()
                                (context as? android.app.Activity)?.recreate()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // ===== v1.7.0 — AI Bubble NYATA: drag bebas ke mana pun, tap buka ROC AI, badge critical =====
            if (aiBubbleOn && !safeMode) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(bubbleOffset.x.roundToInt(), bubbleOffset.y.roundToInt()) }
                        .zIndex(5f)
                        .size(46.dp)
                        .background(Brush.linearGradient(listOf(Win11AccentSolid, Color(0xFF7B1FA2))), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                bubbleOffset += dragAmount
                            }
                        }
                        .clickable { openWin(DesktopWindow.AI_CHAT) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, "ROC AI Bubble", tint = Color.White, modifier = Modifier.size(22.dp))
                    if (criticalAlerts.isNotEmpty() && aiPluginsOn["critical"] != false) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).size(15.dp).background(Color(0xFFFF1744), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(criticalAlerts.size.coerceAtMost(9).toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Launcher Grid for Professional Tools (Optimized for Mali-G72)
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(180.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "QUICK LAUNCH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SecondaryTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        val quickTools = listOf(
                            Triple("MT5", Icons.Default.TrendingUp, DesktopWindow.MT5),
                            Triple("AI", Icons.Default.SmartToy, DesktopWindow.AI_CHAT),
                            Triple("Code", Icons.Default.Code, DesktopWindow.CODE_EDITOR),
                            Triple("PS1", Icons.Default.Terminal, DesktopWindow.COMMAND_PROMPT),
                            Triple("Wine", Icons.Default.Settings, DesktopWindow.WINECFG),
                            Triple("Tricks", Icons.Default.Handyman, DesktopWindow.WINETRICKS),
                            Triple("WSL", Icons.Default.Terminal, DesktopWindow.GIT_BASH),
                            Triple("Web", Icons.Default.Language, DesktopWindow.BROWSER),
                            Triple("Python", Icons.Default.PlayCircle, DesktopWindow.PYTHON_SHELL),
                            Triple("Folder", Icons.Default.Folder, DesktopWindow.MY_COMPUTER),
                            Triple("Setup", Icons.Default.Download, DesktopWindow.MT5_SETUP),
                            Triple("APK", Icons.Default.Android, DesktopWindow.APK_STUDIO),
                            Triple("VM", Icons.Default.Dns, DesktopWindow.VM_BUILDER)
                        )
                        items(quickTools) { (name, icon, win) ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { openWin(win) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Interactive Windows (Windows 11: rounded corners, shadow, caption buttons)
            if (openWindow != DesktopWindow.NONE) {
                val winShape = if (isMaximized || isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
                val windowModifier = when {
                    isFullscreen -> Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .background(Color(0xFF202020))
                    isMaximized -> Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp)
                        .zIndex(2f)
                        .background(Color(0xFF202020))
                    else -> Modifier
                        .offset { IntOffset(windowOffset.x.roundToInt(), windowOffset.y.roundToInt()) }
                        .fillMaxWidth(if (wideScreen) 0.72f else 0.96f)
                        .fillMaxHeight(if (wideScreen) 0.86f else 0.72f)
                        .shadow(24.dp, winShape, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(winShape)
                        .background(Color(0xFF202020))
                        .border(0.5.dp, Win11Stroke, winShape)
                }
                Box(modifier = windowModifier) {
                    Column {
                        // Title bar (Windows 11 — gelap, caption buttons putih)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2B2B2B))
                                .pointerInput(isMaximized, isFullscreen) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (!isMaximized && !isFullscreen) {
                                            windowOffset += dragAmount
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getWindowIcon(openWindow),
                                    contentDescription = null,
                                    tint = Win11Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = getWindowTitle(openWindow),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color(0xFFE8E8E8),
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // [—] Minimize
                                IconButton(
                                    onClick = {
                                        minimizedWindows.add(openWindow)
                                        openWindow = DesktopWindow.NONE
                                        isMaximized = false
                                        isFullscreen = false
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Minimize, contentDescription = "Minimize", tint = Color(0xFFE8E8E8), modifier = Modifier.size(18.dp))
                                }
                                // [□] Maximize / Restore (kotak gaya Windows 11)
                                IconButton(
                                    onClick = {
                                        if (isFullscreen) {
                                            isFullscreen = false
                                            isMaximized = true
                                        } else {
                                            isMaximized = !isMaximized
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMaximized || isFullscreen) Icons.Default.FilterNone else Icons.Default.CropSquare,
                                        contentDescription = if (isMaximized || isFullscreen) "Restore Down" else "Maximize",
                                        tint = Color(0xFFE8E8E8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                // [⛶] Full Screen sejati (F11 — menutupi taskbar)
                                IconButton(
                                    onClick = {
                                        isFullscreen = !isFullscreen
                                        if (isFullscreen) isMaximized = false
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                        contentDescription = if (isFullscreen) "Exit Full Screen" else "Full Screen (F11)",
                                        tint = if (isFullscreen) Win11Accent else Color(0xFFE8E8E8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                // [✕] Close
                                IconButton(
                                    onClick = {
                                        openWindow = DesktopWindow.NONE
                                        isMaximized = false
                                        isFullscreen = false
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFE8E8E8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Window Content Area
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1F1F1F))
                        ) {
                            when (openWindow) {
                                DesktopWindow.MY_COMPUTER -> {
                                    // File Explorer View
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Path bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkSurface)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    currentPath = parentPath(currentPath)
                                                },
                                                enabled = currentPath.trimEnd('\\').length > 2
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            // Address bar bisa diketik — Enter untuk navigasi
                                            var pathInput by remember(currentPath) { mutableStateOf(currentPath) }
                                            BasicTextField(
                                                value = pathInput,
                                                onValueChange = { pathInput = it },
                                                singleLine = true,
                                                textStyle = TextStyle(fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                                                cursorBrush = SolidColor(Win11Accent),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                                keyboardActions = KeyboardActions(onGo = {
                                                    var t = pathInput.trim().replace('/', '\\')
                                                    if (t.length >= 2 && t[1] == ':' && !t.endsWith('\\') && simulatedFiles.containsKey(t)) {
                                                        // path folder valid tanpa backslash akhir
                                                    } else if (t.endsWith('\\') && simulatedFiles.containsKey(t)) {
                                                        // root valid
                                                    } else if (simulatedFiles.containsKey(t)) {
                                                        // ok
                                                    }
                                                    if (simulatedFiles.containsKey(t)) {
                                                        currentPath = t
                                                    } else {
                                                        pathInput = currentPath
                                                    }
                                                }),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            )
                                        }

                                        // File Actions (Create / Delete / CRUD)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = newFileName,
                                                onValueChange = { newFileName = it },
                                                placeholder = { Text("New file name...", fontSize = 12.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = fileTypeFolder,
                                                    onCheckedChange = { fileTypeFolder = it }
                                                )
                                                Text("Folder", fontSize = 11.sp, color = Color.White)
                                            }
                                            Button(
                                                onClick = {
                                                    if (newFileName.isNotBlank()) {
                                                        val list = simulatedFiles[currentPath]?.toMutableList() ?: mutableListOf()
                                                        list.add(SimFile(newFileName, fileTypeFolder, if (fileTypeFolder) "Folder" else "1 KB"))
                                                        simulatedFiles[currentPath] = list
                                                        if (fileTypeFolder) {
                                                            // Folder baru dapat dinavigasi (Explorer + cd di Terminal)
                                                            val newPath = currentPath.trimEnd('\\') + "\\" + newFileName
                                                            simulatedFiles[newPath] = emptyList()
                                                            simulatedFiles[newPath + "\\"] = emptyList()
                                                        }
                                                        newFileName = ""
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("Add", fontSize = 11.sp)
                                            }
                                        }

                                        // Disk Selection list
                                        if (currentPath == "C:\\" || currentPath == "D:\\" || currentPath == "E:\\" || currentPath == "Z:\\") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp)
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Card(
                                                    modifier = Modifier
                                                        .width(120.dp)
                                                        .clickable { currentPath = "C:\\" },
                                                    colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("C:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(32.dp))
                                                        Text("Local Disk (C:)", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                if (dDriveEnabled) {
                                                    Card(
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .clickable { currentPath = "D:\\" },
                                                        colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("D:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(32.dp))
                                                            Text("OBB Space (D:)", fontSize = 11.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                                if (eDriveEnabled) {
                                                    Card(
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .clickable { currentPath = "E:\\" },
                                                        colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("E:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(32.dp))
                                                            Text("Downloads (E:)", fontSize = 11.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                                if (zDriveEnabled) {
                                                    Card(
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .clickable { currentPath = "Z:\\" },
                                                        colors = CardDefaults.cardColors(containerColor = if (currentPath.startsWith("Z:")) PrimarySky.copy(alpha = 0.2f) else DarkSurface)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Memory, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                                                            Text("Root FS (Z:)", fontSize = 11.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Files / Folders List
                                        val files = simulatedFiles[currentPath] ?: emptyList()
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(files) { file ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (file.isDirectory) {
                                                                val newPath = currentPath.trimEnd('\\') + "\\" + file.name
                                                                if (simulatedFiles.containsKey(newPath)) {
                                                                    currentPath = newPath
                                                                } else {
                                                                    // Daftarkan folder belum dikenal agar bisa dinavigasi
                                                                    simulatedFiles[newPath] = emptyList()
                                                                    currentPath = newPath
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = if (file.isDirectory) SecondaryTeal else Color.LightGray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(file.name, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(file.size, fontSize = 11.sp, color = TextSecondary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = {
                                                                val list = simulatedFiles[currentPath]?.toMutableList() ?: mutableListOf()
                                                                list.remove(file)
                                                                simulatedFiles[currentPath] = list
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                DesktopWindow.REGISTRY_EDITOR -> {
                                    // Regedit interface
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        Text(
                                            "Registry Editor (regedit.exe)",
                                            style = MaterialTheme.typography.titleSmall.copy(color = SecondaryTeal)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Tuning keys for Direct3D Rendering on Mali-G72:",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = {
                                                val n = registryKeys.size + 1
                                                registryKeys.add(Pair("HKCU\\Software\\Wine\\Custom\\NewKey$n", "0x00000000 (0)"))
                                            }) {
                                                Text("＋ New", fontSize = 11.sp)
                                            }
                                        }

                                        LazyColumn(modifier = Modifier.weight(1f).border(1.dp, Color.Gray).background(DarkSurface)) {
                                            items(registryKeys) { key ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            editingRegKey = key
                                                            regValueInput = key.second
                                                        }
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(0.6f)) {
                                                        Text(key.first.substringAfterLast("\\"), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text(key.first, fontSize = 9.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    Text(key.second, fontSize = 11.sp, color = Win11Accent, modifier = Modifier.weight(0.4f), textAlign = TextAlign.End)
                                                    IconButton(onClick = { registryKeys.remove(key) }, modifier = Modifier.size(20.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete key", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                                                    }
                                                }
                                            }
                                        }

                                        // Editing Key Overlay dialog
                                        editingRegKey?.let { key ->
                                            AlertDialog(
                                                onDismissRequest = { editingRegKey = null },
                                                title = { Text("Edit String Key", fontSize = 14.sp) },
                                                text = {
                                                    Column {
                                                        Text(key.first, fontSize = 11.sp, color = TextSecondary)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        TextField(
                                                            value = regValueInput,
                                                            onValueChange = { regValueInput = it },
                                                            singleLine = true
                                                        )
                                                    }
                                                },
                                                confirmButton = {
                                                    Button(onClick = {
                                                        val index = registryKeys.indexOfFirst { it.first == key.first }
                                                        if (index != -1) {
                                                            registryKeys[index] = Pair(key.first, regValueInput)
                                                        }
                                                        editingRegKey = null
                                                    }) {
                                                        Text("Save")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { editingRegKey = null }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                DesktopWindow.TASK_MANAGER -> {
                                    // Task Manager — CPU/RAM hidup, End Process mengurangi beban, Run new task
                                    var cpuPct by remember { mutableStateOf(23) }
                                    var memPct by remember { mutableStateOf(48) }
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            delay(1600)
                                            cpuPct = (cpuPct + (-7..7).random()).coerceAtLeast(3).coerceAtMost(97)
                                            memPct = (35 + processes.size * 9 + (-3..3).random()).coerceAtLeast(20).coerceAtMost(95)
                                        }
                                    }
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Task Manager", style = MaterialTheme.typography.titleSmall.copy(color = Win11Accent))
                                            TextButton(onClick = { openWin(DesktopWindow.COMMAND_PROMPT) }) {
                                                Text("▶ Run new task", fontSize = 11.sp)
                                            }
                                        }
                                        // CPU & Memory bars (hidup)
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("CPU  $cpuPct%", fontSize = 10.sp, color = Color(0xFFD0D0D0))
                                                LinearProgressIndicator(
                                                    progress = cpuPct / 100f,
                                                    color = Win11Accent,
                                                    trackColor = Color(0xFF333333),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("RAM  $memPct% (4GB)", fontSize = 10.sp, color = Color(0xFFD0D0D0))
                                                LinearProgressIndicator(
                                                    progress = memPct / 100f,
                                                    color = SecondaryTeal,
                                                    trackColor = Color(0xFF333333),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                )
                                            }
                                        }

                                        LazyColumn(modifier = Modifier.weight(1f).border(1.dp, Color.Gray)) {
                                            items(processes) { proc ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(DarkSurface)
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(proc.first, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text("Memory: ${proc.third}", fontSize = 10.sp, color = TextSecondary)
                                                    }
                                                    Button(
                                                        onClick = { processes.remove(proc) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("End Process", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                }
                                DesktopWindow.COMMAND_PROMPT -> {
                                    // Interactive Terminal (cmd.exe + powershell.exe mode)
                                    TerminalWindow(
                                        container = container,
                                        commandLogs = commandLogs,
                                        terminalInput = terminalInput,
                                        onInputChange = { terminalInput = it },
                                        onLaunch = { w -> openWin(w) },
                                        simulatedFiles = simulatedFiles
                                    )
                                }
                                DesktopWindow.WINRAR -> {
                                    WinRarWindow(simulatedFiles)
                                }
                                DesktopWindow.PYTHON_SHELL -> {
                                    PythonShellWindow()
                                }
                                DesktopWindow.SSH_MANAGER -> {
                                    SshManagerWindow()
                                }
                                DesktopWindow.MT5 -> {
                                    Mt5Window(
                                        lowRam = lowRamMode,
                                        mids = mt5Mids,
                                        prevMids = mt5PrevMids,
                                        candles = mt5Candles,
                                        positions = mt5Positions,
                                        tradeHistory = mt5History,
                                        journalLogs = mt5Journal,
                                        activeEAs = mt5ActiveEAs,
                                        balance = mt5Balance,
                                        onBalance = { mt5Balance = it },
                                        ticket = mt5Ticket,
                                        onTicket = { mt5Ticket = it },
                                        expertAdvisors = expertAdvisors,
                                        account = mt5Account,
                                        onAccount = { mt5Account = it }
                                    )
                                }
                                DesktopWindow.AI_CHAT -> {
                                    RocAiWindow(
                                        fileContents = fileContents,
                                        simulatedFiles = simulatedFiles,
                                        expertAdvisors = expertAdvisors,
                                        mids = mt5Mids,
                                        positions = mt5Positions,
                                        balance = mt5Balance,
                                        pluginsOn = aiPluginsOn,
                                        bubbleOn = aiBubbleOn,
                                        onBubbleChange = { aiBubbleOn = it },
                                        onLaunch = { w -> openWin(w) }
                                    )
                                }
                                DesktopWindow.MQL5_EDITOR -> {
                                    // MQL5 Editor kini = Rofwin Code yang langsung membuka Expert.mq5
                                    CodeEditorWindow(
                                        fileContents = fileContents,
                                        simulatedFiles = simulatedFiles,
                                        expertAdvisors = expertAdvisors,
                                        eaActive = mt5ActiveEAs.size,
                                        initialPath = "C:\\MQL5\\Experts\\Expert.mq5"
                                    )
                                }
                                DesktopWindow.CODE_EDITOR -> {
                                    CodeEditorWindow(
                                        fileContents = fileContents,
                                        simulatedFiles = simulatedFiles,
                                        expertAdvisors = expertAdvisors,
                                        eaActive = mt5ActiveEAs.size,
                                        initialPath = null
                                    )
                                }
                                DesktopWindow.WINECFG -> {
                                    WineCfgWindow()
                                }
                                DesktopWindow.WINETRICKS -> {
                                    WinetricksWindow()
                                }
                                DesktopWindow.BROWSER -> {
                                    BrowserWindow()
                                }
                                DesktopWindow.GIT_BASH -> {
                                    GitBashWindow(simulatedFiles)
                                }
                                DesktopWindow.AI_ROUTE -> {
                                    AiRouteWindow()
                                }
                                DesktopWindow.MT5_SETUP -> {
                                    Mt5SetupWindow(
                                        simulatedFiles = simulatedFiles,
                                        journalLogs = mt5Journal,
                                        installed = mt5Installed,
                                        onInstalled = { mt5Installed = it }
                                    )
                                }
                                DesktopWindow.APK_STUDIO -> {
                                    ApkStudioWindow(
                                        simulatedFiles = simulatedFiles,
                                        journalLogs = mt5Journal
                                    )
                                }
                                DesktopWindow.VM_BUILDER -> {
                                    VmBuilderWindow(
                                        simulatedFiles = simulatedFiles,
                                        journalLogs = mt5Journal
                                    )
                                }
                                DesktopWindow.DX_DIAG -> {
                                    // dxdiag details
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp).background(DarkSurface)) {
                                        Text("DirectX Diagnostic Tool", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
                                        Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp))

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            item { Text("System Information", fontWeight = FontWeight.Bold, color = PrimarySky, fontSize = 12.sp) }
                                            item { DxDiagRow("Current Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) }
                                            item { DxDiagRow("Computer Name", "ROFWIN-VIRT-PC") }
                                            item { DxDiagRow("Operating System", "Wine 8.0.2 Emulation Layer (Windows 10 x64)") }
                                            item { DxDiagRow("Processor", "MediaTek Helio P60 (MT6771) @ 2.00GHz (8 CPUs)") }
                                            item { DxDiagRow("Memory", "4096MB RAM (Dedicated to VirGL)") }
                                            item { DxDiagRow("Page file", "512MB used, 3584MB available") }
                                            item { DxDiagRow("DirectX Version", "DirectX 11 (WineD3D Emulated)") }

                                            item { Spacer(modifier = Modifier.height(8.dp)) }
                                            item { Text("Graphics Display Optimization Advice", fontWeight = FontWeight.Bold, color = SecondaryTeal, fontSize = 12.sp) }
                                            item { DxDiagRow("Direct3D Renderer", "OpenGL VirGL Emulator (via Mali-G72)") }
                                            item { DxDiagRow("Vulkan status", "None (Deactivated to prevent Mali shader crash)") }
                                            item { DxDiagRow("Recommended Res", "800x600 for performance (Helio P60)") }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // ===== Windows 11 Taskbar (terpusat ABSOLUT — 1.5.0) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Win11Taskbar)
                    .align(Alignment.BottomCenter)
                    .border(width = (0.5).dp, color = Win11Stroke)
            ) {
                // Kiri: Widgets — absolute start
                Row(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        widgetsOpen = !widgetsOpen
                        qsOpen = false; notifOpen = false; startMenuOpen = false
                    }) {
                        Icon(Icons.Default.WbSunny, contentDescription = "Widgets", tint = Color(0xFF6EB5FF), modifier = Modifier.size(20.dp))
                    }
                }

                // Tengah: Start + pinned + running — absolute center + scrollable (compact CPH1823)
                Row(
                    modifier = Modifier.align(Alignment.Center).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                startMenuOpen = !startMenuOpen
                                widgetsOpen = false; qsOpen = false; notifOpen = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Win11Logo(Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    // ===== v1.8.1 — pinned taskbar anti-FC: detectTapGestures (tanpa Popup/DropdownMenu di frame-1) =====
                    val running = listOfNotNull(openWindow.takeIf { it != DesktopWindow.NONE }) + minimizedWindows
                    val taskItems = if (safeMode) running.distinct() else (taskbarPinned + running).distinct()
                    taskItems.forEach { win ->
                        val isOpen = win == openWindow
                        val isRunning = running.contains(win)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(37.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .pointerInput(win, taskbarLocked) {
                                    detectTapGestures(
                                        onTap = {
                                            if (isOpen) {
                                                minimizedWindows.add(win)
                                                openWindow = DesktopWindow.NONE
                                                isMaximized = false
                                                isFullscreen = false
                                            } else {
                                                minimizedWindows.remove(win)
                                                openWin(win)
                                            }
                                        },
                                        onLongPress = { pinnedMenuFor = win }
                                    )
                                }
                        ) {
                            Icon(
                                getWindowIcon(win),
                                contentDescription = getWindowTitle(win),
                                tint = if (isOpen) Win11Accent else Color(0xFFE8E8E8),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 3.dp)
                                    .width(if (isOpen) 14.dp else if (isRunning) 6.dp else 0.dp)
                                    .height(3.dp)
                                    .background(
                                        if (isOpen || isRunning) Win11Accent else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }

                // ===== v1.8.1 — panel aksi taskbar (Box biasa, anti Popup-crash ColorOS) =====
                val menuWin = pinnedMenuFor
                if (menuWin != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-48).dp)
                            .zIndex(50f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Win11Card)
                            .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(10.dp))
                            .width(210.dp)
                    ) {
                        val isPinn = taskbarPinned.contains(menuWin)
                        val idxP = taskbarPinned.indexOf(menuWin)
                        if (taskbarLocked) {
                            Text("🔒 Taskbar terkunci — buka kunci dulu", fontSize = 9.sp, color = Color(0xFF808080), modifier = Modifier.padding(10.dp, 6.dp))
                        }
                        TaskMenuItem("Buka") { pinnedMenuFor = null; minimizedWindows.remove(menuWin); openWin(menuWin) }
                        TaskMenuItem("◄ Geser kiri", enabled = !taskbarLocked && isPinn && idxP > 0) { taskbarPinned.add(idxP - 1, taskbarPinned.removeAt(idxP)); pinnedMenuFor = null }
                        TaskMenuItem("Geser kanan ►", enabled = !taskbarLocked && isPinn && idxP in 0 until taskbarPinned.size - 1) { taskbarPinned.add(idxP + 1, taskbarPinned.removeAt(idxP)); pinnedMenuFor = null }
                        if (isPinn) {
                            TaskMenuItem("Lepas sematan", enabled = !taskbarLocked) { taskbarPinned.remove(menuWin); pinnedMenuFor = null }
                        } else {
                            TaskMenuItem("Sematkan ke taskbar", enabled = !taskbarLocked) { taskbarPinned.add(menuWin); pinnedMenuFor = null }
                        }
                        TaskMenuItem(if (taskbarLocked) "🔓 Buka kunci taskbar" else "🔒 Kunci taskbar") { taskbarLocked = !taskbarLocked; pinnedMenuFor = null }
                        TaskMenuItem("Tutup panel") { pinnedMenuFor = null }
                    }
                }


                // Kanan: tray — absolute end
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Show hidden icons", tint = Color(0xFFB0B0B0), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                qsOpen = !qsOpen
                                widgetsOpen = false; notifOpen = false; startMenuOpen = false
                            }
                            .padding(horizontal = 5.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = Color(0xFFE8E8E8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color(0xFFE8E8E8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.BatteryStd, contentDescription = "Battery", tint = Color(0xFFE8E8E8), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs)),
                            color = Color(0xFFE8E8E8), fontSize = 11.sp
                        )
                        Text(
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(nowMs)),
                            color = Color(0xFFB0B0B0), fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                notifOpen = !notifOpen
                                qsOpen = false; widgetsOpen = false; startMenuOpen = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifikasi", tint = Color(0xFFE8E8E8), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // Overlay penutup panel-panel (klik di luar)
            if (widgetsOpen || qsOpen || notifOpen || startMenuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(4f)
                        .clickable {
                            widgetsOpen = false; qsOpen = false; notifOpen = false; startMenuOpen = false; powerOpen = false
                        }
                )
            }

            // ===== Widgets (gaya Windows 11) =====
            if (widgetsOpen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = (-52).dp)
                        .zIndex(6f)
                        .fillMaxWidth(0.8f)
                        .widthIn(max = 280.dp)
                        .background(Win11Card, RoundedCornerShape(12.dp))
                        .border(0.5.dp, Win11Stroke, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Widgets", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("29° Cerah sebagian", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Jambi City, ID · diperbarui baru saja", color = Color(0xFFB0B0B0), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    listOf(
                        "Rofwin 1.4.0: Windows 11 experience mendarat",
                        "MT5 sim: 7 pair feed lokal berjalan",
                        "Tip: aktifkan Low-RAM via Quick Settings"
                    ).forEach { n ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Win11Accent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(n, color = Color(0xFFE0E0E0), fontSize = 11.sp)
                        }
                    }
                }
            }

            // ===== Quick Settings — slider volume & brightness NYATA (mengubah HP) =====
            if (qsOpen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = (-52).dp)
                        .zIndex(6f)
                        .fillMaxWidth(0.82f)
                        .widthIn(max = 300.dp)
                        .background(Win11Card, RoundedCornerShape(12.dp))
                        .border(0.5.dp, Win11Stroke, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QsTile(icon = Icons.Default.Wifi, label = "Wi-Fi", on = true, modifier = Modifier.weight(1f)) {}
                        QsTile(icon = Icons.Default.Bluetooth, label = "Bluetooth", on = false, modifier = Modifier.weight(1f)) {}
                        QsTile(icon = Icons.Default.Memory, label = "Low-RAM", on = lowRamMode, modifier = Modifier.weight(1f)) {
                            lowRamMode = !lowRamMode
                            prefs.edit().putBoolean("low_ram", lowRamMode).apply()
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QsTile(icon = Icons.Default.SmartToy, label = "AI Bubble", on = aiBubbleOn, modifier = Modifier.weight(1f)) { aiBubbleOn = !aiBubbleOn }
                        QsTile(icon = Icons.Default.ScreenRotation, label = "Landscape", on = landscapeLocked, modifier = Modifier.weight(1f)) {
                            landscapeLocked = !landscapeLocked
                            (context as? android.app.Activity)?.requestedOrientation = if (landscapeLocked) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                        }
                        QsTile(icon = Icons.Default.Lock, label = "Kunci Bar", on = taskbarLocked, modifier = Modifier.weight(1f)) { taskbarLocked = !taskbarLocked }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QsTile(icon = Icons.Default.OpenInNew, label = "Overlay", on = Settings.canDrawOverlays(context), modifier = Modifier.weight(1f)) {
                            try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                        }
                        QsTile(icon = Icons.Default.Shield, label = "Safe Mode", on = safeMode, modifier = Modifier.weight(1f)) {
                            crashPrefs.edit().putBoolean("safe_next_mode", !safeMode).apply()
                            android.widget.Toast.makeText(context, if (!safeMode) "Safe Mode AKTIF setelah mulai ulang app" else "Mode NORMAL setelah mulai ulang app", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Volume NYATA — mengubah volume media Android
                    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
                    var vol by remember { mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Slider(
                            value = vol,
                            onValueChange = { v ->
                                vol = v
                                val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * max).toInt().coerceIn(0, max), 0)
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                    // Brightness NYATA — mengubah kecerahan layar aplikasi
                    val activity = context as? android.app.Activity
                    var bri by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.8f) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LightMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Slider(
                            value = bri,
                            onValueChange = { v ->
                                bri = v
                                activity?.window?.let { w ->
                                    val lp = w.attributes
                                    lp.screenBrightness = v.coerceIn(0.05f, 1f)
                                    w.attributes = lp
                                }
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // ===== Notification Center =====
            if (notifOpen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = (-52).dp)
                        .zIndex(6f)
                        .fillMaxWidth(0.85f)
                        .widthIn(max = 300.dp)
                        .background(Win11Card, RoundedCornerShape(12.dp))
                        .border(0.5.dp, Win11Stroke, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Notifications", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (criticalAlerts.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Critical terdeteksi (${criticalAlerts.size})", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Clear", color = Color(0xFFFF8A80), fontSize = 9.sp, modifier = Modifier.clickable { criticalAlerts.clear() }.padding(4.dp))
                        }
                        criticalAlerts.take(3).forEach { c ->
                            Text("• " + c.take(90), color = Color(0xFFFF8A80), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    listOf(
                        Triple("Rofwin", "Windows 11 UI aktif di container ini", "baru saja"),
                        Triple("MetaTrader 5 (sim)", "Feed lokal 7 pair berjalan", "1 menit"),
                        Triple("Terminal", "Ketik 'help' — 30+ perintah hidup", "5 menit")
                    ).forEach { (app, msg, time) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Win11Accent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(msg, color = Color(0xFFD0D0D0), fontSize = 11.sp)
                            }
                            Text(time, color = Color(0xFF808080), fontSize = 9.sp)
                        }
                    }
                    TextButton(onClick = { notifOpen = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear all", fontSize = 11.sp)
                    }
                }
            }

            // ===== Start Menu Windows 11 (search hidup, pinned grid, power) =====
            if (startMenuOpen && !isFullscreen) {
                val startApps = listOf(
                    StartApp("File Explorer", Icons.Default.Folder, DesktopWindow.MY_COMPUTER),
                    StartApp("Google Chrome", Icons.Default.Language, DesktopWindow.BROWSER),
                    StartApp("Terminal", Icons.Default.Terminal, DesktopWindow.COMMAND_PROMPT),
                    StartApp("MetaTrader 5", Icons.Default.TrendingUp, DesktopWindow.MT5),
                    StartApp("ROC AI Assistant", Icons.Default.SmartToy, DesktopWindow.AI_CHAT),
                    StartApp("Rofwin Code", Icons.Default.Code, DesktopWindow.CODE_EDITOR),
                    StartApp("MetaEditor (MQL5)", Icons.Default.Code, DesktopWindow.MQL5_EDITOR),
                    StartApp("Wine Config", Icons.Default.Settings, DesktopWindow.WINECFG),
                    StartApp("Winetricks", Icons.Default.Handyman, DesktopWindow.WINETRICKS),
                    StartApp("Python 3.12", Icons.Default.Code, DesktopWindow.PYTHON_SHELL),
                    StartApp("Git Bash", Icons.Default.Terminal, DesktopWindow.GIT_BASH),
                    StartApp("SSH Manager", Icons.Default.CloudSync, DesktopWindow.SSH_MANAGER),
                    StartApp("WinRAR", Icons.Default.FolderZip, DesktopWindow.WINRAR),
                    StartApp("Task Manager", Icons.Default.AlignVerticalBottom, DesktopWindow.TASK_MANAGER),
                    StartApp("Registry Editor", Icons.Default.Settings, DesktopWindow.REGISTRY_EDITOR),
                    StartApp("AI ROC Route", Icons.Default.Route, DesktopWindow.AI_ROUTE),
                    StartApp("MT5 Setup", Icons.Default.Download, DesktopWindow.MT5_SETUP),
                    StartApp("APK Studio", Icons.Default.Android, DesktopWindow.APK_STUDIO),
                    StartApp("VM Builder", Icons.Default.Dns, DesktopWindow.VM_BUILDER),
                    StartApp("DirectX Diag", Icons.Default.Info, DesktopWindow.DX_DIAG)
                )
                var startQuery by remember { mutableStateOf("") }
                val filtered = if (startQuery.isBlank()) startApps else startApps.filter { it.title.contains(startQuery, ignoreCase = true) }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-56).dp)
                        .zIndex(6f)
                        .fillMaxWidth(0.94f)
                        .widthIn(max = 360.dp)
                        .background(Win11Card, RoundedCornerShape(12.dp))
                        .border(0.5.dp, Win11Stroke, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    // Search bar (filter hidup)
                    TextField(
                        value = startQuery,
                        onValueChange = { startQuery = it },
                        placeholder = { Text("Type here to search", fontSize = 12.sp, color = Color(0xFF909090)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB0B0B0), modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF333333),
                            unfocusedContainerColor = Color(0xFF333333),
                            focusedIndicatorColor = Win11Accent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pinned", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("All apps ›", color = Color(0xFFB0B0B0), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        openWin(app.window)
                                        startMenuOpen = false
                                        startQuery = ""
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(app.icon, contentDescription = app.title, tint = Win11Accent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    app.title,
                                    color = Color(0xFFE8E8E8),
                                    fontSize = 9.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (startQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recommended", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf(
                            "MT5 — Sell/Buy satu klik aktif" to DesktopWindow.MT5,
                            "PowerShell — alias Get-* mendukung" to DesktopWindow.COMMAND_PROMPT
                        ).forEach { (label, win) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { openWin(win); startMenuOpen = false }
                                    .padding(vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFB0B0B0), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color(0xFFD0D0D0), fontSize = 10.sp)
                            }
                        }
                    }
                    Divider(color = Win11Stroke, modifier = Modifier.padding(vertical = 8.dp))
                    // Footer: user + power
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(28.dp).background(Win11AccentSolid, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Administrator", color = Color.White, fontSize = 12.sp)
                        }
                        Box {
                            IconButton(onClick = { powerOpen = !powerOpen }) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            if (powerOpen) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(y = (-52).dp)
                                        .zIndex(8f)
                                        .width(140.dp)
                                        .background(Win11Card, RoundedCornerShape(8.dp))
                                        .border(0.5.dp, Win11Stroke, RoundedCornerShape(8.dp))
                                ) {
                                    ListItemPower("Shut down", Icons.Default.PowerSettingsNew) { onClose() }
                                    ListItemPower("Restart", Icons.Default.Refresh) {
                                        startMenuOpen = false
                                        powerOpen = false
                                        isBooting = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Logo Windows 11 (4 kotak biru)
@Composable
fun Win11Logo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 2f * 0.92f
        val gap = size.minDimension * 0.08f
        val c = Color(0xFF2CB9F0)
        drawRect(c, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cell, cell))
        drawRect(c, topLeft = Offset(cell + gap, 0f), size = androidx.compose.ui.geometry.Size(cell, cell))
        drawRect(c, topLeft = Offset(0f, cell + gap), size = androidx.compose.ui.geometry.Size(cell, cell))
        drawRect(c, topLeft = Offset(cell + gap, cell + gap), size = androidx.compose.ui.geometry.Size(cell, cell))
    }
}

@Composable
fun QsTile(icon: ImageVector, label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) Win11AccentSolid else Color(0xFF3A3A3A))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, color = Color.White, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun ListItemPower(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun DesktopIconButton(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                .border(0.5.dp, Win11Stroke, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = Color(0xFFBEE3F8), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StartMenuItem(
    text: String,
    icon: ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
fun DxDiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.4f))
        Text(value, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}

fun getWindowIcon(window: DesktopWindow): ImageVector {
    return when (window) {
        DesktopWindow.MY_COMPUTER -> Icons.Default.Computer
        DesktopWindow.REGISTRY_EDITOR -> Icons.Default.Settings
        DesktopWindow.TASK_MANAGER -> Icons.Default.AlignVerticalBottom
        DesktopWindow.COMMAND_PROMPT -> Icons.Default.Terminal
        DesktopWindow.DX_DIAG -> Icons.Default.Info
        DesktopWindow.BROWSER -> Icons.Default.Language
        DesktopWindow.GIT_BASH -> Icons.Default.Terminal
        DesktopWindow.AI_ROUTE -> Icons.Default.Route
        DesktopWindow.WINRAR -> Icons.Default.FolderZip
        DesktopWindow.PYTHON_SHELL -> Icons.Default.Code
        DesktopWindow.SSH_MANAGER -> Icons.Default.CloudSync
        DesktopWindow.MT5 -> Icons.Default.TrendingUp
        DesktopWindow.MQL5_EDITOR -> Icons.Default.Code
        DesktopWindow.CODE_EDITOR -> Icons.Default.Code
        DesktopWindow.WINECFG -> Icons.Default.Settings
        DesktopWindow.WINETRICKS -> Icons.Default.Handyman
        DesktopWindow.AI_CHAT -> Icons.Default.SmartToy
        DesktopWindow.APK_STUDIO -> Icons.Default.Android
        DesktopWindow.VM_BUILDER -> Icons.Default.Dns
        DesktopWindow.MT5_SETUP -> Icons.Default.Download
        else -> Icons.Default.Laptop
    }
}

fun getWindowTitle(window: DesktopWindow): String {
    return when (window) {
        DesktopWindow.MY_COMPUTER -> "My Computer"
        DesktopWindow.REGISTRY_EDITOR -> "Registry Editor (regedit.exe)"
        DesktopWindow.TASK_MANAGER -> "Wine Task Manager"
        DesktopWindow.COMMAND_PROMPT -> "Command Prompt / PowerShell"
        DesktopWindow.DX_DIAG -> "DirectX Diagnostic Tool (dxdiag)"
        DesktopWindow.BROWSER -> "Google Chrome"
        DesktopWindow.GIT_BASH -> "Git Bash Terminal"
        DesktopWindow.AI_ROUTE -> "AI ROC-AgentsRoute v1.0"
        DesktopWindow.WINRAR -> "WinRAR (Unregistered Evaluation Copy)"
        DesktopWindow.PYTHON_SHELL -> "Python 3.12.1 Shell"
        DesktopWindow.SSH_MANAGER -> "SSH Connection Manager"
        DesktopWindow.MT5 -> "MetaTrader 5 — Rofwin Demo"
        DesktopWindow.MQL5_EDITOR -> "MetaEditor — Expert.mq5"
        DesktopWindow.CODE_EDITOR -> "Rofwin Code"
        DesktopWindow.WINECFG -> "Wine Configuration (winecfg)"
        DesktopWindow.WINETRICKS -> "Winetricks — Windows DLLs & Runtimes"
        DesktopWindow.AI_CHAT -> "ROC AI — Trading & Code Assistant"
        DesktopWindow.APK_STUDIO -> "APK Studio — Android Build (Windows Compile)"
        DesktopWindow.VM_BUILDER -> "VM Builder — Windows OS Image + OCI Bridge"
        DesktopWindow.MT5_SETUP -> "MT5 Setup — Download & Install"
        else -> "Wine Window"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalWindow(
    container: WineContainer,
    commandLogs: MutableList<String>,
    terminalInput: String,
    onInputChange: (String) -> Unit,
    onLaunch: (DesktopWindow) -> Unit,
    simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>
) {
    // Mode terminal: cmd.exe (hitam-hijau) atau powershell.exe (biru-putih)
    var psMode by remember { mutableStateOf(false) }
    var cwd by remember { mutableStateOf("C:\\Windows\\System32") }
    val bgColor = if (psMode) Color(0xFF012456) else Color.Black
    val fgColor = if (psMode) Color.White else Color.Green
    val prompt = if (psMode) "PS $cwd> " else "$cwd>"

    fun exec(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        commandLogs.add("$prompt$input")
        val cmd = input.lowercase()
        val arg = input.substringAfter(' ', "").trim()

        fun unknown() {
            if (psMode) commandLogs.add("$input : The term '$input' is not recognized as the name of a cmdlet, function, script file, or operable program.")
            else commandLogs.add("'$input' is not recognized as an internal or external command, operable program or batch file.")
        }

        when {
            // ---- Mode switching ----
            psMode && (cmd == "exit" || cmd == "cmd" || cmd == "cmd.exe") -> {
                psMode = false
                commandLogs.add("Leaving Windows PowerShell session...")
            }
            !psMode && (cmd == "powershell" || cmd == "powershell.exe" || cmd == "pwsh" || cmd.startsWith("ps1 ")) -> {
                psMode = true
                commandLogs.add("Windows PowerShell")
                commandLogs.add("Copyright (C) Rofwin Corporation. All rights reserved.")
                commandLogs.add("")
                commandLogs.add("Install the latest PowerShell for new features: not required, this is Rofwin :)")
            }
            cmd == "exit" -> {
                commandLogs.clear()
                commandLogs.add("(session cleared — type 'help' for commands)")
            }

            // ---- Help ----
            cmd == "help" || cmd == "?" || cmd == "/?" || cmd == "get-help" || cmd.startsWith("get-help ") -> {
                commandLogs.add("ROFWIN COMMAND REFERENCE ${if (psMode) "(PowerShell)" else "(cmd.exe)"}:")
                commandLogs.add("  help / ver / cls / exit        Session basics")
                commandLogs.add("  dir / cd <path> / tree         File system")
                commandLogs.add("  echo <text> / set              Environment")
                commandLogs.add("  ipconfig / netstat / ping <h>  Networking")
                commandLogs.add("  tasklist / taskkill / systeminfo   Processes & specs")
                commandLogs.add("  whoami / hostname / date / time    Identity")
                commandLogs.add("  start <app>   (explorer, chrome, mt5, metaeditor)")
                commandLogs.add("  mt5 / terminal64.exe           Launch MetaTrader 5")
                commandLogs.add("  metaeditor / mql5              Launch MQL5 Editor")
                commandLogs.add("  wine --version / winetricks    Wine engine")
                if (psMode) commandLogs.add("  Get-Process / Get-ChildItem / Get-Date / Get-Host (alias OK)")
                if (!psMode) commandLogs.add("  powershell                     Switch to PowerShell mode")
            }

            // ---- System ----
            cmd == "ver" -> commandLogs.add("Rofwin Windows [Version 10.0.19045.4046] (Wine 8.0.2 x86_64)")
            cmd == "cls" || cmd == "clear" -> commandLogs.clear()
            cmd == "echo" -> commandLogs.add("ECHO is on.")
            cmd.startsWith("echo ") -> commandLogs.add(input.substringAfter(' '))
            cmd == "set" -> {
                commandLogs.add("ProgramFiles=C:\\Program Files")
                commandLogs.add("SystemRoot=C:\\Windows")
                commandLogs.add("TEMP=C:\\users\\Administrator\\Temp")
                commandLogs.add("WINEDEBUG=-all")
                commandLogs.add("BOX64_DYNAREC=1")
            }
            cmd == "whoami" -> commandLogs.add("rofwin-virt-pc\\administrator")
            cmd == "hostname" -> commandLogs.add("ROFWIN-VIRT-PC")
            cmd == "date" || cmd == "date /t" || cmd == "get-date" ->
                commandLogs.add(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
            cmd == "time" || cmd == "time /t" ->
                commandLogs.add(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
            cmd == "get-host" -> {
                commandLogs.add("Name             : ConsoleHost   5.1.19045.4046")
                commandLogs.add("InstanceId       : 8f2c1d44-rofwin-sim-0001")
            }

            // ---- Filesystem (dinamis — membaca FS yang sama dengan File Explorer) ----
            cmd == "dir" || cmd == "ls" || cmd == "gci" || cmd == "get-childitem" -> {
                commandLogs.add(" Directory of $cwd")
                commandLogs.add("")
                commandLogs.add("19/07/2026  19:00    <DIR>          .")
                commandLogs.add("19/07/2026  19:00    <DIR>          ..")
                val items = simulatedFiles[cwd] ?: emptyList()
                var dirs = 2
                var files = 0
                items.forEach { f ->
                    if (f.isDirectory) {
                        dirs++
                        commandLogs.add("19/07/2026  19:00    <DIR>          ${f.name}")
                    } else {
                        files++
                        commandLogs.add("19/07/2026  19:00         ${f.size.padStart(8)}  ${f.name}")
                    }
                }
                commandLogs.add("               $files File(s)")
                commandLogs.add("               $dirs Dir(s)  18,446,744,073,709,551,616 bytes free")
            }
            cmd == "cd" || cmd == "chdir" || cmd == "pwd" || cmd == "get-location" -> commandLogs.add(cwd)
            cmd.startsWith("cd ") || cmd.startsWith("chdir ") || cmd.startsWith("set-location ") -> {
                val t = arg.replace("/", "\\").trim()
                var cand = when {
                    t == ".." -> parentPath(cwd)
                    t == "\\" -> cwd.substringBefore('\\') + "\\"
                    t.length >= 2 && t[1] == ':' -> if (t.length == 2) "$t\\" else t
                    else -> cwd.trimEnd('\\') + "\\" + t
                }
                if (simulatedFiles.containsKey(cand)) {
                    cwd = cand
                } else if (simulatedFiles.containsKey(cand + "\\")) {
                    cwd = cand + "\\"
                } else {
                    commandLogs.add("The system cannot find the path specified.")
                }
            }
            cmd == "tree" -> {
                commandLogs.add("C:\\")
                commandLogs.add("+---Windows")
                commandLogs.add("|   \\---System32")
                commandLogs.add("+---Program Files")
                commandLogs.add("|   +---DirectX")
                commandLogs.add("|   \\---WineD3D")
                commandLogs.add("\\---users")
                commandLogs.add("    \\---Administrator")
            }

            // ---- File ops NYATA — sinkron 100% dengan File Explorer ----
            cmd.startsWith("mkdir ") || cmd.startsWith("md ") -> {
                val name = arg.replace("/", "\\").trim()
                if (name.isBlank()) {
                    commandLogs.add("The syntax of the command is incorrect.")
                } else {
                    val newPath = cwd.trimEnd('\\') + "\\" + name
                    if (simulatedFiles.containsKey(newPath)) {
                        commandLogs.add("A subdirectory or file $name already exists.")
                    } else {
                        val list = simulatedFiles[cwd]?.toMutableList() ?: mutableListOf()
                        list.add(SimFile(name, true, "Folder"))
                        simulatedFiles[cwd] = list
                        simulatedFiles[newPath] = emptyList()
                        simulatedFiles[newPath + "\\"] = emptyList()
                        commandLogs.add("(folder '$name' created — visible in File Explorer too)")
                    }
                }
            }
            cmd.startsWith("del ") || cmd.startsWith("erase ") || cmd.startsWith("rm ") || cmd.startsWith("remove-item ") -> {
                val name = arg.trim()
                val list = simulatedFiles[cwd]?.toMutableList() ?: mutableListOf()
                val victim = list.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (victim == null) {
                    commandLogs.add("Could Not Find $cwd\\$name")
                } else {
                    list.remove(victim)
                    simulatedFiles[cwd] = list
                    commandLogs.add("(deleted '$name' — also removed from File Explorer)")
                }
            }
            cmd.startsWith("type ") || cmd.startsWith("cat ") || cmd.startsWith("get-content ") -> {
                val name = arg.trim()
                val f = simulatedFiles[cwd]?.firstOrNull { it.name.equals(name, ignoreCase = true) && !it.isDirectory }
                if (f == null) {
                    commandLogs.add("The system cannot find the file specified.")
                } else {
                    commandLogs.add(":: $name (${f.size}) — konten simulasi:")
                    commandLogs.add(":: Rofwin virtual file. Data tidak nyata.")
                }
            }

            // ---- Networking ----
            cmd == "ipconfig" || cmd == "ipconfig /all" -> {
                commandLogs.add("Windows IP Configuration")
                commandLogs.add("")
                commandLogs.add("Ethernet adapter Wine0:")
                commandLogs.add("   IPv4 Address. . . . . . . . . . . : 10.0.2.15")
                commandLogs.add("   Subnet Mask . . . . . . . . . . . : 255.255.255.0")
                commandLogs.add("   Default Gateway . . . . . . . . . : 10.0.2.2")
                commandLogs.add("   DNS Servers . . . . . . . . . . . : 8.8.8.8")
            }
            cmd == "netstat" || cmd.startsWith("netstat ") -> {
                commandLogs.add("Active Connections")
                commandLogs.add("  Proto  Local Address      Foreign Address    State")
                commandLogs.add("  TCP    10.0.2.15:49152    10.0.2.2:53          ESTABLISHED")
                commandLogs.add("  TCP    10.0.2.15:50001    wine.roadfx:443      TIME_WAIT")
            }
            cmd.startsWith("ping") -> {
                val host = if (arg.isEmpty()) "8.8.8.8" else arg
                commandLogs.add("Pinging $host with 32 bytes of data:")
                commandLogs.add("Reply from $host: bytes=32 time=41ms TTL=117")
                commandLogs.add("Reply from $host: bytes=32 time=38ms TTL=117")
                commandLogs.add("Reply from $host: bytes=32 time=45ms TTL=117")
                commandLogs.add("Reply from $host: bytes=32 time=39ms TTL=117")
                commandLogs.add("Ping statistics: Packets: Sent = 4, Received = 4, Lost = 0 (0% loss)")
            }

            // ---- Processes ----
            cmd == "tasklist" || cmd == "ps" || cmd == "get-process" -> {
                commandLogs.add("Image Name                     PID   Mem Usage")
                commandLogs.add("=========================   =======   =========")
                commandLogs.add("explorer.exe                  1024     12,412 K")
                commandLogs.add("services.exe                   512      4,096 K")
                commandLogs.add("wineserver                     640     32,768 K")
                commandLogs.add("box64                          768     48,128 K")
                commandLogs.add("chrome.exe                    2048    156,672 K")
            }
            cmd.startsWith("taskkill") || cmd.startsWith("stop-process") ->
                commandLogs.add("SUCCESS: The process has been terminated.")
            cmd == "systeminfo" || cmd == "msinfo32" -> {
                commandLogs.add("Host Name        : ROFWIN-VIRT-PC")
                commandLogs.add("OS Name          : Rofwin Windows 10 Pro (Wine 8.0.2)")
                commandLogs.add("Host Device      : Oppo CPH1823 (Oppo F9)")
                commandLogs.add("Processor        : MediaTek Helio P60 MT6771, 8 Core(s) @2.00GHz")
                commandLogs.add("GPU              : ARM Mali-G72 MP3 (VirGL)")
                commandLogs.add("Total Memory     : 4,096 MB LPDDR4X")
                commandLogs.add("Active Preset    : ${container.box64Preset} mode")
            }

            // ---- Launch other windows (seperti 'start' asli) ----
            cmd == "start" || cmd == "start explorer" || cmd == "explorer" || cmd == "explorer.exe" || cmd == "invoke-item ." -> {
                commandLogs.add("Opening File Explorer...")
                onLaunch(DesktopWindow.MY_COMPUTER)
            }
            cmd.startsWith("start chrome") || cmd == "chrome" || cmd == "msedge" || cmd == "iexplore" || cmd.startsWith("start msedge") -> {
                commandLogs.add("Starting Chrome...")
                onLaunch(DesktopWindow.BROWSER)
            }
            cmd == "mt5" || cmd == "terminal64.exe" || cmd == "start terminal64.exe" -> {
                commandLogs.add("Loading MetaTrader 5 terminal...")
                onLaunch(DesktopWindow.MT5)
            }
            cmd == "metaeditor" || cmd == "metaeditor64.exe" || cmd == "mql5" || cmd == "start metaeditor64.exe" -> {
                commandLogs.add("Loading MetaEditor (MQL5)...")
                onLaunch(DesktopWindow.MQL5_EDITOR)
            }
            cmd.startsWith("start ") -> commandLogs.add("Starting '${input.substringAfter("start ")}' (simulated).")

            // ---- Wine extras ----
            cmd == "wine --version" -> commandLogs.add("wine-8.0.2 (Rofwin Dynamic Build x86_64)")
            cmd == "winetricks" || cmd.startsWith("winetricks ") -> {
                commandLogs.add("Winetricks loader:")
                commandLogs.add("Installing corefonts, d3dx9, d3dcompiler_47... SUCCESS.")
            }

            else -> unknown()
        }
        commandLogs.add("")
        onInputChange("")
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Session chips: ketuk untuk pindah cmd <-> PowerShell (ramah layar sentuh)
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !psMode,
                onClick = { psMode = false },
                label = { Text("cmd.exe", fontSize = 10.sp) }
            )
            FilterChip(
                selected = psMode,
                onClick = { psMode = true },
                label = { Text("powershell.exe", fontSize = 10.sp) }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "ketik 'help'",
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            items(commandLogs) { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = fgColor,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(4.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(prompt, color = fgColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            Spacer(modifier = Modifier.width(4.dp))
            TextField(
                value = terminalInput,
                onValueChange = onInputChange,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = bgColor,
                    unfocusedContainerColor = bgColor,
                    focusedTextColor = fgColor,
                    unfocusedTextColor = fgColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { exec(terminalInput) },
                    onGo = { exec(terminalInput) },
                    onDone = { exec(terminalInput) }
                )
            )
            IconButton(onClick = { exec(terminalInput) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Execute Command", tint = fgColor)
            }
        }
    }
}

@Composable
fun WinRarWindow(simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>) {
    // Extract NYATA — menulis hasil ekstraksi ke FS bersama (terlihat di File Explorer!)
    var extractProgress by remember { mutableFloatStateOf(0f) }
    var statusMsg by remember { mutableStateOf("Ready. Archive: backup.rar (15 MB)") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.Gray.copy(alpha = 0.2f)).padding(4.dp)) {
            listOf("File", "Commands", "Tools", "Favorites", "Options", "Help").forEach {
                Text(it, fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Win11Accent)
            IconButton(onClick = {
                // EXTRACT TO — D:\Work\extracted\
                scope.launch {
                    statusMsg = "Extracting to D:\\Work\\extracted..."
                    extractProgress = 0f
                    while (extractProgress < 1f) {
                        delay(120)
                        extractProgress += 0.1f
                    }
                    simulatedFiles["D:\\Work\\extracted"] = listOf(
                        SimFile("database_dump.sql", false, "2 MB"),
                        SimFile("config.json", false, "4 KB"),
                        SimFile("logs", true, "Folder"),
                        SimFile("src_backup", true, "Folder")
                    )
                    simulatedFiles["D:\\Work\\extracted\\logs"] = listOf(SimFile("app.log", false, "120 KB"))
                    simulatedFiles["D:\\Work\\extracted\\src_backup"] = listOf(SimFile("main.py", false, "8 KB"))
                    if (simulatedFiles["D:\\Work"]?.none { it.name == "extracted" } == true) {
                        val list = simulatedFiles["D:\\Work"]!!.toMutableList()
                        list.add(SimFile("extracted", true, "Folder"))
                        simulatedFiles["D:\\Work"] = list
                    }
                    statusMsg = "✔ Done — buka D:\\Work\\extracted di File Explorer"
                    extractProgress = 0f
                }
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Upload, contentDescription = "Extract", tint = SecondaryTeal)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = "Test integrity", tint = Color.Green)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(statusMsg, fontSize = 10.sp, color = if (statusMsg.startsWith("✔")) Color.Green else Color(0xFFB0B0B0))
        if (extractProgress > 0f) {
            LinearProgressIndicator(
                progress = extractProgress,
                color = Win11Accent,
                trackColor = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.Gray).background(DarkSurface).padding(8.dp)) {
            Column {
                Text("Archive: backup.rar", color = Color.White, fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                listOf("database_dump.sql" to "2 MB", "config.json" to "4 KB", "logs/" to "", "src_backup/" to "").forEach { (n, sz) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (n.endsWith("/")) Icons.Default.Folder else Icons.Default.Description, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(n, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(sz, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PythonShellWindow() {
    // Python mini-interpreter NYATA: aritmetika penuh (+ - * / ( ) desimal) + print()
    val logs = remember { mutableStateListOf("Python 3.12.1 (tags/v3.12.1:2305ca5, Dec  7 2023, 22:03:25) [MSC v.1937 64 bit (AMD64)] on win32", "Rofwin mini interpreter — coba: 72/5+3*(9-4), 2**10, print('halo')") }
    var input by remember { mutableStateOf("") }

    fun run(src: String) {
        val t = src.trim()
        if (t.isEmpty()) return
        logs.add(">>> $t")
        // power ** -> eval sebagai loop perkalian
        fun evalExprWithPow(e: String): Double? {
            var expr = e.replace(" ", "")
            val powIdx = expr.indexOf("**")
            if (powIdx > 0) {
                // ambil operand kiri & kanan sederhana
                var l = powIdx - 1
                while (l >= 0 && (expr[l].isDigit() || expr[l] == '.')) l--
                var r = powIdx + 2
                while (r < expr.length && (expr[r].isDigit() || expr[r] == '.')) r++
                val base = expr.substring(l + 1, powIdx).toDoubleOrNull()
                val ex = expr.substring(powIdx + 2, r).toDoubleOrNull()
                if (base != null && ex != null) {
                    val res = Math.pow(base, ex)
                    expr = expr.substring(0, l + 1) + res.toString() + expr.substring(r)
                }
            }
            return evalPy(expr)
        }
        when {
            t == "help" -> logs.add("Aritmetika: + - * / ( ) desimal, ** untuk pangkat. print('teks') / print(ekspresi)")
            t.startsWith("print(") && t.endsWith(")") -> {
                val inner = t.removePrefix("print(").dropLast(1).trim()
                val quoted = Regex("^\"(.*)\"$|^'(.*)'$").find(inner)
                if (quoted != null) {
                    logs.add(quoted.groupValues[1].ifEmpty { quoted.groupValues[2] })
                } else {
                    val v = evalExprWithPow(inner)
                    if (v != null) logs.add(if (v % 1.0 == 0.0) v.toLong().toString() else v.toString())
                    else logs.add("SyntaxError: cannot evaluate '$inner'")
                }
            }
            else -> {
                val v = evalExprWithPow(t)
                if (v != null) logs.add(if (v % 1.0 == 0.0) v.toLong().toString() else v.toString())
                else logs.add("NameError: name '$t' is not defined")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { Text(it, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(">>> ", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    run(input)
                    input = ""
                })
            )
        }
    }
}

@Composable
fun SshManagerWindow() {
    var host by remember { mutableStateOf("161.118.253.28") }
    var user by remember { mutableStateOf("ubuntu") }
    var connected by remember { mutableStateOf(false) }
    // Shell SSH interaktif setelah connect
    val logs = remember { mutableStateListOf<String>() }
    var cmdInput by remember { mutableStateOf("") }

    fun runRemote(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        logs.add("$user@$host:~$ $t")
        when (t) {
            "ls" -> logs.add("webvirtcloud  docker  data  backups")
            "ls -la" -> {
                logs.add("drwxr-xr-x 4 $user $user 4096 .")
                logs.add("drwxr-xr-x 3 root  root  4096 ..")
                logs.add("-rw-r--r-- 1 $user $user  220 .bashrc")
            }
            "whoami" -> logs.add(user)
            "pwd" -> logs.add("/home/$user")
            "uname -a" -> logs.add("Linux oracle-vm 6.8.0 #1-Ubuntu SMP x86_64 GNU/Linux")
            "exit" -> {
                logs.add("logout")
                logs.add("Connection to $host closed.")
                connected = false
            }
            "clear" -> logs.clear()
            else -> logs.add("$t: command not found (remote shell sim)")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!connected) {
            Text("SSH Connection Setup", color = Win11Accent, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                logs.clear()
                logs.add("Connecting to $host...")
                logs.add("Welcome to Ubuntu 24.04 LTS (GNU/Linux 6.8.0 x86_64)")
                logs.add("(remote shell simulasi — ls, whoami, pwd, uname -a, exit)")
                connected = true
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Connect Automatically")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.Green)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connected to $host as $user", color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(logs) { Text(it, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$user@$host:~$ ", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    BasicTextField(
                        value = cmdInput,
                        onValueChange = { cmdInput = it },
                        textStyle = TextStyle(color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(Color.Green),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            runRemote(cmdInput)
                            cmdInput = ""
                        })
                    )
                }
            }
        }
    }
}
@Composable
fun Mt5Window(
    lowRam: Boolean,
    mids: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Double>,
    prevMids: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Double>,
    candles: androidx.compose.runtime.snapshots.SnapshotStateList<Mt5Candle>,
    positions: androidx.compose.runtime.snapshots.SnapshotStateList<Mt5Pos>,
    tradeHistory: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    journalLogs: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    activeEAs: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    balance: Float,
    onBalance: (Float) -> Unit,
    ticket: Long,
    onTicket: (Long) -> Unit,
    expertAdvisors: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    account: String,
    onAccount: (String) -> Unit
) {
    val rnd = remember { java.util.Random() }
    var chartSymbol by remember { mutableStateOf("EURUSD") }
    var timeframe by remember { mutableStateOf("H1") }
    var pingMs by remember { mutableStateOf(41) }
    var toolboxTab by remember { mutableStateOf("Trade") }
    var maOn by remember { mutableStateOf(true) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var attachFor by remember { mutableStateOf<String?>(null) }
    // New Order (F9) form state
    var orderSymIdx by remember { mutableStateOf(0) }
    var orderLotsIdx by remember { mutableStateOf(0) }
    var orderSlPips by remember { mutableStateOf("25") }
    var orderTpPips by remember { mutableStateOf("50") }
    val lotOptions = listOf(0.01, 0.05, 0.10, 0.50)

    // heartbeat ping lokal
    LaunchedEffect(lowRam) {
        while (true) { delay(if (lowRam) 3000L else 1500L); pingMs = 30 + rnd.nextInt(60) }
    }

    fun pnlOf(p: Mt5Pos): Double {
        val mid = mids[p.sym] ?: p.open
        val delta = if (p.buy) (mid - p.open) else (p.open - mid)
        return delta * p.lots * mt5PnlFactor(p.sym)
    }
    fun closePos(p: Mt5Pos, reason: String) {
        val pnl = pnlOf(p)
        positions.remove(p)
        onBalance(balance + pnl.toFloat())
        val side = if (p.buy) "buy" else "sell"
        tradeHistory.add(0, "#${p.ticket} $side ${p.lots} ${p.sym} @ ${mt5Fmt(p.sym, p.open)} → P/L ${"%.2f".format(pnl)} USD ($reason)")
        journalLogs.add(0, "close #${p.ticket} $side ${p.sym} profit ${"%.2f".format(pnl)} [$reason]")
    }
    fun openPos(buy: Boolean, sym: String, lots: Double, reason: String, slPips: Int = 0, tpPips: Int = 0) {
        val mid = mids[sym] ?: return
        val px = if (buy) mid + mt5SpreadOf(sym) else mid
        val pip = if (sym.endsWith("JPY")) 0.01 else 0.0001
        val sl = if (slPips > 0) (if (buy) px - slPips * pip else px + slPips * pip) else 0.0
        val tp = if (tpPips > 0) (if (buy) px + tpPips * pip else px - tpPips * pip) else 0.0
        positions.add(Mt5Pos(ticket, buy, sym, lots, px, sl, tp))
        journalLogs.add(0, "open #$ticket ${if (buy) "buy" else "sell"} $lots $sym @ ${mt5Fmt(sym, px)}" + (if (sl > 0) " sl ${mt5Fmt(sym, sl)}" else "") + (if (tp > 0) " tp ${mt5Fmt(sym, tp)}" else "") + " [$reason]")
        onTicket(ticket + 1)
    }

    // ===== v1.7.0 — Login akun MT5/MQL5 (sim lokal + WebTerminal RESMI nyata via browser) =====
    val context = LocalContext.current
    var loginOpen by remember { mutableStateOf(false) }
    var loginId by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }
    var loginServer by remember { mutableStateOf("Rofwin-Demo") }

    val floating: Float = positions.sumOf { pnlOf(it) }.toFloat()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C))) {
        // Menu bar
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            listOf("File", "View", "Insert", "Charts", "Tools", "Window", "Help").forEach {
                Text(it, color = Color(0xFFBBBBBB), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        // Toolbar: timeframe + F9 + indikator + status
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF242424)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("M1", "M5", "M15", "H1", "H4", "D1").forEach { tf ->
                Text(
                    tf,
                    color = if (tf == timeframe) Color(0xFF4CAF50) else Color(0xFF999999),
                    fontSize = 10.sp,
                    fontWeight = if (tf == timeframe) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { timeframe = tf }.padding(horizontal = 5.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "F9",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFF1E5AA8), RoundedCornerShape(3.dp))
                    .clickable { orderSymIdx = mids.keys.indexOf(chartSymbol).coerceAtLeast(0); showOrderDialog = true }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "MA",
                color = if (maOn) Color(0xFF4CC2FF) else Color(0xFF777777),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { maOn = !maOn }.padding(horizontal = 4.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Login",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFF2E7D32), RoundedCornerShape(3.dp))
                    .clickable { loginOpen = true }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (account.isBlank()) "Demo 12345678" else account, color = Color(0xFF88CC88), fontSize = 9.sp)
        }

        Row(modifier = Modifier.weight(1f)) {
            // ===== Kolom kiri: Market Watch + Navigator =====
            Column(modifier = Modifier.width(132.dp).fillMaxHeight().border(1.dp, Color(0xFF333333))) {
                Column(modifier = Modifier.weight(1f).padding(5.dp)) {
                    Text("Market Watch", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Symbol", color = Color.Gray, fontSize = 8.sp)
                        Text("Bid / Ask", color = Color.Gray, fontSize = 8.sp)
                    }
                    LazyColumn {
                        items(mids.keys.toList()) { s ->
                            val mid = mids[s] ?: 1.0
                            val up = mid >= (prevMids[s] ?: mid)
                            val c = if (up) Color(0xFF4CAF50) else Color(0xFFEF5350)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chartSymbol = s }
                                    .background(if (s == chartSymbol) Color(0xFF173A17) else Color.Transparent)
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(s, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${mt5Fmt(s, mid)}   ${mt5Fmt(s, mid + mt5SpreadOf(s))}", color = c, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                // ===== Navigator =====
                Divider(color = Color(0xFF444444))
                Column(modifier = Modifier.height(112.dp).padding(5.dp)) {
                    Text("Navigator", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("▸ Accounts — 12345678 (Demo, 1:500)", color = Color(0xFFAAAAAA), fontSize = 8.sp)
                    Text("▸ Indicators — MA7/MA21 " + if (maOn) "ON" else "OFF", color = Color(0xFFAAAAAA), fontSize = 8.sp)
                    Text("▸ Expert Advisors (${expertAdvisors.size})", color = Color(0xFFAAAAAA), fontSize = 8.sp)
                    LazyColumn {
                        if (expertAdvisors.isEmpty()) {
                            item { Text("   (compile EA di Rofwin Code)", color = Color(0xFF666666), fontSize = 8.sp) }
                        }
                        items(expertAdvisors) { ea ->
                            val attached = activeEAs.containsKey(ea)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = if (attached) Color(0xFF4CAF50) else Color(0xFF999999), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(ea, color = Color.White, fontSize = 8.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(
                                    if (attached) "Detach" else "Attach",
                                    color = if (attached) Color(0xFFEF5350) else Color(0xFF4CAF50),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        if (attached) {
                                            activeEAs.remove(ea)
                                            journalLogs.add(0, "EA $ea detached")
                                        } else {
                                            attachFor = ea
                                        }
                                    }.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ===== Chart Area: candlestick + MA overlay =====
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black)) {
                Canvas(modifier = Modifier.fillMaxSize().padding(top = 30.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)) {
                    val w = size.width; val h = size.height
                    for (i in 1..4) drawLine(Color(0xFF1B1B1B), Offset(0f, h * i / 5f), Offset(w, h * i / 5f), strokeWidth = 1f)
                    if (candles.size > 1) {
                        val minP = candles.minOf { it.l }
                        val maxP = candles.maxOf { it.h }
                        val span = (maxP - minP).coerceAtLeast(1f)
                        fun y(p: Float) = h - ((p - minP) / span) * h
                        val step = w / candles.size
                        candles.forEachIndexed { i, cd ->
                            val cx = i * step + step / 2f
                            val up = cd.c >= cd.o
                            val col = if (up) Color(0xFF26A69A) else Color(0xFFEF5350)
                            drawLine(col, Offset(cx, y(cd.l)), Offset(cx, y(cd.h)), strokeWidth = 2f)
                            val top = y(maxOf(cd.o, cd.c))
                            val bot = y(minOf(cd.o, cd.c))
                            drawRect(col, topLeft = Offset(cx - step * 0.3f, top), size = androidx.compose.ui.geometry.Size(step * 0.6f, (bot - top).coerceAtLeast(2f)))
                        }
                        if (maOn) {
                            fun ma(n: Int, end: Int): Float? {
                                if (end < n) return null
                                var s = 0f
                                for (k in end - n until end) s += candles[k].c
                                return s / n
                            }
                            for (i in 7 until candles.size) {
                                val m7a = ma(7, i); val m7b = ma(7, i + 1)
                                if (m7a != null && m7b != null) drawLine(Color(0xFF4CC2FF), Offset((i - 1) * step + step / 2f, y(m7a)), Offset(i * step + step / 2f, y(m7b)), strokeWidth = 2f)
                                if (i >= 21) {
                                    val m21a = ma(21, i); val m21b = ma(21, i + 1)
                                    if (m21a != null && m21b != null) drawLine(Color(0xFFFFC107), Offset((i - 1) * step + step / 2f, y(m21a)), Offset(i * step + step / 2f, y(m21b)), strokeWidth = 2f)
                                }
                            }
                        }
                    }
                }
                Text("$chartSymbol, $timeframe" + if (maOn) "  MA7/21" else "", color = Color.DarkGray, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                // Sell / Buy one-click
                Row(modifier = Modifier.align(Alignment.TopStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { openPos(false, chartSymbol, 0.01, "manual") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2424)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) { Text("Sell ${mt5Fmt(chartSymbol, mids[chartSymbol] ?: 1.0)}", fontSize = 9.sp) }
                    Button(
                        onClick = { openPos(true, chartSymbol, 0.01, "manual") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5AA8)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) { Text("Buy ${mt5Fmt(chartSymbol, (mids[chartSymbol] ?: 1.0) + mt5SpreadOf(chartSymbol))}", fontSize = 9.sp) }
                }
                Text(
                    "Bid ${mt5Fmt(chartSymbol, mids[chartSymbol] ?: 1.0)}",
                    color = Color(0xFF4CAF50), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                )
            }
        }

        // ===== Toolbox: Trade | History | News | Journal =====
        Column(modifier = Modifier.fillMaxWidth().height(126.dp).border(1.dp, Color(0xFF333333)).padding(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf("Trade", "History", "News", "Journal").forEach { t ->
                    Text(
                        t,
                        color = if (t == toolboxTab) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = if (t == toolboxTab) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { toolboxTab = t }.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (toolboxTab == "Trade" && positions.isNotEmpty()) {
                    Text("Close All", color = Color(0xFFEF5350), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { positions.toList().forEach { closePos(it, "close all") } }.padding(horizontal = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                when {
                    toolboxTab == "Trade" && positions.isEmpty() ->
                        item { Text("no open positions — F9 order, Sell/Buy satu klik, atau attach EA", color = Color.Gray, fontSize = 9.sp) }
                    toolboxTab == "Trade" -> {
                        item {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Ticket/Side", color = Color.Gray, fontSize = 8.sp, modifier = Modifier.weight(1.3f))
                                Text("Lots", color = Color.Gray, fontSize = 8.sp, modifier = Modifier.weight(0.5f))
                                Text("Open", color = Color.Gray, fontSize = 8.sp, modifier = Modifier.weight(0.9f))
                                Text("P/L $", color = Color.Gray, fontSize = 8.sp, modifier = Modifier.weight(0.8f))
                                Text("", modifier = Modifier.width(16.dp))
                            }
                        }
                        items(positions) { p ->
                            val pnl = pnlOf(p)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("#${p.ticket} ${if (p.buy) "buy" else "sell"} ${p.sym}" + (if (p.sl > 0 || p.tp > 0) " (sl/tp)" else ""), color = if (p.buy) Color(0xFF66BB6A) else Color(0xFFEF5350), fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.3f), maxLines = 1)
                                Text("${p.lots}", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.5f))
                                Text(mt5Fmt(p.sym, p.open), color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.9f))
                                Text("${"%.2f".format(pnl)}", color = if (pnl >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.8f))
                                Text("✕", color = Color.Gray, fontSize = 8.sp, modifier = Modifier.width(16.dp).clickable { closePos(p, "manual close") })
                            }
                        }
                    }
                    toolboxTab == "History" && tradeHistory.isEmpty() ->
                        item { Text("belum ada closed trade", color = Color.Gray, fontSize = 10.sp) }
                    toolboxTab == "History" ->
                        items(tradeHistory) { hTxt -> Text(hTxt, color = Color(0xFFD0D0D0), fontSize = 8.sp, fontFamily = FontFamily.Monospace) }
                    toolboxTab == "News" -> {
                        items(listOf(
                            "04:00  USD  Fed holds rates; DXY flat",
                            "03:15  XAU  Gold tests 2385 resistance",
                            "01:40  BTC  ETF inflows +$210M hari ini",
                            "23:05  JPY  BoJ: kebijakan tetap longgar"
                        )) { n -> Text(n, color = Color(0xFFB0B0B0), fontSize = 8.sp) }
                    }
                    else -> items(journalLogs) { j ->
                        Text(j, color = if (detectCritical(j)) Color(0xFFFF5252) else Color(0xFF9CCC9C), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = if (detectCritical(j)) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            val marginUsed = positions.sumOf { it.lots * 130 }.toFloat()
            Text(
                "Bal: ${"%.2f".format(balance)}  Eq: ${"%.2f".format(balance + floating)}  Mgn: ${"%.2f".format(marginUsed)}  Free: ${"%.2f".format(balance + floating - marginUsed)}" + (if (marginUsed > 0f) "  Lvl: ${"%.0f".format((balance + floating) / marginUsed * 100)}%" else ""),
                color = if (floating >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Status bar
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Circle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(8.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Connected | $pingMs ms | ${activeEAs.size} EA aktif | bot jalan walau jendela ditutup", color = Color(0xFF999999), fontSize = 8.sp)
        }
    }

    // ===== v1.7.0 — Dialog Login akun MT5/MQL5 =====
    if (loginOpen) {
        AlertDialog(
            onDismissRequest = { loginOpen = false },
            title = { Text("Login — MetaTrader 5", fontSize = 14.sp) },
            text = {
                Column {
                    OutlinedTextField(value = loginId, onValueChange = { loginId = it }, label = { Text("Login (no. akun)", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = loginPass, onValueChange = { loginPass = it }, label = { Text("Password", fontSize = 10.sp) }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = loginServer, onValueChange = { loginServer = it }, label = { Text("Server broker", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Akun asli (uang nyata / live trading) hanya bisa via WebTerminal resmi broker — tombol hijau membuka browser.", color = Color(0xFFFFB74D), fontSize = 8.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = loginId.trim().ifBlank { "12345678" }
                    onAccount("$id@$loginServer")
                    journalLogs.add(0, "login $id $loginServer — sesi akun aktif (sim)")
                    loginPass = ""
                    loginOpen = false
                }) { Text("Hubungkan (Sim)", fontSize = 11.sp) }
            },
            dismissButton = {
                TextButton(onClick = {
                    loginOpen = false
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://app.metatrader.app")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                }) { Text("🌐 WebTerminal REAL", fontSize = 11.sp, color = Color(0xFF4CAF50)) }
            }
        )
    }

    // ===== Dialog New Order (F9) — symbol, lot, SL/TP dalam pip =====
    if (showOrderDialog) {
        val syms = mids.keys.toList()
        val sym = syms.getOrElse(orderSymIdx) { "EURUSD" }
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text("New Order (F9)", fontSize = 14.sp) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Symbol:", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(56.dp))
                        Text("‹", color = Win11Accent, fontSize = 18.sp, modifier = Modifier.clickable { orderSymIdx = (orderSymIdx - 1 + syms.size) % syms.size }.padding(6.dp))
                        Text(sym, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("›", color = Win11Accent, fontSize = 18.sp, modifier = Modifier.clickable { orderSymIdx = (orderSymIdx + 1) % syms.size }.padding(6.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Volume:", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(56.dp))
                        lotOptions.forEachIndexed { i, l ->
                            Text(
                                "$l",
                                color = if (i == orderLotsIdx) Color.Black else Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .background(if (i == orderLotsIdx) Win11Accent else Color(0xFF3A3A3A), RoundedCornerShape(3.dp))
                                    .clickable { orderLotsIdx = i }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SL (pip):", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(72.dp))
                        OutlinedTextField(value = orderSlPips, onValueChange = { orderSlPips = it.filter { c -> c.isDigit() } }, singleLine = true, modifier = Modifier.width(80.dp).height(44.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TP:", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(value = orderTpPips, onValueChange = { orderTpPips = it.filter { c -> c.isDigit() } }, singleLine = true, modifier = Modifier.width(80.dp).height(44.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                    Text("(0 = tanpa SL/TP; auto-close dicek tiap tick)", color = TextSecondary, fontSize = 9.sp)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        openPos(false, sym, lotOptions[orderLotsIdx], "F9 order", orderSlPips.toIntOrNull() ?: 0, orderTpPips.toIntOrNull() ?: 0)
                        showOrderDialog = false
                    }) { Text("Sell by Market", color = Color(0xFFEF5350)) }
                    TextButton(onClick = {
                        openPos(true, sym, lotOptions[orderLotsIdx], "F9 order", orderSlPips.toIntOrNull() ?: 0, orderTpPips.toIntOrNull() ?: 0)
                        showOrderDialog = false
                    }) { Text("Buy by Market", color = Color(0xFF66BB6A)) }
                }
            },
            dismissButton = { TextButton(onClick = { showOrderDialog = false }) { Text("Batal") } }
        )
    }

    // ===== Dialog Attach EA — pilih simbol chart target =====
    attachFor?.let { ea ->
        val syms = mids.keys.toList()
        var eaSymIdx by remember(ea) { mutableStateOf(syms.indexOf(chartSymbol).coerceAtLeast(0)) }
        AlertDialog(
            onDismissRequest = { attachFor = null },
            title = { Text("Attach EA: $ea", fontSize = 14.sp) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chart:", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(50.dp))
                    Text("‹", color = Win11Accent, fontSize = 18.sp, modifier = Modifier.clickable { eaSymIdx = (eaSymIdx - 1 + syms.size) % syms.size }.padding(6.dp))
                    Text(syms.getOrElse(eaSymIdx) { "EURUSD" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("›", color = Win11Accent, fontSize = 18.sp, modifier = Modifier.clickable { eaSymIdx = (eaSymIdx + 1) % syms.size }.padding(6.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = syms.getOrElse(eaSymIdx) { "EURUSD" }
                    activeEAs[ea] = target
                    journalLogs.add(0, "EA $ea attached to $target, $timeframe — auto-trade ON (lots 0.01)")
                    attachFor = null
                }) { Text("OK — jalankan bot") }
            },
            dismissButton = { TextButton(onClick = { attachFor = null }) { Text("Batal") } }
        )
    }
}
@Composable
fun Mql5EditorWindow() {
    var code by remember {
        mutableStateOf("//+------------------------------------------------------------------+\n//|                                                      Expert.mq5 |\n//|                                      Copyright 2026, MetaQuotes |\n//|                                             https://www.mql5.com |\n//+------------------------------------------------------------------+\n#property copyright \"Copyright 2026\"\n#property link      \"https://www.mql5.com\"\n#property version   \"1.00\"\n\n//+------------------------------------------------------------------+\n//| Expert initialization function                                   |\n//+------------------------------------------------------------------+\nint OnInit()\n  {\n   Print(\"Algo Editor Sync Activated!\");\n   return(INIT_SUCCEEDED);\n  }\n\n//+------------------------------------------------------------------+\n//| Expert tick function                                             |\n//+------------------------------------------------------------------+\nvoid OnTick()\n  {\n   // strategy here\n  }\n")
    }
    val outputLogs = remember { mutableStateListOf<String>() }
    var compiling by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("Algo Editor MQL5 Sync: Active & Connected") }
    val scope = rememberCoroutineScope()

    fun compile() {
        if (compiling) return
        compiling = true
        outputLogs.add("Compiling 'Expert.mq5'...")
        scope.launch {
            delay(900)
            // Pemeriksaan sederhana: kurung kurawal tak seimbang => error ala compiler
            val openB = code.count { it == '{' }
            val closeB = code.count { it == '}' }
            if (openB != closeB) {
                outputLogs.add("'{' - unbalanced parentheses   Expert.mq5   line ${code.lines().size}   (1 error, 0 warnings)")
            } else {
                val kb = (code.toByteArray().size / 3 + 1024)
                outputLogs.add("0 errors, 0 warnings, $kb bytes code generated")
                outputLogs.add("Expert.ex5 written to C:\\MQL5\\Experts\\")
            }
            compiling = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // Toolbar ala MetaEditor
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("MetaEditor — Expert.mq5", color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { compile() }, modifier = Modifier.height(26.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (compiling) "Compiling..." else "Compile (F7)", fontSize = 10.sp)
            }
        }
        // Editor
        TextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedTextColor = Color(0xFFD4D4D4),
                unfocusedTextColor = Color(0xFFD4D4D4),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        )
        // Output panel (Errors / Warnings) ala MetaEditor
        Column(modifier = Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF252526)).padding(6.dp)) {
            Text("Errors  |  Warnings  |  Find", color = Color.Gray, fontSize = 9.sp)
            LazyColumn {
                if (outputLogs.isEmpty()) {
                    item { Text("Tekan Compile (F7) untuk membangun EA...", color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                } else {
                    items(outputLogs) { line ->
                        Text(
                            line,
                            color = if (line.contains("error", ignoreCase = true) && !line.startsWith("0 errors")) Color(0xFFEF5350) else Color(0xFF4CAF50),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        // Status bar
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF007ACC)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(syncStatus, color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("Ln ${code.lines().size}", color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("UTF-8", color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MQL5", color = Color.White, fontSize = 10.sp)
        }
    }
}
@Composable
fun BrowserWindow() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var urlInput by remember { mutableStateOf(url) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(DarkSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigasi ala Chrome: kembali / maju / beranda
            IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Maju", tint = Color.White)
            }
            IconButton(
                onClick = {
                    url = "https://www.google.com"
                    urlInput = url
                    webView?.loadUrl(url)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Beranda", tint = Color.White)
            }
            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant),
                textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    var loadUrl = urlInput.trim()
                    if (!loadUrl.startsWith("http://") && !loadUrl.startsWith("https://")) {
                        loadUrl = if (loadUrl.contains(".")) "https://$loadUrl" else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(loadUrl, "UTF-8")
                    }
                    url = loadUrl
                    urlInput = loadUrl
                    webView?.loadUrl(url)
                }),
                singleLine = true
            )
            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang", tint = Color.White)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
            AndroidView(
                factory = { ctx ->
                    // v1.8.2 — anti-FC: di ROM tertentu provider WebView bisa rusak/hilang;
                    // tampilkan pesan fallback alih-alih force close.
                    try {
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // hardening v1.8.2: konten lokal tidak diperlukan — semua halaman via HTTPS
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            loadUrl(url)
                            webView = this
                        }
                    } catch (t: Throwable) {
                        webView = null
                        android.widget.TextView(ctx).apply {
                            text = "⚠ WebView tidak tersedia/rusak di device ini.\n${t.message}"
                            setTextColor(android.graphics.Color.DKGRAY)
                            setPadding(28, 28, 28, 28)
                        }
                    }
                },
                update = {
                    // Navigasi via ongmn action/keyboard/Go button
                }
            )
        }
    }
}

@Composable
fun GitBashWindow() {
    // Git Bash interaktif — perintah git umum hidup (mode simulasi lokal)
    val logs = remember { mutableStateListOf("MINGW64 Git Bash — ketik 'help' untuk daftar perintah") }
    var input by remember { mutableStateOf("") }

    fun run(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        logs.add("$ $t")
        when {
            t == "help" -> {
                logs.add("git status | git log | git branch | ls | pwd | clear")
            }
            t == "git status" -> {
                logs.add("On branch main")
                logs.add("nothing to commit, working tree clean")
            }
            t == "git log" || t == "git log --oneline" -> {
                logs.add("ab56873 (HEAD -> main, tag: v1.4.0) feat: Windows 11 Edition")
                logs.add("2becb4e build: gradle wrapper 9.3.1")
            }
            t == "git branch" -> logs.add("* main")
            t == "ls" -> logs.add("app/  README.md  build.gradle.kts  settings.gradle.kts")
            t == "pwd" -> logs.add("/c/rofwin")
            t == "clear" -> logs.clear()
            else -> logs.add("bash: $t: command not found")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(8.dp)) {
        Text("ivansslo@CPH1823 MINGW64 /c/rofwin", color = Color(0xFFADFF2F), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { Text(it, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    run(input)
                    input = ""
                })
            )
        }
    }
}

@Composable
fun AiRouteWindow() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI ROC-AgentsRoute v1.0", style = MaterialTheme.typography.titleMedium.copy(color = SecondaryTeal))
        Text("Powered by Gemini Agentic Engine", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Current Route Status:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                Text("Route A-102 (Optimized for MediaTek P60)", color = PrimarySky, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Latency Reduction: 24ms", color = Color.Green, fontSize = 11.sp)
                Text("Packet Steering: Active", color = Color.Green, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Recalculate */ }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recalculate AI Path")
        }
    }
}

// =====================================================================
// Rofwin Code — editor coding: multi-tab, autosave, line numbers,
// Find & Replace, Run (.py), Compile (.mq5 → EA masuk MT5 Navigator)
// =====================================================================
@Composable
fun CodeEditorWindow(
    fileContents: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>,
    expertAdvisors: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    eaActive: Int = 0,
    initialPath: String?
) {
    val openTabs = remember { mutableStateListOf<String>() }
    var activeTab by remember { mutableStateOf<String?>(null) }
    var untitledN by remember { mutableStateOf(1) }
    val outputLogs = remember { mutableStateListOf("Rofwin Code siap — Open / New file, 💾 autosave") }
    var findOpen by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var openDialog by remember { mutableStateOf(false) }
    var saveAsDialog by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val start = initialPath ?: "untitled-1"
        if (initialPath != null && !fileContents.containsKey(initialPath)) fileContents[initialPath] = DEFAULT_EXPERT_MQ5
        if (initialPath == null && !fileContents.containsKey("untitled-1")) fileContents["untitled-1"] = "// new file — gunakan Save As untuk menamai\n"
        openTabs.add(start)
        activeTab = start
        untitledN = 2
    }

    fun extOf(p: String) = p.substringAfterLast('.', "").lowercase()
    fun langOf(p: String) = when (extOf(p)) {
        "mq5", "mqh" -> "MQL5"; "py" -> "Python"; "ts", "tsx" -> "TypeScript"
        "json" -> "JSON"; "md" -> "Markdown"; "sh" -> "Shell"; "html" -> "HTML"
        "css" -> "CSS"; "js" -> "JavaScript"; else -> "Plain Text"
    }

    // Tulis entri FS (agar file terlihat di File Explorer & Terminal)
    fun ensureFsEntry(path: String): Boolean {
        val folder = path.substringBeforeLast('\\', "")
        val name = path.substringAfterLast('\\')
        if (folder.isEmpty() || name.isEmpty()) return false
        if (!simulatedFiles.containsKey(folder)) {
            val pf = folder.substringBeforeLast('\\', "")
            val dn = folder.substringAfterLast('\\')
            if (simulatedFiles.containsKey(pf)) {
                val pl = simulatedFiles[pf]!!.toMutableList()
                pl.add(SimFile(dn, true, "Folder"))
                simulatedFiles[pf] = pl
                simulatedFiles[folder] = emptyList()
            } else return false
        }
        val list = simulatedFiles[folder]!!.toMutableList()
        val sizeKb = ((fileContents[path]?.length ?: 0) / 1024 + 1)
        val idx = list.indexOfFirst { it.name == name }
        if (idx >= 0) list[idx] = SimFile(name, false, "$sizeKb KB") else list.add(SimFile(name, false, "$sizeKb KB"))
        simulatedFiles[folder] = list
        return true
    }

    fun openPath(p: String) {
        if (!fileContents.containsKey(p)) {
            fileContents[p] = "// $p\n// (kerangka — isi belum ada di simulasi)\n"
        }
        if (!openTabs.contains(p)) openTabs.add(p)
        activeTab = p
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // ===== v1.7.0 — status bot: EA tetap BEKERJA saat editor MQL5 dipakai (tick engine global) =====
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF14261A)).padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SmartToy, null, tint = if (eaActive > 0) Color(0xFF4CAF50) else Color(0xFF777777), modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                if (eaActive > 0) "$eaActive EA/bot sedang BEKERJA — tick engine global jalan walau editor ini sedang dipakai" else "tidak ada EA aktif — compile ⚙ lalu Attach di MT5 Navigator; bot bisa bekerja sambil Anda mengedit",
                color = if (eaActive > 0) Color(0xFF81C784) else Color(0xFF999999),
                fontSize = 8.sp
            )
        }
        // ===== Tab bar =====
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF252526)).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            openTabs.forEach { t ->
                val active = t == activeTab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(if (active) Color(0xFF1E1E1E) else Color(0xFF2D2D2D))
                        .clickable { activeTab = t }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(t.substringAfterLast('\\'), color = if (active) Color.White else Color(0xFFAAAAAA), fontSize = 10.sp, maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("✕", color = Color(0xFF888888), fontSize = 9.sp, modifier = Modifier.clickable {
                        openTabs.remove(t)
                        if (activeTab == t) activeTab = openTabs.lastOrNull()
                    })
                }
            }
            Icon(Icons.Default.Add, contentDescription = "New file", tint = Color(0xFFAAAAAA), modifier = Modifier
                .size(18.dp)
                .clickable {
                    val p = "untitled-${untitledN++}"
                    fileContents[p] = "// new file\n"
                    openTabs.add(p)
                    activeTab = p
                }
                .padding(4.dp))
        }

        // ===== Toolbar =====
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C)).padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { openDialog = true }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.FolderOpen, "Open", tint = Color(0xFFCCCCCC), modifier = Modifier.size(15.dp)) }
            IconButton(onClick = {
                activeTab?.let { t ->
                    if (!t.startsWith("untitled")) {
                        if (ensureFsEntry(t)) outputLogs.add(0, "💾 saved → $t") else outputLogs.add(0, "⚠ folder $t tidak ditemukan")
                    } else {
                        saveAsName = ""; saveAsDialog = true
                    }
                }
            }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Save, "Save", tint = Color(0xFFCCCCCC), modifier = Modifier.size(15.dp)) }
            IconButton(onClick = { saveAsName = ""; saveAsDialog = true }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.SaveAs, "Save As", tint = Color(0xFFCCCCCC), modifier = Modifier.size(15.dp)) }
            IconButton(onClick = { findOpen = !findOpen }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.FindInPage, "Find", tint = if (findOpen) Win11Accent else Color(0xFFCCCCCC), modifier = Modifier.size(15.dp)) }
            // Run untuk .py
            IconButton(
                onClick = {
                    activeTab?.let { t ->
                        if (extOf(t) == "py") {
                            outputLogs.add(0, "▶ Run ${t.substringAfterLast('\\')} …")
                            val outs = runMiniPython(fileContents[t] ?: "")
                            if (outs.isEmpty()) outputLogs.add(0, "  (no output)")
                            outs.asReversed().forEach { outputLogs.add(0, "  $it") }
                        } else outputLogs.add(0, "▶ Run hanya untuk .py (file ini: ${extOf(t)})")
                    }
                },
                modifier = Modifier.size(26.dp)
            ) { Icon(Icons.Default.PlayArrow, "Run", tint = Color(0xFF4CAF50), modifier = Modifier.size(15.dp)) }
            // Compile untuk .mq5 → EA masuk MT5 Navigator
            IconButton(
                onClick = {
                    activeTab?.let { t ->
                        if (extOf(t) == "mq5" || extOf(t) == "mqh") {
                            val eaName = t.substringAfterLast('\\').substringBeforeLast('.')
                            outputLogs.add(0, "⚙ Compiling '${t.substringAfterLast('\\')}'...")
                            val code = fileContents[t] ?: ""
                            scope.launch {
                                delay(700)
                                val ob = code.count { it == '{' }; val cb = code.count { it == '}' }
                                if (ob != cb) {
                                    outputLogs.add(0, "  '{' - unbalanced parentheses (1 error)")
                                } else {
                                    if (!expertAdvisors.contains(eaName)) {
                                        expertAdvisors.add(eaName)
                                        SimFileAdd(fsOwner = null, simulatedFiles, "C:\\MQL5\\Experts", "$eaName.mq5")
                                    }
                                    outputLogs.add(0, "  0 errors, 0 warnings — $eaName.ex5 build OK")
                                    outputLogs.add(0, "  ✔ terdaftar di MT5 Navigator → Attach untuk auto-trade")
                                }
                            }
                        } else outputLogs.add(0, "⚙ Compile hanya untuk .mq5/.mqh")
                    }
                },
                modifier = Modifier.size(26.dp)
            ) { Icon(Icons.Default.Build, "Compile", tint = Color(0xFFFFB74D), modifier = Modifier.size(15.dp)) }
            Spacer(modifier = Modifier.weight(1f))
            Text(activeTab?.let { langOf(it) } ?: "", color = Color(0xFF999999), fontSize = 9.sp)
        }

        // ===== Find & Replace bar =====
        if (findOpen) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF252526)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = findText, onValueChange = { findText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Win11Accent),
                    decorationBox = { inner -> Box(Modifier.background(Color(0xFF333333), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 4.dp)) { if (findText.isEmpty()) Text("find", color = Color(0xFF777777), fontSize = 11.sp); inner() } },
                    modifier = Modifier.width(80.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val matches = activeTab?.let { t -> if (findText.isEmpty()) 0 else (fileContents[t] ?: "").split(findText).size - 1 } ?: 0
                Text("$matches", color = Color(0xFF9CCC9C), fontSize = 10.sp)
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = replaceText, onValueChange = { replaceText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Win11Accent),
                    decorationBox = { inner -> Box(Modifier.background(Color(0xFF333333), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 4.dp)) { if (replaceText.isEmpty()) Text("replace", color = Color(0xFF777777), fontSize = 11.sp); inner() } },
                    modifier = Modifier.width(80.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Replace All", color = Win11Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                    activeTab?.let { t ->
                        if (findText.isNotEmpty()) {
                            val src = fileContents[t] ?: ""
                            val n = src.split(findText).size - 1
                            fileContents[t] = src.replace(findText, replaceText)
                            outputLogs.add(0, "replaced $n occurrence(s) of '$findText'")
                        }
                    }
                })
            }
        }

        // ===== Editor (line numbers + autosave ke fileContents) =====
        val tab = activeTab
        if (tab == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Tidak ada file terbuka — Open (📂) atau ＋ New", color = Color(0xFF777777), fontSize = 11.sp)
            }
        } else {
            val text = fileContents[tab] ?: ""
            val editorScroll = rememberScrollState()
            Row(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(editorScroll)) {
                // Line numbers
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(34.dp).background(Color(0xFF252526)).padding(top = 4.dp)
                ) {
                    val n = text.lines().size.coerceAtMost(4000)
                    for (i in 1..n) {
                        Text("$i", color = Color(0xFF858585), fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(end = 6.dp))
                    }
                }
                BasicTextField(
                    value = text,
                    onValueChange = { fileContents[tab] = it },
                    textStyle = TextStyle(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                    cursorBrush = SolidColor(Win11Accent),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp)
                )
            }
        }

        // ===== Output panel =====
        Column(modifier = Modifier.fillMaxWidth().height(84.dp).background(Color(0xFF252526)).border(1.dp, Color(0xFF333333)).padding(4.dp)) {
            Text("Output (Run / Compile / Find)", color = Color.Gray, fontSize = 8.sp)
            LazyColumn {
                items(outputLogs.take(80)) { line ->
                    Text(line, color = when {
                        line.contains("error", ignoreCase = true) && !line.startsWith("  0 errors") -> Color(0xFFEF5350)
                        line.startsWith("  ✔") || line.startsWith("💾") -> Color(0xFF4CAF50)
                        else -> Color(0xFFD4D4D4)
                    }, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ===== Status bar =====
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF007ACC)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            val t = activeTab
            val content = t?.let { fileContents[it] } ?: ""
            Text(
                if (t == null) "Ready" else "Ln ${content.lines().size} · ${content.length} chr · ${langOf(t)}" + if (t.startsWith("untitled")) " · (belum disimpan)" else " · saved↔FS",
                color = Color.White, fontSize = 9.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("UTF-8 · Rofwin Code", color = Color.White, fontSize = 9.sp)
        }
    }

    // ===== Dialog Open (daftar file kode dari seluruh FS) =====
    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text("Open File", fontSize = 14.sp) },
            text = {
                val codeExts = setOf("mq5", "mqh", "py", "txt", "md", "ts", "tsx", "json", "sh", "css", "html", "js")
                val candidates = simulatedFiles.keys.flatMap { k ->
                    (simulatedFiles[k] ?: emptyList())
                        .filter { !it.isDirectory && it.name.substringAfterLast('.', "").lowercase() in codeExts }
                        .map { k.trimEnd('\\') + "\\" + it.name }
                }.distinct().sorted()
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(candidates) { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { openPath(p); openDialog = false }.padding(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Description, null, tint = Win11Accent, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(p, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { openDialog = false }) { Text("Batal") } }
        )
    }

    // ===== Dialog Save As =====
    if (saveAsDialog) {
        AlertDialog(
            onDismissRequest = { saveAsDialog = false },
            title = { Text("Save As", fontSize = 14.sp) },
            text = {
                Column {
                    Text("Tulis nama file (relatif ke D:\\Work) atau path penuh (C:\\...)", fontSize = 10.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = saveAsName, onValueChange = { saveAsName = it }, singleLine = true, placeholder = { Text("bot_saya.mq5", fontSize = 12.sp) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (saveAsName.isNotBlank()) {
                        val path = if (saveAsName.contains(":")) saveAsName.trim() else "D:\\Work\\" + saveAsName.trim()
                        fileContents[path] = fileContents[activeTab] ?: "// ${path.substringAfterLast('\\')}\n"
                        ensureFsEntry(path)
                        openTabs.add(path)
                        activeTab = path
                        outputLogs.add(0, "💾 saved as → $path (tampil di File Explorer)")
                        saveAsDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { saveAsDialog = false }) { Text("Batal") } }
        )
    }
}

// Helper kecil: tambahkan SimFile ke folder FS (dipakai Compile EA)
fun SimFileAdd(fsOwner: Any?, fs: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>, folder: String, name: String) {
    val list = fs[folder]?.toMutableList() ?: mutableListOf()
    if (list.none { it.name == name }) list.add(SimFile(name, false, "2 KB"))
    fs[folder] = list
}

// Mini interpreter Python (dipakai Python Shell + Rofwin Code ▶ Run)
fun evalPyPow(e: String): Double? {
    var expr = e.replace(" ", "")
    val powIdx = expr.indexOf("**")
    if (powIdx > 0) {
        var l = powIdx - 1
        while (l >= 0 && (expr[l].isDigit() || expr[l] == '.')) l--
        var r = powIdx + 2
        while (r < expr.length && (expr[r].isDigit() || expr[r] == '.')) r++
        val base = expr.substring(l + 1, powIdx).toDoubleOrNull()
        val ex = expr.substring(powIdx + 2, r).toDoubleOrNull()
        if (base != null && ex != null) {
            expr = expr.substring(0, l + 1) + Math.pow(base, ex).toString() + expr.substring(r)
        }
    }
    return evalPy(expr)
}

fun fmtPyNum(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

fun runMiniPython(code: String): List<String> {
    val out = mutableListOf<String>()
    for (rawLine in code.lines()) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("import ") || line.startsWith("from ") || line.startsWith("def ") || line.startsWith("class ") || line.endsWith(":")) continue
        val body = line.trimStart()
        val pm = Regex("^print\\((.*)\\)$").find(body)
        when {
            pm != null -> {
                val inner = pm.groupValues[1].trim()
                val q = Regex("^\"(.*)\"$|^'(.*)'$").find(inner)
                if (q != null) out.add(q.groupValues[1].ifEmpty { q.groupValues[2] })
                else {
                    val v = evalPyPow(inner)
                    out.add(if (v == null) "NameError: cannot evaluate '$inner'" else fmtPyNum(v))
                }
            }
            body.contains('=') && !body.contains("==") -> { /* assignment — skip */ }
            else -> {
                val v = evalPyPow(body)
                if (v != null) out.add(fmtPyNum(v))
            }
        }
    }
    return out
}

// =====================================================================
// Wine Configuration (winecfg) — Applications / Libraries / Drives / Graphics
// =====================================================================
@Composable
fun WineCfgWindow() {
    var tab by remember { mutableStateOf("Libraries") }
    val overrides = remember {
        mutableStateListOf(
            "d3d11" to "builtin", "dxgi" to "builtin", "d3dcompiler_47" to "native",
            "winhttp" to "native, builtin", "mscoree" to "disabled"
        )
    }
    var newOverride by remember { mutableStateOf("") }
    var winVersion by remember { mutableStateOf("Windows 10") }
    var virtualDesktop by remember { mutableStateOf(true) }
    var captureMouse by remember { mutableStateOf(true) }
    var resolution by remember { mutableStateOf("1280x720") }
    var statusMsg by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Applications", "Libraries", "Drives", "Graphics").forEach { t ->
                Text(
                    t,
                    color = if (t == tab) Win11Accent else Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = if (t == tab) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { tab = t }.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Divider(color = Win11Stroke, modifier = Modifier.padding(vertical = 4.dp))
        when (tab) {
            "Applications" -> Column {
                Text("Windows Version:", color = Color.White, fontSize = 12.sp)
                listOf("Windows 11", "Windows 10", "Windows 8.1", "Windows 7").forEach { v ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { winVersion = v }.padding(vertical = 3.dp)) {
                        RadioButton(selected = winVersion == v, onClick = { winVersion = v })
                        Text(v, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            "Libraries" -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newOverride, onValueChange = { newOverride = it }, singleLine = true, placeholder = { Text("new override (mis. quartz)", fontSize = 10.sp) }, modifier = Modifier.weight(1f).height(44.dp), textStyle = TextStyle(fontSize = 11.sp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = {
                        if (newOverride.isNotBlank() && overrides.none { it.first == newOverride }) {
                            overrides.add(newOverride to "native, builtin"); newOverride = ""
                        }
                    }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Add", fontSize = 10.sp) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn {
                    items(overrides) { ov ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val i = overrides.indexOf(ov)
                                overrides[i] = ov.first to when (ov.second) {
                                    "native" -> "builtin"; "builtin" -> "native, builtin"
                                    "native, builtin" -> "disabled"; else -> "native"
                                }
                            }.padding(vertical = 4.dp)
                        ) {
                            Text("*${ov.first}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Text(ov.second, color = SecondaryTeal, fontSize = 10.sp)
                        }
                    }
                }
                Text("(klik untuk putar mode: native/builtin/disabled)", color = TextSecondary, fontSize = 9.sp)
            }
            "Drives" -> Column {
                listOf(
                    Triple("C:", "..\\..", "Bottle root FS"),
                    Triple("D:", "OBB space", "Game/app data"),
                    Triple("E:", "Downloads", "User downloads"),
                    Triple("Z:", "/", "Android root (hati-hati)")
                ).forEach { (d, target, desc) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, tint = Win11Accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(d, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                        Column {
                            Text(target, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(desc, color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }
            "Graphics" -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = virtualDesktop, onCheckedChange = { virtualDesktop = it })
                    Text("Emulate a virtual desktop", color = Color.White, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Desktop size:", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = resolution, onValueChange = { resolution = it }, singleLine = true, modifier = Modifier.width(110.dp).height(44.dp), textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = captureMouse, onCheckedChange = { captureMouse = it })
                    Text("Automatically capture the mouse", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (statusMsg.isNotEmpty()) Text(statusMsg, color = Color(0xFF4CAF50), fontSize = 10.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { statusMsg = "✔ Settings applied ke container (winetricks & drives sinkron)" }) { Text("Apply") }
            TextButton(onClick = { statusMsg = "✔ OK — konfigurasi disimpan"; }) { Text("OK") }
        }
    }
}

// =====================================================================
// Winetricks — installer paket Windows DLL/runtime (progress hidup)
// =====================================================================
@Composable
fun WinetricksWindow() {
    data class Verb(val name: String, val desc: String)
    val verbs = listOf(
        Verb("corefonts", "Arial, Times, Courier (font dasar Windows)"),
        Verb("d3dx9", "Direct3D 9 runtime penuh"),
        Verb("d3dcompiler_47", "Shader compiler untuk DX11"),
        Verb("vcrun2019", "Visual C++ 2015-2022 Redistributable"),
        Verb("dotnet48", ".NET Framework 4.8 (berat)"),
        Verb("dxvk", "DirectX→Vulkan (Mali tidak mendukung—sim saja)"),
        Verb("vb6run", "Visual Basic 6 runtime"),
        Verb("directplay", "DirectPlay jaringan game lama"),
        Verb("faudio", "Audio XAudio2 reimplementation"),
        Verb("allfonts", "Semua font Microsoft")
    )
    val installed = remember { mutableStateMapOf<String, Boolean>() }
    val progress = remember { mutableStateMapOf<String, Float>() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Text("Pilih paket untuk di-install ke container:", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        LazyColumn {
            items(verbs) { v ->
                val done = installed[v.name] == true
                val prog = progress[v.name] ?: -1f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(
                        if (done) Icons.Default.CheckCircle else Icons.Default.Archive,
                        contentDescription = null,
                        tint = if (done) Color(0xFF4CAF50) else Color(0xFF999999),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(v.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(v.desc, color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (prog >= 0f) {
                            LinearProgressIndicator(progress = prog, color = Win11Accent, trackColor = Color(0xFF333333), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (done) {
                        Text("installed ✔", color = Color(0xFF4CAF50), fontSize = 9.sp)
                    } else {
                        Button(
                            onClick = {
                                if (prog < 0f) {
                                    scope.launch {
                                        var p = 0f
                                        progress[v.name] = 0f
                                        while (p < 1f) { delay(130); p += 0.12f; progress[v.name] = p.coerceAtMost(1f) }
                                        progress.remove(v.name)
                                        installed[v.name] = true
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) { Text(if (prog >= 0f) "${(prog * 100).toInt()}%" else "Install", fontSize = 9.sp) }
                    }
                }
            }
        }
        Text("Paket terinstall menandai container (lihat winecfg).", color = TextSecondary, fontSize = 9.sp)
    }
}

// =====================================================================
// ROC AI — asisten trading & coding.
// ONLINE: tempel API key Groq/OpenAI-compatible (disimpan LOKAL di HP,
// tidak pernah ditulis ke source/ log). OFFLINE: otak rule-based lokal.
// =====================================================================
@Composable
fun RocAiWindow(
    fileContents: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>,
    expertAdvisors: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    mids: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Double>,
    positions: androidx.compose.runtime.snapshots.SnapshotStateList<Mt5Pos>,
    balance: Float,
    pluginsOn: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    bubbleOn: Boolean,
    onBubbleChange: (Boolean) -> Unit,
    onLaunch: (DesktopWindow) -> Unit
) {
    val context = LocalContext.current
    val aiPrefs = remember { context.getSharedPreferences("RofwinAI", android.content.Context.MODE_PRIVATE) }
    var apiBase by remember { mutableStateOf(aiPrefs.getString("base", "https://api.groq.com/openai/v1") ?: "https://api.groq.com/openai/v1") }
    // v1.8.2 — API key tersimpan TERENKRIPSI (SecureBox / Android Keystore);
    // nilai legacy plaintext dari v1.8.1 ke bawah tetap terbaca & otomatis terenkripsi saat diedit.
    var apiKey by remember { mutableStateOf(SecureBox.decryptFrom(aiPrefs.getString("key", "") ?: "")) }
    var model by remember { mutableStateOf(aiPrefs.getString("model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile") }
    var showSettings by remember { mutableStateOf(false) }
    val chat = remember {
        mutableStateListOf<Pair<Boolean, String>>().apply {
            add(false to "Halo! Saya ROC-AI 🤖 — asisten coding & bot trading Anda.\n• ONLINE: tempel API key di ⚙ Settings (Groq/OpenAI-compatible)\n• OFFLINE: tetap menjawab via otak lokal (analisis momentum, template EA)\nCoba tombol cepat di bawah.")
        }
    }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun pushUser(t: String) { chat.add(true to t) }
    fun pushBot(t: String) { chat.add(false to t); if (chat.size > 120) chat.removeAt(0) }

    fun marketDigest(): String {
        val top = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "BTCUSD", "USDIDR")
        val px = top.joinToString(" ") { s -> "$s=${mt5Fmt(s, mids[s] ?: 0.0)}" }
        val pos = if (positions.isEmpty()) "tanpa posisi" else positions.joinToString(" ") { p -> "${if (p.buy) "BUY" else "SELL"}_${p.lots}_${p.sym}" }
        return "balance=${"%.2f".format(balance)} | $px | $pos | EA compiled=${expertAdvisors.size}"
    }

    fun momentumOf(sym: String): Double {
        val base = MT5_BASELINE[sym] ?: return 0.0
        val now = mids[sym] ?: base
        return (now - base) / base * 100.0
    }

    fun offlineBrain(promptRaw: String): String {
        val prompt = promptRaw.lowercase()
        return when {
            "analisis" in prompt || "market" in prompt || "sinyal" in prompt -> {
                val watch = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "BTCUSD", "USDIDR")
                val sb = StringBuilder("📊 Analisis momentum lokal (vs baseline):\n")
                watch.forEach { s ->
                    val m = momentumOf(s)
                    val arrow = if (m > 0.02) "▲" else if (m < -0.02) "▼" else "◆"
                    sb.append("$arrow $s ${"%+.3f".format(m)}% @ ${mt5Fmt(s, mids[s] ?: 0.0)}\n")
                }
                val best = watch.maxByOrNull { momentumOf(it) } ?: "EURUSD"
                val worst = watch.minByOrNull { momentumOf(it) } ?: "GBPUSD"
                sb.append("➜ Terkuat: $best (bias BUY) · Terlemah: $worst (bias SELL).\n")
                if (positions.isNotEmpty()) sb.append("⚠ ${positions.size} posisi terbuka — pertimbangkan trailing SL.")
                else sb.append("Tidak ada posisi — momentum-scalp 0.01 lot cocok.")
                sb.toString()
            }
            "ea" in prompt || "bot" in prompt || "expert" in prompt ->
                "🤖 Status EA: ${expertAdvisors.size} terkompilasi" + (if (expertAdvisors.isNotEmpty()) " (${expertAdvisors.joinToString()})" else "") +
                        ".\nAlur: 1) Rofwin Code → ＋ New 'nama.mq5' 2) ⚙ Compile 3) MT5 → Navigator → Attach.\nKetik: buat ea <nama> <strategi> — saya tuliskan kodenya (online: AI sungguhan, offline: template MA-cross)."
            "sync" in prompt || "rocagents" in prompt ->
                "🔄 Tekan tombol 'Sync rocagents' di bawah — saya tarik struktur terbaru dari GitHub ivansslo/rocagents ke D:\\Work\\rocagents dan perbarui README. Selalu sinkron setiap repo Anda berubah."
            "compile" in prompt ->
                "⚙ Compiler lokal memeriksa keseimbangan kurung + nama file .mq5. EA terdaftar: ${expertAdvisors.size}. Error umum: '{' - unbalanced parentheses → hitung { dan }."
            "help" in prompt || prompt.isBlank() ->
                "Perintah saya:\n• 'analisis market'\n• 'buat ea <nama> <strategi>'\n• 'status ea'\n• 'sync rocagents'\n• bebas bertanya (online mode dengan key)"
            else -> "🤖 (offline) Terima: '$promptRaw'. Dengan API key di ⚙ Settings saya menjawab penuh via ${model}. Market kini: ${marketDigest()}"
        }
    }

    fun makeEa(rawName: String, desc: String, viaTemplate: Boolean) {
        val safe = rawName.replace(Regex("[^A-Za-z0-9_]"), "_").ifBlank { "EA_AI" }
        val path = "D:\\Work\\$safe.mq5"
        if (viaTemplate) {
            fileContents[path] = AI_EA_TEMPLATE.replace("MA Cross Scalper", "$safe — $desc")
            SimFileAdd(null, simulatedFiles, "D:\\Work", "$safe.mq5")
            pushBot("✔ EA '$safe' dibuat (template lokal MA-cross).\nBuka Rofwin Code → Compile ⚙ → MT5 attach.")
            onLaunch(DesktopWindow.CODE_EDITOR)
        }
    }

    fun callOnline(userText: String, isEaGen: Boolean, eaName: String) {
        busy = true
        val systemPrompt = if (isEaGen)
            "Anda generator kode MQL5. Balas HANYA kode .mq5 valid (OnInit/OnTick), tanpa penjelasan, tanpa markdown fence. Strategi: $userText"
        else "Kamu ROC-AI, asisten aplikasi Rofwin (Android Windows 11 sim). Jawab ringkas & teknis dalam Bahasa Indonesia." + (if (pluginsOn["digest"] != false) " Konteks live: ${marketDigest()}" else "")
        val body = "{\"model\":${jsonStr(model)},\"temperature\":0.3,\"max_tokens\":${if (isEaGen) "900" else "500"},\"messages\":[" +
                "{\"role\":\"system\",\"content\":${jsonStr(systemPrompt)}}," +
                "{\"role\":\"user\",\"content\":${jsonStr(userText)}}]}"
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val (code, resp) = httpPostJson("$apiBase/chat/completions", apiKey, body)
            val content = try {
                kotlinx.serialization.json.Json.parseToJsonElement(resp).jsonObject["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
            } catch (_: Exception) { null }
            if (isEaGen && code == 200 && content != null) {
                val clean = content.replace("```mql5", "").replace("```", "").trim()
                val safe = eaName.replace(Regex("[^A-Za-z0-9_]"), "_").ifBlank { "EA_AI" }
                val path = "D:\\Work\\$safe.mq5"
                fileContents[path] = clean + "\n"
                SimFileAdd(null, simulatedFiles, "D:\\Work", "$safe.mq5")
                pushBot("✔ EA '$safe' ditulis oleh AI (${clean.lines().size} baris) → tersimpan di $path.\nBuka Rofwin Code → Compile ⚙.")
            } else {
                pushBot(
                    when {
                        code == 200 && content != null -> content
                        code == -1 -> "⚠ Tidak ada koneksi. Mode offline:\n" + offlineBrain(userText)
                        else -> "⚠ API error $code (key/model?). Offline fallback:\n" + offlineBrain(userText)
                    }
                )
            }
            busy = false
        }
    }

    fun send(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        pushUser(t)
        input = ""
        val lower = t.lowercase()
        when {
            lower.startsWith("buat ea ") && pluginsOn["eagen"] == false -> pushBot("🔌 Plugin 'EA Generator' sedang NONAKTIF — aktifkan di ⚙ Settings → Plugins.")
            lower.startsWith("buat ea ") -> {
                val rest = t.substring(8).trim()
                val parts = rest.split(" ", limit = 2)
                val name = parts.getOrElse(0) { "EA_AI" }
                val desc = parts.getOrElse(1) { "strategi umum" }
                if (apiKey.isBlank()) {
                    pushBot("🔌 OFFLINE — membangun dari template lokal...")
                    makeEa(name, desc, true)
                } else {
                    pushBot("🌐 Menulis EA '$name' via AI ($model)...")
                    callOnline("nama:$name; strategi:$desc", true, name)
                }
            }
            lower == "analisis" || lower.startsWith("analisis") ->
                if (apiKey.isBlank()) pushBot(offlineBrain(t)) else callOnline("Analisis market sim sekarang & saran posisi saya", false, "")
            lower.startsWith("sync") -> pushBot("🔄 Gunakan tombol Sync di bawah (menarik tree GitHub penuh).")
            apiKey.isBlank() -> pushBot(offlineBrain(t))
            else -> callOnline(t, false, "")
        }
    }

    fun syncRocAgents() {
        busy = true
        pushBot("🔄 Sync ivansslo/rocagents dari GitHub…")
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val (code, body) = httpGet("https://api.github.com/repos/ivansslo/rocagents/git/trees/HEAD?recursive=1")
            var added = 0
            if (code == 200) {
                try {
                    val arr = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject["tree"]!!.jsonArray
                    // bangun peta folder → entries (tanpa sessions/ dan __pycache__)
                    val folders = mutableMapOf<String, MutableList<SimFile>>()
                    arr.forEach { el ->
                        val o = el.jsonObject
                        val p = o["path"]!!.jsonPrimitive.content
                        if (!p.contains("__pycache__") && !p.startsWith("sessions/")) {
                            val isDir = o["type"]!!.jsonPrimitive.content == "tree"
                            val parent = p.substringBeforeLast('/', "")
                            val name = p.substringAfterLast('/')
                            folders.getOrPut(parent) { mutableListOf() }.add(SimFile(name, isDir, if (isDir) "Folder" else "3 KB"))
                            if (isDir) folders.getOrPut(p) { mutableListOf() }
                        }
                    }
                    folders.forEach { (parent, list) ->
                        val key = "D:\\Work\\rocagents" + (if (parent.isEmpty()) "" else "\\" + parent.replace('/', '\\'))
                        val existing = simulatedFiles[key] ?: emptyList()
                        val newcomers = list.filter { nf -> existing.none { it.name == nf.name } }
                        if (existing.isEmpty() && !simulatedFiles.containsKey(key)) {
                            simulatedFiles[key] = list
                            added += list.size
                        } else if (newcomers.isNotEmpty()) {
                            simulatedFiles[key] = existing + newcomers
                            added += newcomers.size
                        }
                    }
                    // perbarui README juga
                    val (rc, rb) = httpGet("https://raw.githubusercontent.com/ivansslo/rocagents/HEAD/README.md")
                    if (rc == 200) fileContents["D:\\Work\\rocagents\\README.md"] = rb.take(4000)
                } catch (e: Exception) {
                    pushBot("⚠ parse tree gagal: ${e.message}")
                }
            }
            pushBot(if (code == 200) "✔ Sync selesai: +$added entri baru di D:\\Work\\rocagents + README terbaru (HEAD)." else "⚠ Sync gagal (http $code) — cek koneksi.")
            busy = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF141A22))) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1B2330)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SmartToy, null, tint = Win11Accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ROC-AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (apiKey.isBlank()) "OFFLINE mode (otak lokal)" else "ONLINE — $model",
                    color = if (apiKey.isBlank()) Color(0xFFFFB74D) else Color(0xFF4CAF50),
                    fontSize = 9.sp
                )
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Win11Accent)
            IconButton(onClick = { showSettings = !showSettings }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Settings, "Settings", tint = Color(0xFFCCCCCC), modifier = Modifier.size(15.dp))
            }
        }

        // Settings (key disimpan LOKAL di prefs HP — tidak pernah ke source/log)
        if (showSettings) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1B2330)).padding(8.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; aiPrefs.edit().putString("key", SecureBox.encryptTo(it)).apply() },
                    label = { Text("API Key (Groq: gsk_... / OpenAI-compatible)", fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it; aiPrefs.edit().putString("model", it).apply() },
                        label = { Text("Model", fontSize = 10.sp) }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = apiBase,
                        onValueChange = { apiBase = it; aiPrefs.edit().putString("base", it).apply() },
                        label = { Text("Base URL", fontSize = 10.sp) }, singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                Text("Key tersimpan hanya di HP Anda (SharedPreferences). Untuk Groq gratis: console.groq.com → API Keys.", color = TextSecondary, fontSize = 8.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Bubble mengambang", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Switch(checked = bubbleOn, onCheckedChange = onBubbleChange, modifier = Modifier.height(28.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Plugins (AI new era — modular)", color = Win11Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                AI_PLUGINS.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { pluginsOn[p.id] = pluginsOn[p.id] == false }.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            if (pluginsOn[p.id] != false) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            null,
                            tint = if (pluginsOn[p.id] != false) Win11Accent else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(p.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(p.desc, color = TextSecondary, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        // Quick actions
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(4.dp)) {
            val quickChips = buildList<Pair<String, () -> Unit>> {
                add("📊 Analisis market" to { send("analisis market") })
                if (pluginsOn["eagen"] != false) add("🤖 Buat EA…" to { send("buat ea EA_AI scalp MA7-21") })
                if (pluginsOn["rocsync"] != false) add("🔄 Sync rocagents" to { syncRocAgents() })
                add("📈 Status posisi" to { pushBot("📈 " + marketDigest()) })
                add("🧪 Cek compile" to { pushBot(offlineBrain("compile")) })
            }
            quickChips.forEach { (label, act) ->
                Text(
                    label,
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .padding(3.dp)
                        .background(Color(0xFF243040), RoundedCornerShape(12.dp))
                        .clickable(enabled = !busy) { act() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        // Chat
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            items(chat) { (isUser, text) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    val isCrit = !isUser && detectCritical(text) && pluginsOn["critical"] != false
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(
                                if (isUser) Color(0xFF274B73) else if (isCrit) Color(0xFF3A1616) else Color(0xFF1F2937),
                                RoundedCornerShape(8.dp)
                            )
                            .border(if (isCrit) 1.dp else 0.dp, if (isCrit) Color(0xFFFF5252) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (isCrit) Text("⚠ CRITICAL TEXT terdeteksi", color = Color(0xFFFF5252), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(text, color = if (isCrit) Color(0xFFFFCFCF) else Color(0xFFE8E8E8), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1B2330)).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("tanya / 'buat ea <nama> <strategi>'…", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) })
            )
            IconButton(onClick = { send(input) }, enabled = !busy) {
                Icon(Icons.Default.Send, "Kirim", tint = if (busy) Color.Gray else Win11Accent)
            }
        }
    }
}

// =====================================================================
// Git Bash + rocd multi-OS (Ubuntu/Debian/Alpine/Fedora/Arch containers)
// =====================================================================
@Composable
fun GitBashWindow(simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>) {
    val logs = remember { mutableStateListOf("MINGW64 Git Bash + rocd multi-OS — ketik 'help'") }
    var input by remember { mutableStateOf("") }
    // Multi-OS containers (rocd): true = running
    val containers = remember {
        mutableStateMapOf(
            "ubuntu-22.04" to true, "debian-12" to false, "alpine-3.20" to false,
            "fedora-40" to false, "archlinux" to false
        )
    }
    var currentOs by remember { mutableStateOf<String?>(null) }

    fun run(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        logs.add(if (currentOs != null) "root@$currentOs:/# $t" else "$ $t")
        val parts = t.split(" ").filter { it.isNotBlank() }
        val c0 = parts.getOrElse(0) { "" }
        val c1 = parts.getOrElse(1) { "" }
        val c2 = parts.getOrElse(2) { "" }
        when {
            // ---- rocd multi-OS (dari repo ivansslo/rocd) ----
            t == "rocd" || t == "rocd help" -> {
                logs.add("rocd — ROC Distro (container manager)")
                logs.add("  rocd list | rocd ps          daftar OS")
                logs.add("  rocd create <os> <nama>      buat (ubuntu/debian/alpine/fedora/arch)")
                logs.add("  rocd start|stop <nama>       hidup/matikan")
                logs.add("  rocd enter <nama>            masuk shell OS")
                logs.add("  rocd remove <nama>           hapus container")
                logs.add("  exit                         keluar container")
            }
            t == "rocd list" || t == "rocd ps" -> {
                logs.add("NAME            STATUS      DISTRO")
                containers.forEach { (n, running) ->
                    logs.add("${n.padEnd(16)}${(if (running) "running" else "stopped").padEnd(12)}${n.substringBefore('-')}")
                }
            }
            t.startsWith("rocd create ") -> {
                val newName = if (c2.isNotBlank()) "$c0-$c2".replace("rocd-", "") else "ubuntu-${(containers.size + 20)}"
                val os = c2.ifBlank { c1 }
                if (os !in listOf("ubuntu", "debian", "alpine", "fedora", "arch")) {
                    logs.add("rocd: distro '$os' tidak dikenal (pilih: ubuntu/debian/alpine/fedora/arch)")
                } else {
                    val name = "$os-${100 + containers.size}"
                    containers[name] = false
                    logs.add("rocd: pulling $os:latest rootfs… done (sim)")
                    logs.add("rocd: container '$name' created. 'rocd start $name' untuk menjalankan.")
                }
            }
            t.startsWith("rocd start ") -> {
                val n = t.removePrefix("rocd start ").trim()
                if (containers.containsKey(n)) { containers[n] = true; logs.add("rocd: '$n' started (udocker)") } else logs.add("rocd: '$n' tidak ada")
            }
            t.startsWith("rocd stop ") -> {
                val n = t.removePrefix("rocd stop ").trim()
                if (containers.containsKey(n)) { containers[n] = false; if (currentOs == n) currentOs = null; logs.add("rocd: '$n' stopped") } else logs.add("rocd: '$n' tidak ada")
            }
            t.startsWith("rocd enter ") || t.startsWith("rocd login ") -> {
                val n = t.split(" ").last()
                if (containers[n] == true) {
                    currentOs = n
                    logs.add("root@$n:~# (selamat datang di $n — 'exit' untuk keluar)")
                } else if (containers.containsKey(n)) {
                    logs.add("rocd: '$n' sedang stopped — 'rocd start $n' dulu")
                } else logs.add("rocd: '$n' tidak ada (lihat 'rocd list')")
            }
            t.startsWith("rocd remove ") -> {
                val n = t.removePrefix("rocd remove ").trim()
                if (containers.containsKey(n)) { if (currentOs == n) currentOs = null; containers.remove(n); logs.add("rocd: '$n' removed") } else logs.add("rocd: '$n' tidak ada")
            }
            t == "exit" && currentOs != null -> {
                logs.add("logout $currentOs")
                currentOs = null
            }

            // ---- git clone rocagents (menanam struktur nyata jika belum ada) ----
            t.startsWith("git clone") && t.contains("rocagents") -> {
                if (simulatedFiles.containsKey("D:\\Work\\rocagents")) {
                    logs.add("rocagents already cloned — 'Already up to date.' (HEAD)")
                } else {
                    logs.add("Cloning into 'D:\\Work\\rocagents'…")
                    logs.add("remote: Enumerating objects: 57, done.")
                    seedRocAgentsFs().forEach { (k, v) -> simulatedFiles[k] = v }
                    val list = simulatedFiles["D:\\Work"]?.toMutableList() ?: mutableListOf()
                    if (list.none { it.name == "rocagents" }) { list.add(SimFile("rocagents", true)); simulatedFiles["D:\\Work"] = list }
                    logs.add("done. 57 objects — cek File Explorer / 'ls D:\\Work\\rocagents' di Terminal")
                }
            }

            t == "help" -> {
                logs.add("git: status log branch | clone https://github.com/ivansslo/rocagents")
                logs.add("rocd: list create start stop enter remove (multi-OS)")
                logs.add("umum: ls pwd uname clear (mengikuti OS aktif)")
            }
            t == "git status" -> { logs.add("On branch main"); logs.add("nothing to commit, working tree clean") }
            t == "git log" || t == "git log --oneline" -> {
                logs.add("3b5aabb (HEAD -> main, tag: v1.5.0) feat: Coder & Trader Edition")
                logs.add("fce5ff3 (tag: v1.4.0) feat: Windows 11 Edition")
            }
            t == "git branch" -> logs.add("* main")
            t == "ls" -> {
                if (currentOs != null) logs.add("bin  etc  home  root  usr  var")
                else logs.add("app/  rocd/  rocagents/  README.md  build.gradle.kts")
            }
            t == "pwd" -> logs.add(if (currentOs != null) "/root" else "/c/rofwin")
            t == "uname -a" -> {
                logs.add(
                    when (currentOs) {
                        null -> "Linux CPH1823 6.6.30-android15-8-g #1 SMP aarch64 GNU/Linux"
                        "alpine-3.20" -> "Linux alpine-3-20 6.6.30 #1-Alpine SMP x86_64 Linux"
                        else -> "Linux $currentOs 6.8.0-45-generic #45-Ubuntu SMP x86_64 GNU/Linux"
                    }
                )
            }
            t == "clear" -> logs.clear()
            else -> logs.add("${if (currentOs != null) "bash" else "bash"}: $t: command not found")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(8.dp)) {
        Text(
            if (currentOs != null) "rocd ▶ $currentOs (running)" else "ivansslo@CPH1823 MINGW64 /c/rofwin",
            color = if (currentOs != null) Color(0xFF4CC2FF) else Color(0xFFADFF2F),
            fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
        if (currentOs == null) {
            Text("containers: ${containers.count { it.value }} running / ${containers.size} total — 'rocd list'", color = Color(0xFF888888), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { Text(it, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (currentOs != null) "/# " else "$ ", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    run(input)
                    input = ""
                })
            )
        }
    }
}

// =====================================================================
// v1.7.0 — MT5/MQL5 Setup: DOWNLOAD NYATA installer desktop (HTTP) +
// install ke FS Windows di dalam APK jenius (live ticks/order/EA global).
// =====================================================================
@Composable
fun Mt5SetupWindow(
    simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>,
    journalLogs: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    installed: Boolean,
    onInstalled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("https://download.mql5.com/cdn/web/metaquotes.software.corp/mt5/mt5setup.exe") }
    var busy by remember { mutableStateOf(false) }
    var dlBytes by remember { mutableLongStateOf(0L) }
    var status by remember { mutableStateOf("Siap. URL default = installer resmi MetaQuotes (file desktop ASLI).") }
    var downloaded by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF141B22)).padding(10.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Download, null, tint = Win11Accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("MT5 / MQL5 Setup", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (installed) "✔ Terinstal di C:\\Program Files\\MetaTrader 5 — LIVE" else "download nyata → install di dalam APK jenius",
                    color = if (installed) Color(0xFF4CAF50) else Color(0xFF9FB2CC), fontSize = 9.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL installer (.exe)", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        Button(enabled = !busy, onClick = {
            busy = true
            dlBytes = 0
            status = "Mengunduh (NYATA via HTTP)…"
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val dest = java.io.File(context.filesDir, "downloads/mt5setup.exe")
                val (ok, res) = downloadToFile(url.trim(), dest) { b -> dlBytes = b }
                if (ok) {
                    val totalB = res.toLongOrNull() ?: 0L
                    val mbText = "%.2f MB".format(totalB / 1048576f)
                    putSimFile(simulatedFiles, "C:\\Downloads", "mt5setup.exe", mbText)
                    status = "✔ Download NYATA selesai → C:\\Downloads\\mt5setup.exe ($mbText)"
                    downloaded = true
                    journalLogs.add(0, "downloaded mt5setup.exe ($totalB bytes, real http)")
                } else {
                    status = "🔴 Download gagal: $res — cek internet/URL. (Install simulasi tetap bisa.)"
                }
                busy = false
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Mengunduh… ${"%.1f".format(dlBytes / 1048576f)} MB" else "⬇ Download Installer (NYATA)", fontSize = 11.sp)
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Spacer(modifier = Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 110.dp)) {
            items(status.split("\n")) { s ->
                Text(s, color = if (detectCritical(s) || s.startsWith("🔴")) Color(0xFFFF8A80) else if (s.startsWith("✔")) Color(0xFF81C784) else Color(0xFF9FB2CC), fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            enabled = !busy && !installing && !installed,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            onClick = {
                installing = true
                scope.launch {
                    status = status + "\n▸ mengekstrak terminal64.exe, metaeditor64.exe, MQL5\\…"
                    delay(800)
                    putSimFile(simulatedFiles, "C:\\Program Files\\MetaTrader 5", "terminal64.exe", "48,1 MB")
                    putSimFile(simulatedFiles, "C:\\Program Files\\MetaTrader 5", "metaeditor64.exe", "21,7 MB")
                    putSimFile(simulatedFiles, "C:\\Program Files\\MetaTrader 5\\MQL5\\Experts", "README.txt", "1 KB")
                    putSimFile(simulatedFiles, "C:\\Program Files\\MetaTrader 5\\MQL5\\Include", "Trade.mqh", "212 KB")
                    delay(600)
                    onInstalled(true)
                    installing = false
                    status = status + "\n✔ Instalasi selesai — MetaTrader 5 LIVE: feed 24 simbol, order, EA auto-trade jalan global."
                    journalLogs.add(0, "MT5 installed to Program Files (sim) — live mode ON")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (installed) "✔ Sudah Terinstal" else if (installing) "Menginstal…" else "⚙ Install ke C:\\Program Files\\MetaTrader 5", fontSize = 11.sp) }
        if (!downloaded && !installed) {
            Text("Install bisa tanpa download (mode simulasi penuh). Download memberi file .exe ASLI di storage APK.", color = Color(0xFF9FB2CC), fontSize = 8.sp, modifier = Modifier.padding(top = 6.dp))
        }
        if (installed) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14261A))) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("LIVE aktif — segala hal jalan:", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("• ticks 24 simbol & EA auto-trade jalan walau jendela ditutup\n• SL/TP auto-close per tick\n• MQL5 ↔ MT5 sinkron via sesi tersimpan\n• Login akun: MT5 → tombol Login (WebTerminal REAL untuk akun asli)", color = Color(0xFF9FB2CC), fontSize = 9.sp)
                }
            }
        }
    }
}

// =====================================================================
// v1.7.0 — APK Studio: "compile" Android APK dari desktop Windows.
// Pipeline realistis + catatan jujur: build NYATA bisa via VM Builder →
// OCI Bridge (SSH JSch nyata menjalankan gradle assembleRelease).
// =====================================================================
@Composable
fun ApkStudioWindow(
    simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>,
    journalLogs: androidx.compose.runtime.snapshots.SnapshotStateList<String>
) {
    val scope = rememberCoroutineScope()
    var projName by remember { mutableStateOf("MyTraderApp") }
    var template by remember { mutableStateOf("Trading Bot") }
    var building by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val logs = remember { mutableStateListOf("APK Studio siap — pilih template lalu ▶ Build Release") }
    val rnd = remember { java.util.Random() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF181C14)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Android, null, tint = Color(0xFF3DDC84), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("APK Studio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Windows → Android compile · AGP 9.1 · Kotlin 2.1 · targetSdk 35", color = Color(0xFF9AA86F), fontSize = 9.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = projName,
            onValueChange = { projName = it.filter { c -> c.isLetterOrDigit() }.take(24) },
            label = { Text("Nama project", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            listOf("Trading Bot", "WebView App", "Empty").forEach { t ->
                Text(
                    t,
                    color = if (template == t) Color.White else Color(0xFF9AA86F), fontSize = 10.sp,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .background(if (template == t) Color(0xFF2E7D32) else Color(0xFF262D1E), RoundedCornerShape(6.dp))
                        .clickable { template = t }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(enabled = !building && projName.isNotBlank(), onClick = {
            building = true
            progress = 0f
            logs.clear()
            val proj = projName.trim()
            scope.launch {
                val failAt = if (rnd.nextInt(100) < 10) 5 + rnd.nextInt(5) else -1
                val stages = listOf(
                    "> Configure project :app",
                    "> Task :app:preBuild UP-TO-DATE",
                    "> Task :app:mergeReleaseResources",
                    "> Task :app:compileReleaseKotlin",
                    "> Task :app:compileReleaseJavaWithJavac",
                    "> Task :app:mergeDexRelease",
                    "> Task :app:lintVitalAnalyzeRelease",
                    "> Task :app:packageRelease",
                    "> Task :app:assembleRelease",
                    "zipalign -f -p 4 app-release.apk",
                    "apksigner sign --ks rofwin-local.keystore",
                    "apksigner verify --verbose ✔ 1 signer"
                )
                var ok = true
                run {
                    var i = 0
                    while (i < stages.size) {
                        if (i == failAt) {
                            logs.add("e: ${stages[i]} — ERROR: unresolved reference 'BuildConfig' (gagal simulasi)")
                            logs.add("FAILURE: Build failed with an exception. Transient — Coba Build lagi.")
                            journalLogs.add(0, "apk build FAILED: $proj (sim) — ERROR compile")
                            progress = 1f
                            ok = false
                            break
                        }
                        logs.add(stages[i])
                        i++
                        progress = i / stages.size.toFloat()
                        delay(340)
                    }
                }
                if (ok) {
                    progress = 1f
                    logs.add("BUILD SUCCESSFUL in ${18 + rnd.nextInt(40)}s")
                    val mb = "%.1f MB".format(5.5f + rnd.nextFloat() * 8f)
                    putSimFile(simulatedFiles, "D:\\Projects\\$proj", "app-release.apk", mb)
                    logs.add("output → D:\\Projects\\$proj\\app-release.apk ($mb)")
                    journalLogs.add(0, "apk built: D:\\Projects\\$proj\\app-release.apk ($mb) [sim]")
                }
                building = false
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (building) "Building… ${(progress * 100).toInt()}%" else "▶ Build Release APK", fontSize = 11.sp)
        }
        if (building) LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text("Jujur: pipeline di atas simulasi realistis. Build APK NYATA dari source tersedia — jalankan 'gradle assembleRelease' di repo Anda lewat VM Builder → OCI Bridge (SSH nyata).", color = Color(0xFF9AA86F), fontSize = 8.sp)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f).background(Color(0xFF101308), RoundedCornerShape(6.dp)).padding(6.dp)) {
            items(logs) { l ->
                Text(l, color = if (detectCritical(l)) Color(0xFFFF8A80) else if (l.contains("SUCCESSFUL") || l.contains("✔")) Color(0xFF8BC34A) else Color(0xFFB9C99B), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}

// =====================================================================
// v1.7.0 — VM Builder: rakit OS image Windows (sim, hasil FS + journal)
// + Rofwin Auth (integrasi: mengunci Start VM & OCI Bridge)
// + OCI Bridge: SSH NYATA (JSch) — skenario peningkatan lokal → OCI.
// =====================================================================
@Composable
fun VmBuilderWindow(
    simulatedFiles: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<SimFile>>,
    journalLogs: androidx.compose.runtime.snapshots.SnapshotStateList<String>
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("RofwinDrives", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Image") }

    // --- Auth (integrasi auth — gate Start VM & OCI Bridge) ---
    var authUser by remember { mutableStateOf(prefs.getString("vm_auth_user", "") ?: "") }
    var authInput by remember { mutableStateOf("") }
    var authPass by remember { mutableStateOf("") }
    val authed = authUser.isNotBlank()

    // --- Image builder ---
    val images = listOf("Windows 11 23H2 Pro", "Windows 10 22H2 Pro", "Windows Server 2022")
    var imgIdx by remember { mutableStateOf(0) }
    var ramGb by remember { mutableFloatStateOf(4f) }
    var building by remember { mutableStateOf(false) }
    var buildLog by remember { mutableStateOf("") }
    var vmRunning by remember { mutableStateOf(false) }
    var imageReady by remember { mutableStateOf(false) }

    // --- OCI bridge (SSH NYATA) ---
    var ociHost by remember { mutableStateOf(prefs.getString("oci_host", "161.118.253.28") ?: "161.118.253.28") }
    var ociUser by remember { mutableStateOf(prefs.getString("oci_user", "ubuntu") ?: "ubuntu") }
    var ociPass by remember { mutableStateOf("") }
    var ociPort by remember { mutableStateOf("22") }
    var ociBusy by remember { mutableStateOf(false) }
    var ociCmd by remember { mutableStateOf("uname -a") }
    val ociLogs = remember { mutableStateListOf<String>() }

    fun ociRun(cmd: String, tag: String) {
        if (ociBusy) return
        ociBusy = true
        ociLogs.add(0, "→ [$tag] $cmd")
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // v1.8.2 — kh = prefs -> host-key pinning TOFU aktif (anti-MITM)
            val (ok, out) = sshExec(ociHost.trim(), ociPort.toIntOrNull() ?: 22, ociUser.trim(), ociPass, cmd, kh = prefs)
            ociLogs.add(0, (if (ok) "✔ " else "🔴 ") + out.take(700))
            ociBusy = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF171B24)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dns, null, tint = Win11Accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("VM Builder Pro", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (authed) "auth: $authUser ✔ · jembatan lokal→OCI siap" else "belum login — Auth mengunci Start VM & OCI Bridge",
                    color = if (authed) Color(0xFF4CAF50) else Color(0xFFFFB74D), fontSize = 9.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            listOf("Image", "Auth", "OCI Bridge").forEach { t ->
                Text(
                    t,
                    color = if (tab == t) Color.White else Color(0xFF8FA3BF),
                    fontSize = 11.sp,
                    fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .background(if (tab == t) Win11AccentSolid else Color(0xFF232B3B), RoundedCornerShape(6.dp))
                        .clickable { tab = t }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (tab) {
            "Image" -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Rakit OS image Windows — hasil terdaftar di FS (D:\\VM) & journal.", color = Color(0xFF9FB2CC), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Image:", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(46.dp))
                    Text("‹", color = Win11Accent, fontSize = 18.sp, modifier = Modifier.clickable { imgIdx = (imgIdx - 1 + images.size) % images.size }.padding(6.dp))
                    Text(images[imgIdx], color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("›", color = Win11Accent, fontSize = 18.sp, modifier = Modifier.clickable { imgIdx = (imgIdx + 1) % images.size }.padding(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RAM ${ramGb.toInt()} GB", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(74.dp))
                    Slider(value = ramGb, onValueChange = { ramGb = it }, valueRange = 2f..16f, modifier = Modifier.weight(1f))
                }
                Button(enabled = !building, onClick = {
                    building = true
                    buildLog = ""
                    val img = images[imgIdx]
                    scope.launch {
                        val stages = listOf(
                            "dism /apply-image /imagefile:${img.replace(" ", "_")}.wim /index:1",
                            "menanam driver virtio + qemu-guest-agent…",
                            "unattend.xml → autologon RofwinAuth, timezone Asia/Jakarta",
                            "sysprep /generalize /oobe /shutdown",
                            "konversi → qcow2 (profil RAM ${ramGb.toInt()} GB)",
                        )
                        stages.forEach { s -> buildLog += "▸ $s\n"; delay(520) }
                        val name = img.replace(" ", "_") + ".qcow2"
                        putSimFile(simulatedFiles, "D:\\VM", name, "6,2 GB")
                        imageReady = true
                        building = false
                        buildLog += "✔ OS image siap: D:\\VM\\$name\n"
                        journalLogs.add(0, "vm image built: $name (sim)")
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (building) "Membangun…" else "⚒ Build OS Image", fontSize = 11.sp) }
                if (buildLog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(buildLog, color = if (buildLog.contains("✔")) Color(0xFF81C784) else Color(0xFF9FB2CC), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    enabled = imageReady && !building,
                    colors = ButtonDefaults.buttonColors(containerColor = if (authed) Color(0xFF2E7D32) else Color(0xFF555555)),
                    onClick = {
                        if (authed) {
                            vmRunning = !vmRunning
                            journalLogs.add(0, if (vmRunning) "VM started (sim) — auth OK via $authUser" else "VM stopped (sim)")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (!authed) "🔒 Start VM (butuh Auth dulu)" else if (vmRunning) "■ Stop VM (running)" else "▶ Start VM — integrasi auth ✔", fontSize = 11.sp) }
                if (vmRunning) Text("Console (sim): Windows login → RofwinAuth auto · RDP 127.0.0.1:3390", color = Color(0xFF81C784), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
            }
            "Auth" -> Column {
                if (authed) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14261A))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("✔ Terautentikasi sebagai $authUser", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Integrasi auth aktif untuk: Start VM, OCI Bridge, Sync sesi → OCI.", color = Color(0xFF9FB2CC), fontSize = 9.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        authUser = ""
                        prefs.edit().remove("vm_auth_user").apply()
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2424)), modifier = Modifier.fillMaxWidth()) { Text("Logout") }
                } else {
                    Text("Rofwin Auth — mengunci VM Builder & OCI Bridge (kredensial disimpan lokal di HP, tidak pernah dicetak).", color = Color(0xFF9FB2CC), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = authInput, onValueChange = { authInput = it }, label = { Text("Username", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = authPass, onValueChange = { authPass = it }, label = { Text("Password", fontSize = 10.sp) }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(enabled = authInput.isNotBlank() && authPass.isNotBlank(), onClick = {
                        authUser = authInput.trim()
                        prefs.edit().putString("vm_auth_user", authUser).apply()
                        authPass = ""
                        journalLogs.add(0, "auth login: $authUser (Rofwin Auth lokal)")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Login / Daftar") }
                }
            }
            else -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("OCI Bridge — SSH NYATA (JSch) ke VM Oracle. Nomor langkah mengikuti skenario peningkatan: ① test ② perintah ③ sync sesi.", color = Color(0xFF9FB2CC), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (!authed) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A14))) {
                        Text("🔒 Terkunci — login dulu di tab Auth (integrasi auth).", color = Color(0xFFFFB74D), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                    }
                } else {
                    Row {
                        OutlinedTextField(value = ociHost, onValueChange = { ociHost = it; prefs.edit().putString("oci_host", it).apply() }, label = { Text("Host", fontSize = 9.sp) }, singleLine = true, modifier = Modifier.weight(1.6f))
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(value = ociPort, onValueChange = { ociPort = it }, label = { Text("Port", fontSize = 9.sp) }, singleLine = true, modifier = Modifier.weight(0.8f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        OutlinedTextField(value = ociUser, onValueChange = { ociUser = it; prefs.edit().putString("oci_user", it).apply() }, label = { Text("User", fontSize = 9.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(value = ociPass, onValueChange = { ociPass = it }, label = { Text("Password (tidak disimpan)", fontSize = 9.sp) }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.weight(1.2f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        Button(enabled = !ociBusy && ociPass.isNotBlank(), onClick = { ociRun("echo LOGIN-SUKSES && hostname && uptime", "test") }, modifier = Modifier.weight(1f)) { Text("① Test Koneksi", fontSize = 10.sp) }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(enabled = !ociBusy && ociPass.isNotBlank(), onClick = {
                            val blob = prefs.getString("rofwin_session_v2", "") ?: ""
                            val b64 = java.util.Base64.getEncoder().encodeToString(blob.toByteArray(Charsets.UTF_8))
                            ociRun("echo $b64 | base64 -d > ~/rofwin_session.json && echo SYNC-OK", "sync sesi")
                        }, modifier = Modifier.weight(1f)) { Text("③ Sync Sesi → OCI", fontSize = 10.sp) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = ociCmd, onValueChange = { ociCmd = it }, label = { Text("② Perintah remote", fontSize = 9.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                        IconButton(enabled = !ociBusy && ociPass.isNotBlank(), onClick = { ociRun(ociCmd, "cmd") }) { Icon(Icons.Default.Send, "Run", tint = Win11Accent) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (ociBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(ociLogs) { l ->
                            Text(l, color = if (detectCritical(l) || l.startsWith("🔴")) Color(0xFFFF8A80) else if (l.startsWith("✔")) Color(0xFF81C784) else Color(0xFF9FB2CC), fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}


// ===== v1.8.1 — breadcrumb anti-FC =====
fun crumb(context: android.content.Context, tag: String) {
    try {
        val p = context.getSharedPreferences("RofwinCrash", android.content.Context.MODE_PRIVATE)
        val old = p.getString("crumbs", "") ?: ""
        val line = if (old.isEmpty()) tag else old + ">" + tag
        p.edit().putString("crumbs", line.takeLast(220)).apply()
    } catch (_: Exception) {}
}

// ===== v1.8.1 — layar error (menggantikan FC senyap) =====
@Composable
fun CrashScreen(text: String, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2B0A0A)).padding(18.dp)) {
        Text("Rofwin MENANGKAP error (bukan FC senyap) — screenshot / salin lalu kirim ke pengembang:", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)) {
            Text(text, color = Color(0xFFFFCDD2), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                "SALIN LOG", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF8A80)).clickable {
                    try {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("rofwin-crash", text))
                        android.widget.Toast.makeText(context, "Log tersalin — paste ke chat pengembang", android.widget.Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {}
                }.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "KEMBALI KE DASHBOARD", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF555555)).clickable { onClose() }.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

// ===== v1.8.1 — item menu panel taskbar =====
@Composable
fun TaskMenuItem(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 11.sp,
        color = if (enabled) Color.White else Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
