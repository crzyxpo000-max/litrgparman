package com.yukselis.protocol.ui.system

import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeMoment
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.system.Rarity

/**
 * Yalnızca hata ayıklama derlemesi: büyük anları oyunu oraya kadar oynamadan görmek için örnek bildirimler
 * (ekran görüntüsü ve görsel denetim; MainActivity `demo_moment` extra'sı). Oyun durumuna dokunmaz.
 */
object MomentDemo {
    fun notices(kind: String, lang: Lang): List<NoticeView> {
        val tr = lang == Lang.TR
        fun t(a: String, b: String) = if (tr) a else b
        val echo = NoticeView(NoticeKind.ECHO, t("bunu hissettin mi", "did you feel that"), listOf(t("biri daha bakıyor. sakin ol.", "someone else is watching. stay calm.")), null)
        return when (kind) {
            "level" -> listOf(
                NoticeView(
                    NoticeKind.LEVEL_UP, t("Seviye atladın!", "Level up!"),
                    listOf(t("Seviye 47 → 48", "Level 47 → 48"), t("+3 serbest puan", "+3 free points"), t("Can ve mana yenilendi.", "HP and MP restored.")),
                    null, from = 47, to = 48,
                ),
                NoticeView(NoticeKind.TITLE, t("Unvan: Gölgede Yürüyen", "Title: Shadow Walker"), listOf(t("Gizlilik +5%", "Stealth +5%")), Rarity.RARE),
            )
            "evolution" -> listOf(
                NoticeView(NoticeKind.EVOLUTION, t("Evrim tamamlandı: Gölge Bıçağı · Kademe C", "Evolution complete: Shadow Blade · Grade C"),
                    listOf(t("Bedenin eski sınırını geride bıraktı.", "Your body left its old ceiling behind.")), null, from = 2, to = 3),
                echo,
            )
            "unique" -> listOf(
                NoticeView(NoticeKind.SKILL, t("Yeni yetenek: Kırık Ayna", "New skill: Broken Mirror"),
                    listOf(t("Bu yetenek bu dünyada yalnızca bir kez var.", "This skill exists only once in this world."), t("Bir saldırıyı saldırgana geri yansıt.", "Reflect one attack back at its source.")), Rarity.UNIQUE),
            )
            "rank" -> listOf(
                NoticeView(NoticeKind.SYSTEM, t("[Küresel Sıralama: yeni yerin #37.]", "[Global Ranking: you're now #37.]"), emptyList(), null,
                    moment = NoticeMoment.RANK_UP, from = 214, to = 37),
            )
            "breach" -> listOf(
                NoticeView(NoticeKind.WARNING, t("Gerçek seviyen görüldü: 58", "Your true level is out: 58"),
                    listOf(t("Dikkat +59", "Attention +59")), null, moment = NoticeMoment.REVEAL, from = 31, to = 58),
                NoticeView(NoticeKind.ECHO, t("kimse bu sayıyı görmemeliydi", "no one was supposed to see that number"), listOf(t("şimdi sen gördün.", "now you have.")), null),
            )
            else -> emptyList()
        }
    }
}
