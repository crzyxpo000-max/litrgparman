package com.yukselis.protocol.ui.skills

import com.yukselis.protocol.ui.components.AppAlertDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.SkillView
import com.yukselis.engine.story.SkillsView
import com.yukselis.engine.system.SkillType
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.rarityColor

/** Yetenekler: sınıf başlığı ve yetenek listesi. Uzun basmak ayrıntı kartını açar. */
@Composable
fun SkillsScreen(view: SkillsView, modifier: Modifier = Modifier) {
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = view.className ?: stringResource(R.string.classless),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            view.classDescription?.let { Text(it, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            SectionTitle(stringResource(R.string.skills))
            Text(stringResource(R.string.long_press_hint), style = GameType.system, color = MaterialTheme.colorScheme.outline)
        }
        items(view.skills, key = { it.id }) { skill ->
            SkillRow(skill, onLongPress = { detail = skill.id })
        }
    }
    view.skills.firstOrNull { it.id == detail }?.let { SkillDetail(it) { detail = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillRow(skill: SkillView, onLongPress: () -> Unit) {
    val color = rarityColor(skill.rarity)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onLongPress, onLongClick = onLongPress),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(skill.name, style = GameType.systemTitle, color = color, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.skill_level, skill.level), style = GameType.system)
            }
            Text(
                text = listOfNotNull(
                    stringResource(if (skill.type == SkillType.ACTIVE) R.string.skill_active else R.string.skill_passive),
                    costText(skill),
                ).joinToString(" · "),
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (skill.xpToNext > 0) {
                StatBar("XP", skill.xp.toLong(), skill.xpToNext.toLong(), color, Modifier.padding(top = 6.dp))
            } else {
                Text(stringResource(R.string.skill_max), style = GameType.system, color = color)
            }
        }
    }
}

@Composable
private fun costText(skill: SkillView): String? {
    val parts = buildList {
        if (skill.costMp > 0) add("${skill.costMp} ${stringResource(R.string.mp)}")
        if (skill.costStamina > 0) add("${skill.costStamina} ${stringResource(R.string.stamina)}")
        if (skill.cooldown > 0) add(stringResource(R.string.cooldown_n, skill.cooldown))
    }
    return parts.joinToString(" · ").ifEmpty { null }
}

@Composable
private fun SkillDetail(skill: SkillView, onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(skill.name, color = rarityColor(skill.rarity)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(skill.description, style = GameType.narrationPast)
                Text(stringResource(R.string.rarity_line, skill.rarityName), style = GameType.system)
                costText(skill)?.let { Text(it, style = GameType.system) }
                if (skill.evolutions.isNotEmpty()) {
                    Text(stringResource(R.string.evolves_into, skill.evolutions.joinToString(" / ")), style = GameType.system, color = Color(0xFFFFA726))
                }
                skill.penalty?.let { Text(stringResource(R.string.penalty_line, it), style = GameType.system, color = Color(0xFFEF5350)) }
            }
        },
    )
}

