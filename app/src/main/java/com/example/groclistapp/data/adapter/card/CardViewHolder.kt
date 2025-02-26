package com.example.groclistapp.data.adapter.card

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R

class CardViewHolder(itemView: View, listener: OnItemClickListener?): RecyclerView.ViewHolder(itemView) {
//    private var studentNameTextView: TextView? = null
//    private var studentIdTextView: TextView? = null
//    private var studentCheckStatusCheckBox: CheckBox? = null
//    private var studentProfileImageView: ImageView? = null
//    private var student: Student? = null

//    init {
//        studentNameTextView = itemView.findViewById(R.id.tvStudentListRowName)
//        studentIdTextView = itemView.findViewById(R.id.tvStudentListRowId)
//        studentCheckStatusCheckBox = itemView.findViewById(R.id.cbStudentListRowCheckStatus)
//        studentProfileImageView = itemView.findViewById(R.id.ivStudentListRowProfileImage)
//
//        studentCheckStatusCheckBox?.setOnClickListener {
//            student?.isChecked = studentCheckStatusCheckBox?.isChecked ?: false
//        }
//
//        itemView.setOnClickListener {
//            listener?.onItemClick(student)
//        }
//    }

    fun bind(card: Any?) {
//        this.student = student
//
//        studentProfileImageView?.setImageResource(student?.picture ?: R.drawable.avatar)
//        studentNameTextView?.text = student?.name
//        studentIdTextView?.text = student?.id.toString()
//        studentCheckStatusCheckBox?.isChecked = student?.isChecked ?: false
    }
}