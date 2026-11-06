package com.yukselis.protocol.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yukselis.protocol.R

/** Arayüzdeki semboller ve terimler: her ekranın (i) düğmesi ve Daha fazla → Yardım aynı listeyi kullanır. */
object Glossary {
    @Composable
    fun story(): List<Pair<String, String>> = listOf(
        stringResource(R.string.help_req_term) to stringResource(R.string.help_req),
        stringResource(R.string.help_odds_term) to stringResource(R.string.help_odds),
        stringResource(R.string.help_kind_term) to stringResource(R.string.help_kind),
        stringResource(R.string.help_voices_term) to stringResource(R.string.help_voices),
        stringResource(R.string.help_earlier_term) to stringResource(R.string.help_earlier),
    )

    @Composable
    fun hub(): List<Pair<String, String>> = listOf(
        stringResource(R.string.help_actions_term) to stringResource(R.string.help_actions),
        stringResource(R.string.help_tension_term) to stringResource(R.string.help_tension),
        stringResource(R.string.help_danger_term) to stringResource(R.string.help_danger),
        stringResource(R.string.help_free_term) to stringResource(R.string.help_free),
        stringResource(R.string.help_gates_term) to stringResource(R.string.help_gates),
        stringResource(R.string.help_tower_term) to stringResource(R.string.help_tower),
    )

    @Composable
    fun combat(): List<Pair<String, String>> = listOf(
        stringResource(R.string.stance_all_out) to stringResource(R.string.stance_all_out_desc),
        stringResource(R.string.stance_measured) to stringResource(R.string.stance_measured_desc),
        stringResource(R.string.stance_defensive) to stringResource(R.string.stance_defensive_desc),
        stringResource(R.string.effort) to stringResource(R.string.effort_desc),
        stringResource(R.string.help_target_term) to stringResource(R.string.help_target),
        stringResource(R.string.help_actions_combat_term) to stringResource(R.string.help_actions_combat),
    )

    @Composable
    fun status(): List<Pair<String, String>> = listOf(
        stringResource(R.string.help_grade_term) to stringResource(R.string.help_grade),
        stringResource(R.string.help_public_term) to stringResource(R.string.help_public),
        stringResource(R.string.help_attention_term) to stringResource(R.string.help_attention),
        stringResource(R.string.help_corruption_term) to stringResource(R.string.help_corruption),
        stringResource(R.string.help_points_term) to stringResource(R.string.help_points),
        stringResource(R.string.help_morality_term) to stringResource(R.string.help_morality),
    )
}
