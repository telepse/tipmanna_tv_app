package com.example.telepse.ui.tv

/** Screen-level D-pad hooks when keys reach the activity before Compose focus moves. */
object TvFocusBridge {
    @Volatile
    var onDpadDown: (() -> Unit)? = null

    @Volatile
    var onDpadUp: (() -> Unit)? = null
}
