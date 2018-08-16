package org.nypl.audiobook.demo.android.with_fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventChapterCompleted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventChapterWaiting
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackBuffering
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackPaused
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackProgressUpdate
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackStarted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackStopped
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerType
import org.slf4j.LoggerFactory
import rx.Subscription

class PlayerTOCFragment : DialogFragment() {

  private val log = LoggerFactory.getLogger(PlayerTOCFragment::class.java)
  private lateinit var listener: PlayerFragmentListenerType
  private lateinit var adapter: PlayerTOCAdapter
  private lateinit var book: PlayerAudioBookType
  private lateinit var player: PlayerType
  private var bookSubscription: Subscription? = null
  private var playerSubscription: Subscription? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {

    val view: RecyclerView =
      inflater.inflate(R.layout.player_toc_view, container, false) as RecyclerView

    view.layoutManager = LinearLayoutManager(view.context)
    view.setHasFixedSize(true)
    view.adapter = this.adapter

    this.dialog.setTitle("Table Of Contents")
    return view
  }

  override fun onDestroy() {
    super.onDestroy()

    this.bookSubscription?.unsubscribe()
    this.playerSubscription?.unsubscribe()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is PlayerFragmentListenerType) {
      this.listener = context

      this.book = this.listener.onPlayerTOCWantsBook()
      this.player = this.listener.onPlayerWantsPlayer()

      this.adapter =
        PlayerTOCAdapter(
          context = context,
          spineElements = this.book.spine,
          interactionListener = this.listener)

      this.bookSubscription =
        this.book.spineElementDownloadStatus.subscribe(
          { status -> this.onSpineElementStatusChanged(status) },
          { error -> this.onSpineElementStatusError(error) },
          { })

      this.playerSubscription =
        this.player.events.subscribe(
          { event -> this.onPlayerEvent(event) },
          { error -> this.onPlayerError(error) },
          { })

    } else {
      throw ClassCastException(
        StringBuilder(64)
          .append("The activity hosting this fragment must implement one or more listener interfaces.\n")
          .append("  Activity: ")
          .append(context::class.java.canonicalName)
          .append('\n')
          .append("  Required interface: ")
          .append(PlayerFragmentListenerType::class.java.canonicalName)
          .append('\n')
          .toString())
    }
  }

  private fun onPlayerError(error: Throwable) {
    this.log.error("onPlayerError: ", error)
  }

  private fun onPlayerEvent(event: PlayerEvent) {
    this.onPlayerSpineElement(event.spineElement.index)
  }

  private fun onPlayerSpineElement(index: Int) {
    UIThread.runOnUIThread(Runnable {
      this.adapter.setCurrentSpineElement(index)
    })
  }

  private fun onSpineElementStatusError(error: Throwable?) {
    this.log.error("onSpineElementStatusError: ", error)
  }

  private fun onSpineElementStatusChanged(status: PlayerSpineElementDownloadStatus) {
    UIThread.runOnUIThread(Runnable {
      val spineElement = status.spineElement
      this.adapter.notifyItemChanged(spineElement.index)
    })
  }

  companion object {
    @JvmStatic
    fun newInstance() = PlayerTOCFragment()
  }
}
