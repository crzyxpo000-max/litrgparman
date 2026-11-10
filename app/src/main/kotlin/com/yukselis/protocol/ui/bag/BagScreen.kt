package com.yukselis.protocol.ui.bag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.BagView
import com.yukselis.engine.story.ItemView
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.RecipeView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.components.SystemCard
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.rarityColorOf

/** Çanta: Öz Kristali, envanter (kullanılabilir eşyalar). Meslek/üretim kapalı (Paket 4, DECISIONS #166); eski kayıtta meslek varsa motor zaten vermez. */
@Composable
fun BagScreen(
    bag: BagView,
    notices: List<NoticeView>,
    onUse: (String) -> Unit,
    onCraft: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(notices.size) { SystemCard(notices[it]) }
        item {
            Text(
                text = stringResource(R.string.currency_crystals, bag.crystals),
                style = GameType.systemTitle,
                color = Game.colors.amber,
            )
            SectionTitle(stringResource(R.string.inventory))
            if (bag.items.isEmpty()) Text(stringResource(R.string.inventory_empty), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(bag.items, key = { "i-${it.id}" }) { ItemRow(it) { onUse(it.id) } }

        // Meslek bölümü yalnız motor bir meslek adı veriyorsa (sistem kapalıyken hiç görünmez).
        val professionName = bag.professionName
        if (professionName != null) {
            item {
                SectionTitle(stringResource(R.string.profession))
                Text(stringResource(R.string.profession_level, professionName, bag.professionLevel), style = GameType.systemTitle)
                StatBar("XP", bag.professionXp.toLong(), bag.professionXpToNext.toLong(), Game.colors.success, Modifier.padding(top = 4.dp, bottom = 4.dp))
            }
            items(bag.recipes, key = { "r-${it.id}" }) { RecipeRow(it, bag.professionLevel) { onCraft(it.id) } }
        }
    }
}

@Composable
private fun ItemRow(item: ItemView, onUse: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${item.name} ×${item.count}", style = GameType.systemTitle, color = rarityColorOf(item.rarity))
                Text(item.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.usable) OutlinedButton(onClick = onUse) { Text(stringResource(R.string.use)) }
        }
    }
}

@Composable
private fun RecipeRow(recipe: RecipeView, level: Int, onCraft: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${recipe.name} ×${recipe.count}", style = GameType.systemTitle)
                Text(
                    recipe.ingredients.joinToString(", ") { "${it.name} ${it.have}/${it.need}" },
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (level < recipe.minLevel) {
                    Text(stringResource(R.string.requires_level, recipe.minLevel), style = GameType.system, color = Game.colors.danger)
                }
            }
            Button(onClick = onCraft, enabled = recipe.craftable) { Text(stringResource(R.string.craft)) }
        }
    }
}
