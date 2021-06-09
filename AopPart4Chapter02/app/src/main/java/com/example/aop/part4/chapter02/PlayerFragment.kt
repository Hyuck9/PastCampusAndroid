package com.example.aop.part4.chapter02

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.aop.part4.chapter02.databinding.FragmentPlayerBinding
import com.example.aop.part4.chapter02.service.MusicDto
import com.example.aop.part4.chapter02.service.MusicService
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PlayerFragment : Fragment(R.layout.fragment_player) {

	private lateinit var binding: FragmentPlayerBinding
	private var model: PlayerModel = PlayerModel()
	private var player: SimpleExoPlayer? = null
	private lateinit var playListAdapter: PlayListAdapter

	private val updateSeekRunnable = Runnable { updateSeek() }

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding = FragmentPlayerBinding.bind(view)

		initPlayView()
		initPlayListButton()
		initPlayControlButtons()
		initSeekBar()
		initRecyclerView()

		getVideoListFromServer()
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun initSeekBar() {
		binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar) {
				player?.seekTo((seekBar.progress * 1000).toLong())
			}
		})
		binding.playListSeekBar.setOnTouchListener { _, _ -> false }
	}

	private fun initPlayControlButtons() {
		binding.playControlImageView.setOnClickListener {
			val player = this.player ?: return@setOnClickListener

			if (player.isPlaying) {
				player.pause()
			} else {
				player.play()
			}
		}
		binding.skipNextImageView.setOnClickListener {
			val nextMusic = model.nextMusic() ?: return@setOnClickListener
			playMusic(nextMusic)
		}
		binding.skipPrevImageView.setOnClickListener {
			val prevMusic = model.prevMusic() ?: return@setOnClickListener
			playMusic(prevMusic)
		}
	}

	private fun initPlayView() {
		context?.let {
			player = SimpleExoPlayer.Builder(it).build()
		}
		binding.apply {
			playerView.player = player
			player?.addListener(object : Player.EventListener {
				override fun onIsPlayingChanged(isPlaying: Boolean) {
					super.onIsPlayingChanged(isPlaying)

					if (isPlaying) {
						playControlImageView.setImageResource(R.drawable.ic_baseline_pause_48)
					} else {
						playControlImageView.setImageResource(R.drawable.ic_baseline_play_arrow_48)
					}
				}

				override fun onPlaybackStateChanged(state: Int) {
					super.onPlaybackStateChanged(state)

					updateSeek()
				}

				override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
					super.onMediaItemTransition(mediaItem, reason)

					val newIndex = mediaItem?.mediaId ?: return
					model.currentPosition = newIndex.toInt()
					updatePlayerView(model.currentMusicModel())
					playListAdapter.submitList(model.getAdapterModels())
				}
			})
		}
	}

	private fun updateSeek() {
		val player = this.player ?: return
		val duration = if (player.duration >= 0) player.duration else 0
		val position = player.currentPosition

		updateSeekUi(duration, position)

		val state = player.playbackState

		view?.removeCallbacks(updateSeekRunnable)
		if (state != Player.STATE_IDLE && state != Player.STATE_ENDED) {
			view?.postDelayed(updateSeekRunnable, 1000)
		}

	}

	private fun updateSeekUi(duration: Long, position: Long) {
		binding.playListSeekBar.max = (duration / 1000).toInt()
		binding.playListSeekBar.progress = (position / 1000).toInt()

		binding.playerSeekBar.max = (duration / 1000).toInt()
		binding.playerSeekBar.progress = (position / 1000).toInt()

		binding.playTimeTextView.text = String.format(
			"%02d:%02d",
			TimeUnit.MINUTES.convert(position, TimeUnit.MILLISECONDS),
			(position / 1000) % 60
		)
		binding.totalTimeTextView.text = String.format(
			"%02d:%02d",
			TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS),
			(duration / 1000) % 60
		)
	}

	private fun updatePlayerView(currentMusicModel: MusicModel?) {
		currentMusicModel ?: return

		binding.trackTextView.text = currentMusicModel.track
		binding.artistTextView.text = currentMusicModel.artist
		Glide.with(binding.coverImageView.context)
			.load(currentMusicModel.coverUrl)
			.into(binding.coverImageView)
	}

	private fun initRecyclerView() {
		playListAdapter = PlayListAdapter {
			playMusic(it)
		}

		binding.playListRecyclerView.apply {
			adapter = playListAdapter
			layoutManager = LinearLayoutManager(context)
		}
	}

	private fun initPlayListButton() {
		binding.playlistImageView.setOnClickListener {
			if (model.currentPosition == -1) return@setOnClickListener

			binding.playerViewGroup.isVisible = model.isWatchingPlayListView
			binding.playListViewGroup.isVisible = model.isWatchingPlayListView.not()

			model.isWatchingPlayListView = !model.isWatchingPlayListView
		}
	}

	private fun getVideoListFromServer() {
		val retrofit = Retrofit.Builder()
			.baseUrl("https://run.mocky.io")
			.addConverterFactory(GsonConverterFactory.create())
			.build()

		retrofit.create(MusicService::class.java).also {
			it.listMusics()
				.enqueue(object : Callback<MusicDto> {
					override fun onResponse(call: Call<MusicDto>, response: Response<MusicDto>) {
						Log.d("PlayerFragment", "${response.body()}")

						response.body()?.let { musicDto ->

							model = musicDto.mapper()

							setMusicList(model.getAdapterModels())
							playListAdapter.submitList(model.getAdapterModels())

						}
					}

					override fun onFailure(call: Call<MusicDto>, t: Throwable) {
					}
				})
		}
	}

	private fun setMusicList(modelList: List<MusicModel>) {
		context?.let {
			player?.addMediaItems(modelList.map { musicModel ->
				MediaItem.Builder()
					.setMediaId(musicModel.id.toString())
					.setUri(musicModel.streamUrl)
					.build()
			})

			player?.prepare()
		}
	}

	private fun playMusic(musicModel: MusicModel) {
		model.updateCurrentPosition(musicModel)
		player?.seekTo(model.currentPosition, 0)
		player?.play()
	}

	override fun onStop() {
		super.onStop()
		player?.pause()
		view?.removeCallbacks(updateSeekRunnable)
	}

	override fun onDestroy() {
		super.onDestroy()
		player?.release()
		view?.removeCallbacks(updateSeekRunnable)
	}

	companion object {
		fun newInstance(): PlayerFragment = PlayerFragment()
	}

}