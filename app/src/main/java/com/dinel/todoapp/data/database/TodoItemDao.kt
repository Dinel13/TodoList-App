package com.dinel.todoapp.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.todolist.data.database.TodoItem


@Dao
interface TodoItemDao {

    @Insert
    suspend fun saveTodoItem(todoItem: TodoItem)

    @Insert
    suspend fun saveTodoItems(todoItems: List<TodoItem>)

    @Delete
    suspend fun deleteTodoItem(todoItem: TodoItem)

    @Update
    suspend fun updateTodoItem(todoItem: TodoItem)

    @Query("SELECT * FROM todo ORDER BY id DESC")
    fun getAllTodoList(): LiveData<MutableList<TodoItem>>

}