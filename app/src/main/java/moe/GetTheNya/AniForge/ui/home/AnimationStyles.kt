package moe.GetTheNya.AniForge.ui.home

enum class TitleAnimStyle(val labelKey: String) {
    NONE("animNone"),
    DECODING("animTitleDecoding"),
    SLIDE_SIDE("animTitleSlideSide"),
    TURNSTILE_3D("animTitleTurnstile3d"),
    GLITCH("animTitleGlitch")
}

enum class SubtitleAnimStyle(val labelKey: String) {
    NONE("animNone"),
    BLUR_FADE("animSubtitleBlurFade"),
    WORD_BY_WORD("animSubtitleWordByWord"),
    TYPEWRITER("animSubtitleTypewriter")
}

enum class ContentAnimStyle(val labelKey: String) {
    NONE("animNone"),
    POWER_UP("animContentPowerUp"),
    SLIDE_UP("animContentSlideUp"),
    FLIP_3D("animContentFlip3d")
}
