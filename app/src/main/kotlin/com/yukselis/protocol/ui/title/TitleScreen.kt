package com.yukselis.protocol.ui.title

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.yukselis.protocol.ui.components.EchoLine
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SlotInfo
import com.yukselis.protocol.R
import com.yukselis.protocol.data.ThemeMode
import com.yukselis.protocol.ui.more.LanguagePicker
import com.yukselis.protocol.ui.more.ThemePicker
import com.yukselis.protocol.ui.theme.GameType

@Composable
fun TitleScreen(
    lang: Lang,
    autosave: SlotInfo?,
    onNewGame: (name: String, ironman: Boolean, newGamePlus: Boolean) -> Unit,
    newGamePlusUnlocked: Boolean = false,
    onContinue: () -> Unit,
    onLanguage: (Lang) -> Unit,
    theme: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    ready: Boolean = true,
) {
    val defaultName = stringResource(R.string.default_player_name)
    var name by rememberSaveable { mutableStateOf("") }
    var ironman by rememberSaveable { mutableStateOf(false) }
    var newGamePlus by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // Tablette başlık ekranı tek bir okunur sütun kalır.
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        // Logo: uygulama içi dile göre (cihaz dili değil). Işıltılı logo siyahtan saydamlaştırıldı;
        // başlık ekranı bu yüzden her temada koyu (App.kt, DECISIONS #150).
        Image(
            painter = painterResource(if (lang == Lang.TR) R.drawable.logo_tr else R.drawable.logo_en),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "[ ${stringResource(R.string.system_header).uppercase(LocalConfiguration.current.locales[0])} ] " + stringResource(R.string.system_online),
            style = GameType.systemTitle.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = GameType.narration,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(36.dp))
        if (autosave != null) {
            Button(onClick = onContinue, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.continue_game))
                    Text(
                        text = stringResource(R.string.continue_summary, autosave.playerName, autosave.level, autosave.day),
                        style = GameType.system,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Text(stringResource(R.string.new_game), style = GameType.systemTitle, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= MAX_NAME) name = it },
            label = { Text(stringResource(R.string.your_name)) },
            placeholder = { Text(defaultName) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ironman_mode), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.ironman_desc), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = ironman, onCheckedChange = { ironman = it })
        }
        if (newGamePlusUnlocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ng_plus), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.ng_plus_desc), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = newGamePlus, onCheckedChange = { newGamePlus = it })
            }
        }
        val start = { onNewGame(name.ifBlank { defaultName }, ironman, newGamePlus && newGamePlusUnlocked) }
        if (autosave != null) {
            OutlinedButton(onClick = start, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start)) }
        } else {
            Button(onClick = start, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start)) }
        }
        if (!ready) {
            Text(
                text = stringResource(R.string.loading_content),
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        // Yankı açılışta bir kez konuşur: kayıt varsa geri dönüşü, yoksa uyanışı fark eder.
        EchoLine(
            stringResource(if (autosave != null) R.string.title_echo_back else R.string.title_echo_new),
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(24.dp))
        LanguagePicker(lang, onLanguage)
        ThemePicker(theme, onTheme)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "v1.0",
            style = GameType.system,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val MAX_NAME = 20
