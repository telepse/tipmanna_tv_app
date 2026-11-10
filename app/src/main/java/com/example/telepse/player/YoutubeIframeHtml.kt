package com.example.telepse.player

/**
 * Builds HTML for YouTube IFrame Player API inside WebView.
 * Base URL must match [EMBED_ORIGIN] for Referer / embed identity (see architecture gate Doc 02).
 */
object YoutubeIframeHtml {
    const val EMBED_ORIGIN = "https://www.youtube-nocookie.com"
    const val IFRAME_API_SCRIPT = "https://www.youtube.com/iframe_api"

    fun build(initialVideoId: String, autoplay: Boolean, mute: Boolean, startSeconds: Float): String {
        val start = if (startSeconds > 0f) ", start: ${startSeconds.toInt()}" else ""
        val ap = if (autoplay) 1 else 0
        val mu = if (mute) 1 else 0
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <style>
                html, body { margin:0; padding:0; background:#000; height:100%; overflow:hidden; }
                #player { width:100%; height:100%; }
              </style>
            </head>
            <body>
              <div id="player"></div>
              <script>
                var player;
                function onYouTubeIframeAPIReady() {
                  TipmannaBridge.onIframeApiReady();
                  player = new YT.Player('player', {
                    height: '100%',
                    width: '100%',
                    videoId: '$initialVideoId',
                    host: 'https://www.youtube-nocookie.com',
                    playerVars: {
                      autoplay: $ap,
                      mute: $mu,
                      controls: 0,
                      modestbranding: 1,
                      rel: 0,
                      iv_load_policy: 3,
                      playsinline: 1,
                      disablekb: 1,
                      fs: 0
                      $start
                    },
                    events: {
                      onReady: function(e) {
                        TipmannaBridge.onPlayerReady();
                        TipmannaBridge.onVideoLoaded('$initialVideoId');
                      },
                      onStateChange: function(e) {
                        TipmannaBridge.onStateChange(e.data);
                      },
                      onError: function(e) {
                        TipmannaBridge.onError(e.data);
                      }
                    }
                  });
                }
                function loadVideoById(id, start) {
                  if (!player || !player.loadVideoById) return;
                  if (start > 0) player.loadVideoById(id, start);
                  else player.loadVideoById(id);
                }
                function cueVideoById(id, start) {
                  if (!player || !player.cueVideoById) return;
                  if (start > 0) player.cueVideoById(id, start);
                  else player.cueVideoById(id);
                }
                function playVideo() { if (player) player.playVideo(); }
                function pauseVideo() { if (player) player.pauseVideo(); }
                function seekTo(sec) { if (player) player.seekTo(sec, true); }
                function mutePlayer() { if (player) player.mute(); }
                function unMutePlayer() { if (player) player.unMute(); }
                function getCurrentTime() {
                  if (!player || !player.getCurrentTime) return 0;
                  return player.getCurrentTime();
                }
                function getDuration() {
                  if (!player || !player.getDuration) return 0;
                  return player.getDuration();
                }
                function getPlayerState() {
                  if (!player || !player.getPlayerState) return -1;
                  return player.getPlayerState();
                }
              </script>
              <script src="$IFRAME_API_SCRIPT"></script>
            </body>
            </html>
        """.trimIndent()
    }
}
