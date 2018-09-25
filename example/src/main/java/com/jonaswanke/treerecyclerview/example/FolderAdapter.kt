package com.jonaswanke.treerecyclerview.example

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.jonaswanke.treerecyclerview.TreeAdapter
import com.jonaswanke.treerecyclerview.example.databinding.ItemFolderBinding
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class FolderAdapter(lifecycleOwner: LifecycleOwner) : TreeAdapter<Folder, FolderAdapter.ViewHolder>(lifecycleOwner) {
    private lateinit var context: Context
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("FolderAdapter", "create")
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val node = positionToNode(position)
        holder.binding.folder = node.value
    }

    override fun provideChildren(item: Folder): LiveData<List<Folder>> {
        val liveData = MutableLiveData<List<Folder>>().apply {
            value = emptyList()
        }

        if (item.name.endsWith('9'))
            launch {
                delay(5000)
                val children = (1..3).map { this@FolderAdapter.context.createDummyFolder(item, it) }
                Log.d("FolderAdapter", "new children for $item")
                liveData.postValue(children)
            }

        return liveData
    }

    override fun areItemsTheSame(old: Folder, new: Folder): Boolean {
        return old == new
    }

    override fun areContentsTheSame(old: Folder, new: Folder): Boolean {
        return old.name == new.name
    }


    class ViewHolder(val binding: ItemFolderBinding) : TreeAdapter.TreeViewHolder(binding.root)
}
