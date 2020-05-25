package com.dinel.todoapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.dinel.todoapp.R
import com.dinel.todoapp.adapters.TodoListAdapter
import com.dinel.todoapp.data.database.TodoItem
import com.dinel.todoapp.notification.NotificationUtils
import com.dinel.todoapp.utilities.Constants
import com.dinel.todoapp.utilities.convertMillis
import com.dinel.todoapp.utilities.convertNumberToMonthName
import com.dinel.todoapp.viewmodel.TodoViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.todo_item_display_details_dialog.*
import kotlinx.android.synthetic.main.todo_list.*

class MainActivity : AppCompatActivity(), TodoListAdapter.TodoItemClickListener {

    private lateinit var todoViewModel: TodoViewModel
    private lateinit var searchView: SearchView
    private lateinit var todoAdapter: TodoListAdapter

    private var dialog: Dialog? = null
    private var countDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv_todo_list.layoutManager = LinearLayoutManager(this)
        todoAdapter = TodoListAdapter(this)
        rv_todo_list.adapter = todoAdapter

        todoViewModel = ViewModelProviders.of(this).get(TodoViewModel::class.java)
        todoViewModel.getAllTodoItemList().observe(this, Observer { it ->

            val itemsWithNoDeadline = mutableListOf<TodoItem>()
            val completedItems = mutableListOf<TodoItem>()

            for (item in it) {
                if (item.dueTime!!.toInt() == 0 && !item.completed) {
                    itemsWithNoDeadline.add(item)
                } else if (item.completed) {
                    completedItems.add(item)
                }
            }

            for (item in itemsWithNoDeadline) {
                it.remove(item)
            }

            for (item in completedItems) {
                it.remove(item)
            }

            it.sortBy { it.dueTime }

            it.addAll(itemsWithNoDeadline)
            it.addAll(completedItems)

            todoAdapter.setTodoItems(it)

            if (it.size == 0) {
                displayEmptyTaskListImage()
            }
        })


    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        countDialog?.dismiss()
    }




    private fun createMockTodoItems(count: Int) {
        val todoList = mutableListOf<TodoItem>()

        for (index in 0..count) {
            todoList.add(
                TodoItem(
                    null,
                    "Nama $index",
                    "Catatan $index",
                    0,
                    false
                )
            )
        }

        todoViewModel.saveTodoItems(todoList)
    }

    private fun clearSearchView() {
        if (!searchView.isIconified) {
            searchView.isIconified = true
            return
        }
    }

    override fun onDeleteClicked(todoItem: TodoItem) {
        todoViewModel.deleteTodoItem(todoItem)

        NotificationUtils().cancelNotification(todoItem, this)
    }

    override fun onItemClicked(todoItem: TodoItem) {
        clearSearchView()

        // display the details of the item in a dialog.
        displayEventDetails(todoItem)

    }

    override fun onCheckClicked(todoItem: TodoItem) {
         if (!todoItem.completed) {
            NotificationUtils().cancelNotification(todoItem, this)
        } else if (todoItem.completed && todoItem.dueTime!! > 0 && System.currentTimeMillis() < todoItem.dueTime) {
            NotificationUtils().setNotification(todoItem, this)
        }

        todoViewModel.toggleCompleteState(todoItem)
    }

    private fun displayEventDetails(todoItem: TodoItem) {
        dialog = Dialog(this)
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setCancelable(true)
        dialog!!.setContentView(R.layout.todo_item_display_details_dialog)

        dialog!!.tv_todo_title_content.text = todoItem.title
        dialog!!.tv_todo_description_content.text = todoItem.note

        if (todoItem.dueTime!!.toInt() == 0) {
            dialog!!.tv_todo_due_content.text = getString(R.string.no_due_is_set)
        } else {
            val dateValues = convertMillis(todoItem.dueTime)
            val displayFormat: String
            if (dateValues[4] < 10) {
                displayFormat = String
                    .format(
                        getString(R.string.due_date_minute_less_than_ten),
                        convertNumberToMonthName(dateValues[1]),
                        dateValues[0],
                        dateValues[2],
                        dateValues[3],
                        dateValues[4]
                    )
            } else {
                displayFormat = String
                    .format(
                        getString(R.string.due_date_minute_greater_than_ten),
                        convertNumberToMonthName(dateValues[1]),
                        dateValues[0],
                        dateValues[2],
                        dateValues[3],
                        dateValues[4]
                    )
            }

            dialog!!.tv_todo_due_content.text = displayFormat
        }

        if (todoItem.completed) {
            dialog!!.button_complete_todo_item.text = getString(R.string.mark_as_incomplete)
        } else {
            dialog!!.button_complete_todo_item.text = getString(R.string.mark_as_complete)
        }
        dialog!!.button_complete_todo_item.setOnClickListener {
            if (!todoItem.completed) {
                dialog!!.button_complete_todo_item.text = getString(R.string.mark_as_incomplete)
            } else {
                dialog!!.button_complete_todo_item.text = getString(R.string.mark_as_complete)
            }
            onCheckClicked(todoItem)
        }

        dialog!!.button_edit_todo_item.setOnClickListener {
            NotificationUtils().cancelNotification(todoItem, this)
            val intent = Intent(this@MainActivity, AddEditTodoItemActivity::class.java)
            intent.putExtra(Constants.KEY_INTENT, todoItem)
            startActivityForResult(intent, Constants.INTENT_EDIT_TODO_ITEM)
            dialog!!.dismiss()
        }

        dialog!!.show()
    }

    

    private fun displayEmptyTaskListImage() {
        if (iv_empty_task_list.visibility == View.GONE) {
            iv_empty_task_list.visibility = View.VISIBLE
        }
    }
}