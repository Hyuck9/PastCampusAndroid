package com.example.aop.part5.chapter01.viewmodel.todo

import com.example.aop.part5.chapter01.data.entity.ToDoEntity
import com.example.aop.part5.chapter01.domain.todo.InsertToDoItemUseCase
import com.example.aop.part5.chapter01.presentation.detail.DetailMode
import com.example.aop.part5.chapter01.presentation.detail.DetailViewModel
import com.example.aop.part5.chapter01.presentation.detail.ToDoDetailState
import com.example.aop.part5.chapter01.presentation.list.ListViewModel
import com.example.aop.part5.chapter01.presentation.list.ToDoListState
import com.example.aop.part5.chapter01.viewmodel.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.test.inject

/**
 * [DetailViewModel]을 테스트하기 위한 Unit Test class
 *
 * 1. initData()
 * 2. test viewModel fetch
 * 3. test delete todo
 * 4. test update todo
 *
 */
@ExperimentalCoroutinesApi
internal class DetailViewModelTest: ViewModelTest() {

	private val id = 1L

	private val detailViewModel by inject<DetailViewModel> { parametersOf(DetailMode.DETAIL, id) }
	private val listViewModel by inject<ListViewModel>()

	private val insertToDoItemItemUseCase: InsertToDoItemUseCase by inject()

	private val todo = ToDoEntity(
		id = id,
		title = "title $id",
		description = "description $id",
		hasCompleted = false
	)

	@Before
	fun init() {
		initData()
	}

	private fun initData() = runBlockingTest {
		insertToDoItemItemUseCase(todo)
	}

	@Test
	fun `test viewModel fetch`() = runBlockingTest {
		val testObservable = detailViewModel.toDoDetailLiveData.test()

		detailViewModel.fetchData()

		testObservable.assertValueSequence(
			listOf(
				ToDoDetailState.UnInitialized,
				ToDoDetailState.Loading,
				ToDoDetailState.Success(todo),
			)
		)
	}

	@Test
	fun `test delete todo`() = runBlockingTest {
		val detailTestObservable = detailViewModel.toDoDetailLiveData.test()
		detailViewModel.deleteToDo()

		detailTestObservable.assertValueSequence(
			listOf(
				ToDoDetailState.UnInitialized,
				ToDoDetailState.Loading,
				ToDoDetailState.Delete
			)
		)

		val listTestObservable = listViewModel.toDoListLiveData.test()
		listViewModel.fetchData()
		listTestObservable.assertValueSequence(
			listOf(
				ToDoListState.UnInitialized,
				ToDoListState.Loading,
				ToDoListState.Success(listOf()),
			)
		)
	}

	@Test
	fun `test update todo`() = runBlockingTest {
		val testObservable = detailViewModel.toDoDetailLiveData.test()

		val updateTitle = "title 1 update"
		val updateDescription = "description 1 update"

		val updateToDo = todo.copy(
			title = updateTitle,
			description = updateDescription
		)

		detailViewModel.writeToDo(
			title = updateTitle,
			description = updateDescription
		)

		testObservable.assertValueSequence(
			listOf(
				ToDoDetailState.UnInitialized,
				ToDoDetailState.Loading,
				ToDoDetailState.Success(updateToDo)
			)
		)
	}

}