package com.example.aop.part5.chapter01.data.repository

import com.example.aop.part5.chapter01.data.entity.ToDoEntity

class TestToDoRepository: ToDoRepository {

	private val toDoList: MutableList<ToDoEntity> = mutableListOf()

	override suspend fun getToDoList(): List<ToDoEntity> {
		return toDoList
	}

	override suspend fun insertToDoItem(toDoItem: ToDoEntity) {
		this.toDoList.add(toDoItem)
	}

	override suspend fun insertToDoList(toDoList: List<ToDoEntity>) {
		this.toDoList.addAll(toDoList)
	}

	override suspend fun updateToDoItem(toDoItem: ToDoEntity): Boolean {
		val foundToDoEntity = toDoList.find { it.id == toDoItem.id }
		return if (foundToDoEntity == null) {
			false
		} else {
			this.toDoList[toDoList.indexOf(foundToDoEntity)] = toDoItem
			true
		}
	}

	override suspend fun getToDoItem(itemId: Long): ToDoEntity? {
		return toDoList.find { it.id == itemId }
	}

	override suspend fun deleteAll() {
		toDoList.clear()
	}

	override suspend fun deleteToDoItem(id: Long): Boolean {
		val foundTodoEntity = toDoList.find { it.id == id }
		return if (foundTodoEntity == null) {
			false
		} else {
			this.toDoList.removeAt(toDoList.indexOf(foundTodoEntity))
			true
		}
	}

}