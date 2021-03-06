package com.example.aop.part5.chapter01.data.entity

data class ToDoEntity(
	val id: Long = 0,
	val title: String,
	val description: String,
	val hasCompleted: Boolean = false
)