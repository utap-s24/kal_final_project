package edu.utap.kal.view

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.Timestamp
import edu.utap.kal.AuthWrap
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.databinding.NoteListRowBinding
import edu.utap.kal.model.Note
import java.util.*


class NotesAdapter(private val viewModel: MainViewModel,
                   private val editNote: ((Int) -> Unit)? = null)
    : ListAdapter<Note, NotesAdapter.VH>(Diff()) {
    // This class allows the adapter to compute what has changed
    class Diff : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.firestoreID == newItem.firestoreID
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.firestoreID == newItem.firestoreID
                    && oldItem.text == newItem.text
                    && oldItem.ownerUid == newItem.ownerUid
                    && oldItem.timeStamp == newItem.timeStamp
        }
    }

    // Puts the time first, which is most important.  But date is useful too
    private val dateFormat: DateFormat =
        SimpleDateFormat("hh:mm:ss MM-dd-yyyy", Locale.US)

    // https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ViewHolder#getBindingAdapterPosition()
    // Getting the position of the selected item is unfortunately complicated
    // This always returns a valid index.
    private fun getPos(holder: VH) : Int {
        val pos = holder.bindingAdapterPosition
        // notifyDataSetChanged was called, so position is not known
        if( pos == RecyclerView.NO_POSITION) {
            return holder.absoluteAdapterPosition
        }
        return pos
    }

    inner class VH(private val noteListRowBinding: NoteListRowBinding) :
        RecyclerView.ViewHolder(noteListRowBinding.root) {

        private fun bindPic1(imageList: List<String>) {
            if(imageList.isNotEmpty()) {
                viewModel.glideFetch(imageList[0], noteListRowBinding.pic1IV)
            } else {
                noteListRowBinding.pic1IV.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
                // NB: This won't work
                //pic1IV.background = ColorDrawable(Color.WHITE)
            }
        }
        init {
            noteListRowBinding.noteRowRV.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

//            noteListRowBinding.deleteBut.setOnClickListener {
//                editNote?.let { it1 -> it1(getPos(this)) }
//                true
//            }

        }
        fun bind(holder: VH, position: Int) {
            // differentiate between whether user is viewing their own notes or others'
            val note = viewModel.getNote(position)
            holder.noteListRowBinding.text.text = note.text

            // parse the timeStamp to look nice
            val timeStamp: Timestamp? = note.timeStamp
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.ENGLISH)
            val formattedDate = if (timeStamp != null) {
                val date = Date(timeStamp.seconds * 1000 + timeStamp.nanoseconds / 1000000)
                outputFormat.format(date)
            } else {
                null
            }
            holder.noteListRowBinding.timestamp.text = formattedDate


            bindPic1(note.pictureUUIDs)
            //Log.d(javaClass.simpleName, "bind adapter ${bindingAdapterPosition}")
            val adapter = ImageAdapter(viewModel)
            holder.noteListRowBinding.noteRowRV.adapter = adapter
            adapter.submitList(note.pictureUUIDs)
            note.timeStamp?.let {
                holder.noteListRowBinding.timestamp.text = dateFormat.format(it.toDate())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val noteListRowBinding = NoteListRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return VH(noteListRowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(holder, position)
    }
}