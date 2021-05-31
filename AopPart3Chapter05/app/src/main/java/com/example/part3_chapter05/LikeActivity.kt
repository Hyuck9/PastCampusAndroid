package com.example.part3_chapter05

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.part3_chapter05.DBKey.Companion.DIS_LIKE
import com.example.part3_chapter05.DBKey.Companion.LIKE
import com.example.part3_chapter05.DBKey.Companion.LIKED_BY
import com.example.part3_chapter05.DBKey.Companion.MATCH
import com.example.part3_chapter05.DBKey.Companion.NAME
import com.example.part3_chapter05.DBKey.Companion.USERS
import com.example.part3_chapter05.DBKey.Companion.USER_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {

	private var auth: FirebaseAuth = FirebaseAuth.getInstance()
	private lateinit var userDB: DatabaseReference

	private val adapter = CardItemAdapter()
	private val cardItems = mutableListOf<CardItem>()

	private val manager by lazy {
		CardStackLayoutManager(this, this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_like)

		userDB = Firebase.database.reference.child(USERS)

		val currentUserDB = userDB.child(getCurrentUserID())
		currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (snapshot.child(NAME).value == null) {
					showNameInputPopup()
					return
				}
				// 유저정보 갱신
				getUnSelectedUsers()
			}
			override fun onCancelled(error: DatabaseError) {}
		})

		initCardStackView()
		initSignOutButton()
		initMatchedListButton()
	}

	private fun initCardStackView() {
		val stackView = findViewById<CardStackView>(R.id.cardStackView)

		stackView.layoutManager = manager
		stackView.adapter = adapter
	}

	private fun initSignOutButton() {
		val signOutButton = findViewById<Button>(R.id.signOutButton)
		signOutButton.setOnClickListener {
			auth.signOut()
			startActivity(Intent(this, MainActivity::class.java))
			finish()
		}
	}

	private fun initMatchedListButton() {
		val matchListButton = findViewById<Button>(R.id.matchListButton)
		matchListButton.setOnClickListener {
			startActivity(Intent(this, MatchedUserActivity::class.java))
		}
	}

	private fun getUnSelectedUsers() {
		userDB.addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
				if (snapshot.child(USER_ID).value != getCurrentUserID()
					&& snapshot.child(LIKED_BY).child(LIKE).hasChild(getCurrentUserID()).not()
					&& snapshot.child(LIKED_BY).child(DIS_LIKE).hasChild(getCurrentUserID()).not()) {

					val userId = snapshot.child(USER_ID).value.toString()
					var name = "undecided"
					if (snapshot.child(NAME).value != null) {
						name = snapshot.child(NAME).value.toString()
					}

					cardItems.add(CardItem(userId, name))
					adapter.submitList(cardItems)
					adapter.notifyDataSetChanged()
				}
			}
			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				cardItems.find { it.userId == snapshot.key }?.let {
					it.name = snapshot.child(NAME).value.toString()
				}

				adapter.submitList(cardItems)
				adapter.notifyDataSetChanged()
			}
			override fun onChildRemoved(snapshot: DataSnapshot) {}
			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
			override fun onCancelled(error: DatabaseError) {}

		})
	}

	private fun showNameInputPopup() {
		val editText = EditText(this)

		AlertDialog.Builder(this)
			.setTitle("이름을 입력해주세요")
			.setView(editText)
			.setPositiveButton("저장") { _, _ ->
				if (editText.text.isEmpty()) {
					showNameInputPopup()
				} else {
					saveUserName(editText.text.toString())
				}
			}
			.setCancelable(false)
			.show()
	}

	private fun saveUserName(name: String) {

		val userId = auth.currentUser?.uid.orEmpty()
		val currentUserDB = userDB.child(userId)
		val user = mutableMapOf<String, Any>()
		user[USER_ID] = userId
		user[NAME] = name
		currentUserDB.updateChildren(user)

		getUnSelectedUsers()

	}

	private fun getCurrentUserID(): String {
		if (auth.currentUser == null) {
			Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
			finish()
		}

		return auth.currentUser?.uid.orEmpty()
	}

	private fun like() {
		val card = cardItems[manager.topPosition - 1]
		cardItems.removeFirst()

		userDB.child(card.userId)
			.child(LIKED_BY)
			.child(LIKE)
			.child(getCurrentUserID())
			.setValue(true)

		// 매칭이 된 시점을 봐야한다.
		saveMatchIfOtherUserLikedMe(card.userId)

		Toast.makeText(this, "${card.name}님을 Like 하셨습니다.", Toast.LENGTH_SHORT).show()
	}

	private fun disLike() {
		val card = cardItems[manager.topPosition - 1]
		cardItems.removeFirst()

		userDB.child(card.userId)
			.child(LIKED_BY)
			.child(DIS_LIKE)
			.child(getCurrentUserID())
			.setValue(true)

		Toast.makeText(this, "${card.name}님을 disLike 하셨습니다.", Toast.LENGTH_SHORT).show()
	}

	private fun saveMatchIfOtherUserLikedMe(otherUserId: String) {
		val otherUserDB = userDB.child(getCurrentUserID()).child(LIKED_BY).child(LIKE).child(otherUserId)
		otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (snapshot.value == true) {
					userDB.child(getCurrentUserID())
						.child(LIKED_BY)
						.child(MATCH)
						.child(otherUserId)
						.setValue(true)

					userDB.child(otherUserId)
						.child(LIKED_BY)
						.child(MATCH)
						.child(getCurrentUserID())
						.setValue(true)
				}
			}
			override fun onCancelled(error: DatabaseError) {}

		})
	}

	override fun onCardDragging(direction: Direction?, ratio: Float) {}
	override fun onCardSwiped(direction: Direction?) {
		when (direction) {
			Direction.Right -> like()
			Direction.Left -> disLike()
			else -> {}
		}
	}
	override fun onCardRewound() {}
	override fun onCardCanceled() {}
	override fun onCardAppeared(view: View?, position: Int) {}
	override fun onCardDisappeared(view: View?, position: Int) {}

}