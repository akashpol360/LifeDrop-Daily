package com.lifedrop.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedrop.app.content.FeedItem
import com.lifedrop.app.content.Question
import com.lifedrop.app.data.AppVm

@Composable
fun FavoritesScreen(feed: List<FeedItem>, think: List<Question>, whatIf: List<Question>, vm: AppVm) {
    val favs by vm.favs.collectAsState()
    val items = feed.filter { it.id in favs }.map { Triple(it.id, it.type, it.text) } +
        think.filter { it.id in favs }.map { Triple(it.id, "🧠 " + it.cat, it.text) } +
        whatIf.filter { it.id in favs }.map { Triple(it.id, "🤔 What if?", it.text) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("♥ Favorites", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        if (items.isEmpty()) item { Text("Nothing here yet. Tap ♡ on any card to save it.") }
        items(items, key = { it.first }) { (id, title, text) ->
            LifeCard(title) {
                Text(text, fontSize = 17.sp)
                TextButton({ vm.toggleFav(id, true) }, Modifier.heightIn(min = 48.dp)) { Text("♥ Remove") }
            }
        }
    }
}
