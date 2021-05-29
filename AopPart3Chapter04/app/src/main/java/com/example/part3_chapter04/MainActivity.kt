package com.example.part3_chapter04

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.part3_chapter04.adapter.BookAdapter
import com.example.part3_chapter04.api.BookService
import com.example.part3_chapter04.databinding.ActivityMainBinding
import com.example.part3_chapter04.model.BestSellerDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding
	private lateinit var adapter: BookAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		initBookRecyclerView()

		val retrofit = Retrofit.Builder()
			.baseUrl("https://book.interpark.com")
			.addConverterFactory(GsonConverterFactory.create())
			.build()

		val bookService = retrofit.create(BookService::class.java)

		bookService.getBestSellerBooks("D5DB4760E780540FA32AE026036C98987EDEDACB0D25A4AB47E92F033A3870E3")
			.enqueue(object : Callback<BestSellerDto> {
				override fun onResponse(
					call: Call<BestSellerDto>,
					response: Response<BestSellerDto>
				) {
					// TODO: 성공처리
					if (response.isSuccessful.not()) {
						Log.e(TAG, "NOT!! SUCCESS")
						return
					}
					response.body()?.let {
						Log.d(TAG, it.toString())

						it.books.forEach { book ->
							Log.d(TAG, book.toString())
						}

						adapter.submitList(it.books)
					}
				}

				override fun onFailure(call: Call<BestSellerDto>, t: Throwable) {
					// TODO: 실패처리
					Log.e(TAG, t.toString())
				}

			})
	}

	fun initBookRecyclerView() {
		adapter = BookAdapter()
		binding.bookRecyclerView.layoutManager = LinearLayoutManager(this)
		binding.bookRecyclerView.adapter = adapter
	}

	companion object {
		private const val TAG = "MainActivity"
	}
}