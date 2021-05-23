package ch.ralena.natibo.ui.study.insession

import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.ralena.natibo.R
import ch.ralena.natibo.data.room.`object`.Course
import ch.ralena.natibo.data.room.`object`.Day
import ch.ralena.natibo.data.room.`object`.SentenceGroup
import ch.ralena.natibo.databinding.FragmentCoursePickLanguagesBinding
import ch.ralena.natibo.databinding.FragmentStudySessionBinding
import ch.ralena.natibo.di.component.PresentationComponent
import ch.ralena.natibo.service.StudySessionService
import ch.ralena.natibo.ui.MainActivity
import ch.ralena.natibo.ui.adapter.SentenceGroupAdapter
import ch.ralena.natibo.ui.base.BaseFragment
import ch.ralena.natibo.ui.study.overview.StudySessionOverviewFragment
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.realm.Realm
import java.util.*
import javax.inject.Inject

class StudySessionFragment :
	BaseFragment<FragmentStudySessionBinding, StudySessionViewModel.Listener, StudySessionViewModel>(
		FragmentStudySessionBinding::inflate
	) {
	companion object {
		val TAG = StudySessionFragment::class.java.simpleName
		const val KEY_COURSE_ID = "language_id"
		private const val KEY_IS_PAUSED = "key_is_paused"
	}

	@Inject
	lateinit var activity: MainActivity

	lateinit var course: Course
	private lateinit var realm: Realm

	// fields
	private val prefs: SharedPreferences? = null
	private lateinit var studySessionService: StudySessionService
	private var millisLeft: Long = 0
	private lateinit var countDownTimer: CountDownTimer
	private var isPaused = false
	var adapter: SentenceGroupAdapter? = null

	// views
	private lateinit var sentencesLayout: LinearLayout
	var serviceDisposable: Disposable? = null
	var sentenceDisposable: Disposable? = null
	var finishDisposable: Disposable? = null

	override fun setupViews(view: View) {
		// load schedules from database
		val id = arguments!!.getString(KEY_COURSE_ID)
		realm = Realm.getDefaultInstance()
		course = realm.where(Course::class.java).equalTo("id", id).findFirst()!!
		activity.startSession(course)

		// set up recycler view
		binding.recyclerView.apply {
			adapter = SentenceGroupAdapter()
			layoutManager = LinearLayoutManager(context)
		}

		// handle playing/pausing
		binding.playPauseImage.setOnClickListener { v: View -> playPause(v) }

		// connect to the studySessionService and start the session
		connectToService()

		// if the current day is done, start the next one
		if (course.currentDay == null || course.currentDay.isCompleted)
			course.prepareNextDay(realm)

		// load all the views
		loadGlobalViews(view)

		activity.startSession(course)
	}

	override fun injectDependencies(injector: PresentationComponent) {
		injector.inject(this)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		isPaused = savedInstanceState?.getBoolean(KEY_IS_PAUSED, true) ?: true
		return super.onCreateView(inflater, container, savedInstanceState)
	}

	private fun loadGlobalViews(view: View) {
		// hide sentences layout until a sentence has been loaded
		binding.sentencesLayout.visibility = View.VISIBLE

		// load course title
		binding.courseTitleText.text = course.title

		binding.settingsIcon.setOnClickListener {
			Toast.makeText(
				activity,
				R.string.course_settings_not_implemented,
				Toast.LENGTH_SHORT
			).show()
		}
	}

	private fun connectToService() {
		serviceDisposable =
			activity.sessionPublish.subscribe({ service: StudySessionService ->
				studySessionService = service
				if (course.getCurrentDay().getCurrentSentenceGroup() != null) nextSentence(
					course.getCurrentDay().getCurrentSentenceGroup()
				) else sessionFinished(course.getCurrentDay())
				sentenceDisposable = studySessionService.sentenceObservable()
					.subscribe({ sentenceGroup: SentenceGroup ->
						nextSentence(sentenceGroup)
					})
				finishDisposable = studySessionService.finishObservable()
					.subscribe(Consumer<Day> { day: Day -> sessionFinished(day) })
				setPaused(studySessionService.getPlaybackStatus() == null || studySessionService.getPlaybackStatus() == StudySessionService.PlaybackStatus.PAUSED)
				if (!isPaused) {
					startTimer()
				}
				updateTime()
				updatePlayPauseImage()
			})
	}

	private fun playPause(view: View) {
		if (studySessionService != null) {
			if (studySessionService.getPlaybackStatus() == StudySessionService.PlaybackStatus.PLAYING) {
				studySessionService.pause()
				setPaused(true)
				if (countDownTimer != null) countDownTimer.cancel()
			} else {
				studySessionService.resume()
				startTimer()
			}
			updatePlayPauseImage()
		}
	}

	private fun updatePlayPauseImage() {
		if (studySessionService.playbackStatus == StudySessionService.PlaybackStatus.PLAYING) {
			binding.playPauseImage.setImageResource(R.drawable.ic_pause)
		} else {
			binding.playPauseImage.setImageResource(R.drawable.ic_play)
		}
	}

	private fun sessionFinished(day: Day) {
		// mark day as completed
		realm.executeTransaction { r: Realm? ->
			course.addReps(course.getCurrentDay().getTotalReviews())
			day.setCompleted(true)
		}
		val fragment = StudySessionOverviewFragment()
		val bundle = Bundle()
		bundle.putString(StudySessionOverviewFragment.KEY_COURSE_ID, course.getId())
		fragment.setArguments(bundle)
		fragmentManager!!.beginTransaction()
			.replace(R.id.fragmentPlaceHolder, fragment)
			.commit()
	}

	private fun nextSentence(sentenceGroup: SentenceGroup) {
		updatePlayPauseImage()
		sentencesLayout.setVisibility(View.VISIBLE)
		adapter?.updateSentenceGroup(sentenceGroup)

		// update number of reps remaining
		binding.remainingRepsText.setText(
			String.format(
				Locale.getDefault(),
				"%d",
				course.getCurrentDay().getNumReviewsLeft()
			)
		)
		binding.totalRepsText.setText(
			String.format(
				Locale.getDefault(),
				"%d",
				course.getTotalReps()
			)
		)

		// update time left
		millisLeft = course.getCurrentDay().getTimeLeft().toLong()
	}

	private fun startTimer() {
		millisLeft = millisLeft - millisLeft % 1000 - 1
		updateTime()
		// Make sure no two active timers are displayed at the same time
		if (countDownTimer != null) {
			countDownTimer.cancel()
		}
		countDownTimer = object : CountDownTimer(millisLeft, 100) {
			override fun onTick(millisUntilFinished: Long) {
				if (!isPaused) {
					millisLeft = millisUntilFinished
					updateTime()
				}
			}

			override fun onFinish() {
				Log.d(TAG, "timer finished: $this")
			}
		}
		setPaused(false)
		countDownTimer.start()
	}

	private fun setPaused(isPaused: Boolean) {
		this.isPaused = isPaused
	}

	private fun updateTime() {
		val secondsLeft = (millisLeft / 1000).toInt()
		binding.remainingTimeText.text = String.format(
			Locale.US,
			"%d:%02d",
			secondsLeft / 60,
			secondsLeft % 60
		)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(KEY_IS_PAUSED, isPaused)
	}

	override fun onPause() {
		super.onPause()
		if (serviceDisposable != null) serviceDisposable!!.dispose()
		if (sentenceDisposable != null) sentenceDisposable!!.dispose()
		if (finishDisposable != null) finishDisposable!!.dispose()
		if (countDownTimer != null) countDownTimer.cancel()
	}

	override fun onResume() {
		super.onResume()
		connectToService()
	}

	override fun onDestroy() {
		super.onDestroy()
		if (countDownTimer != null) countDownTimer.cancel()
	}
}