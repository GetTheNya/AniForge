package moe.GetTheNya.AniForge.ui.utils

import android.content.Context
import android.content.Intent
import moe.GetTheNya.AniForge.core.model.Anime

fun shareAnime(context: Context, anime: Anime, title: String = anime.titleRomaji) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out $title on AniForge: https://aniforge.pages.dev/anime?id=${anime.anilistId}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
