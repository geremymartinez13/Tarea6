package com.geremy.tarea6

import android.media.MediaPlayer
import android.os.Bundle
import android.text.Html
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

const val WP_SITE = "https://techcrunch.com"

suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val c = URL(url).openConnection() as HttpURLConnection
    c.setRequestProperty("User-Agent", "Mozilla/5.0")
    c.connectTimeout = 15000; c.readTimeout = 15000
    try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
}

fun enc(s: String) = URLEncoder.encode(s.trim(), "UTF-8")
fun html(s: String) = Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT).toString().trim()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val nav = rememberNavController()
    val titles = mapOf(
        "home" to "Caja de herramientas", "gender" to "Predecir género", "person" to "Persona aleatoria",
        "unis" to "Universidades", "weather" to "Clima en RD", "pokemon" to "Pokémon",
        "wp" to "Noticias WordPress", "about" to "Acerca de"
    )
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(titles[route] ?: "") },
            navigationIcon = {
                if (route != "home") IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                }
            })
    }) { p ->
        NavHost(nav, "home", Modifier.padding(p)) {
            composable("home") { Home(titles.filterKeys { it != "home" }) { nav.navigate(it) } }
            composable("gender") { GenderScreen() }
            composable("person") { PersonScreen() }
            composable("unis") { UniScreen() }
            composable("weather") { WeatherScreen() }
            composable("pokemon") { PokemonScreen() }
            composable("wp") { WpScreen() }
            composable("about") { AboutScreen() }
        }
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
    content = content
)

@Composable
fun SearchRow(label: String, onGo: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(text, { text = it }, Modifier.weight(1f), label = { Text(label) }, singleLine = true)
        Button(onClick = { if (text.isNotBlank()) onGo(text) }) { Text("Buscar") }
    }
}

@Composable
fun Home(items: Map<String, String>, go: (String) -> Unit) = Page {
    Image(painterResource(R.drawable.toolbox), "Caja de herramientas", Modifier.size(200.dp))
    Text("Seleccione una opción", fontSize = 16.sp)
    items.forEach { (route, title) -> Button(onClick = { go(route) }, Modifier.fillMaxWidth()) { Text(title) } }
}


@Composable
fun GenderScreen() {
    var state by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val bg = when (state) { "male" -> Color(0xFF64B5F6); "" -> Color.White; else -> Color(0xFFF48FB1) }
    Column(Modifier.fillMaxSize().background(bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchRow("Nombre") { n ->
            scope.launch {
                label = "Buscando..."
                try {
                    val g = JSONObject(httpGet("https://api.genderize.io/?name=${enc(n)}")).optString("gender", "null")
                    state = if (g == "male") "male" else "other"
                    label = when (g) { "male" -> "Masculino"; "female" -> "Femenino"; else -> "No se pudo determinar" }
                } catch (e: Exception) { label = "Error: ${e.message}" }
            }
        }
        Text(label, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PersonScreen() {
    var info by remember { mutableStateOf<List<String>>(emptyList()) }
    var photo by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch {
        try {
            val u = JSONObject(httpGet("https://randomuser.me/api/?results=1")).getJSONArray("results").getJSONObject(0)
            val n = u.getJSONObject("name"); val d = u.getJSONObject("dob")
            photo = u.getJSONObject("picture").getString("large")
            info = listOf("Nombre: ${n.getString("first")}", "Apellido: ${n.getString("last")}",
                "Nacimiento: ${d.getString("date").take(10)}", "Edad: ${d.getInt("age")}")
        } catch (e: Exception) { info = listOf("Error: ${e.message}") }
    }
    LaunchedEffect(Unit) { load() }
    Page {
        if (photo.isNotEmpty()) AsyncImage(photo, null, Modifier.size(150.dp), contentScale = ContentScale.Crop)
        info.forEach { Text(it, fontSize = 18.sp) }
        Button(onClick = { load() }) { Text("Otra persona") }
    }
}

@Composable
fun UniScreen() {
    var list by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var msg by remember { mutableStateOf("Escribe un país en inglés, ej: Dominican Republic") }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    Page {
        SearchRow("País (inglés)") { c ->
            scope.launch {
                msg = "Buscando..."; list = emptyList()
                try {
                    val a = JSONArray(httpGet("https://adamix.net/proxy.php?country=${enc(c).replace("%20", "+")}"))
                    list = (0 until a.length()).map {
                        val o = a.getJSONObject(it)
                        Triple(o.optString("name"), o.optJSONArray("domains")?.optString(0) ?: "",
                            o.optJSONArray("web_pages")?.optString(0) ?: "")
                    }
                    msg = if (list.isEmpty()) "Sin resultados" else "${list.size} universidades"
                } catch (e: Exception) { msg = "Error: ${e.message}" }
            }
        }
        Text(msg)
        list.forEach { (name, dom, web) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text("Dominio: $dom")
                    if (web.isNotEmpty()) TextButton(onClick = { uri.openUri(web) }) { Text(web) }
                }
            }
        }
    }
}

fun weatherDesc(c: Int) = when (c) {
    0 -> "Despejado"; 1, 2 -> "Parcialmente nublado"; 3 -> "Nublado"; 45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"; in 61..67 -> "Lluvia"; in 80..82 -> "Chubascos"; in 95..99 -> "Tormenta"; else -> "Variable"
}

@Composable
fun WeatherScreen() {
    var lines by remember { mutableStateOf(listOf("Cargando...")) }
    LaunchedEffect(Unit) {
        try {
            val cur = JSONObject(httpGet("https://api.open-meteo.com/v1/forecast?latitude=18.4861&longitude=-69.9312" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&timezone=America%2FSanto_Domingo")).getJSONObject("current")
            lines = listOf("Santo Domingo, RD", "${cur.getDouble("temperature_2m")} °C", weatherDesc(cur.getInt("weather_code")),
                "Humedad: ${cur.getInt("relative_humidity_2m")}%", "Viento: ${cur.getDouble("wind_speed_10m")} km/h",
                "Actualizado: ${cur.getString("time").replace("T", " ")}")
        } catch (e: Exception) { lines = listOf("Error: ${e.message}") }
    }
    Page {
        lines.forEachIndexed { i, t -> Text(t, fontSize = if (i == 1) 48.sp else 18.sp, fontWeight = if (i <= 1) FontWeight.Bold else null) }
    }
}

@Composable
fun PokemonScreen() {
    var name by remember { mutableStateOf("") }
    var img by remember { mutableStateOf("") }
    var exp by remember { mutableStateOf("") }
    var abil by remember { mutableStateOf("") }
    var cry by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    Page {
        SearchRow("Pokémon (ej: pikachu)") { n ->
            scope.launch {
                msg = "Buscando..."; img = ""
                try {
                    val o = JSONObject(httpGet("https://pokeapi.co/api/v2/pokemon/${enc(n).lowercase()}"))
                    name = o.getString("name").replaceFirstChar { it.uppercase() }
                    img = o.getJSONObject("sprites").optString("front_default")
                    exp = o.getInt("base_experience").toString()
                    val ab = o.getJSONArray("abilities")
                    abil = (0 until ab.length()).joinToString(", ") { ab.getJSONObject(it).getJSONObject("ability").getString("name") }
                    cry = o.optJSONObject("cries")?.optString("latest") ?: ""
                    msg = ""
                } catch (e: Exception) { msg = "No se encontró el Pokémon"; name = "" }
            }
        }
        Text(msg)
        if (name.isNotEmpty()) {
            AsyncImage(img, name, Modifier.size(200.dp))
            Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Experiencia base: $exp")
            Text("Habilidades: $abil")
            if (cry.isNotEmpty()) Button(onClick = {
                try { player.reset(); player.setDataSource(cry); player.setOnPreparedListener { it.start() }; player.prepareAsync() }
                catch (e: Exception) { msg = "No se pudo reproducir" }
            }) { Text("Reproducir sonido") }
        }
    }
}

data class Post(val title: String, val summary: String, val link: String)

@Composable
fun WpScreen() {
    var logo by remember { mutableStateOf("") }
    var siteName by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var msg by remember { mutableStateOf("Cargando...") }
    val uri = LocalUriHandler.current
    LaunchedEffect(Unit) {
        try {
            val site = JSONObject(httpGet("$WP_SITE/wp-json"))
            siteName = site.optString("name"); logo = site.optString("site_icon_url")
            val a = JSONArray(httpGet("$WP_SITE/wp-json/wp/v2/posts?per_page=3"))
            posts = (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Post(html(o.getJSONObject("title").getString("rendered")),
                    html(o.getJSONObject("excerpt").getString("rendered")), o.getString("link"))
            }
            msg = ""
        } catch (e: Exception) { msg = "Error: ${e.message}" }
    }
    Page {
        if (logo.isNotEmpty()) AsyncImage(logo, "Logo", Modifier.size(96.dp))
        Text(siteName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(msg)
        posts.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.title, fontWeight = FontWeight.Bold)
                    Text(p.summary)
                    TextButton(onClick = { uri.openUri(p.link) }) { Text("Visitar") }
                }
            }
        }
    }
}

@Composable
fun AboutScreen() = Page {
    Image(painterResource(R.drawable.foto), "Mi foto", Modifier.size(160.dp), contentScale = ContentScale.Crop)
    Text("Geremy Martinez", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Text("Técnico de soporte IT / Desarrollador Android")
    Text("Correo: geremymartinez13@gmail.com")
    Text("Teléfono: 829-891-1321")
    Text("Disponible para trabajos y proyectos")
}