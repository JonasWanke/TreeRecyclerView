package com.jonaswanke.treerecyclerview

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updateMarginsRelative
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.properties.Delegates


abstract class TreeAdapter<T, VH : TreeAdapter.TreeViewHolder>(
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<VH>() {

    private var _children: MutableList<TreeNode<T>> by Delegates.observable(mutableListOf()) { _, _, _ ->
        notifyDataSetChanged()
    }
    private val observers: MutableMap<TreeNode<T>, Pair<LiveData<List<T>>, Observer<List<T>>>> = mutableMapOf()

    final override fun getItemCount() = _children.sumBy { it.totalSize }
    override fun onBindViewHolder(holder: VH, position: Int) {
        Log.d("TreeAdapter", "bind pos $position")
        holder.setLevel(positionToNode(position).level)
    }

    fun setItems(items: List<T>) {
        _children = items.map { populate(null, it) }.toMutableList()
    }

    private fun populate(parent: TreeNode<T>?, child: T): TreeNode<T> {
        fun stopObserving(node: TreeNode<T>) {
            val pair = observers[node]
            if (pair != null) {
                val (liveData, observer) = pair
                liveData.removeObserver(observer)
            }

            @Suppress("NAME_SHADOWING")
            for (child in node.children)
                stopObserving(child)
        }

        fun startObserving(node: TreeNode<T>) {
            val liveData = provideChildren(node.value)
            val observer = Observer<List<T>> { list ->
                val oldChildren = node.children
                val oldPositions = oldChildren.map { nodeToPosition(it) }
                val newChildren = list.map { populate(node, it) }
                node.children = newChildren

                launch {
                    val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                            val oldItem = oldChildren[oldItemPosition]
                            val newItem = newChildren[newItemPosition]
                            return this@TreeAdapter.areItemsTheSame(oldItem.value, newItem.value)
                        }

                        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                            val oldItem = oldChildren[oldItemPosition]
                            val newItem = newChildren[newItemPosition]
                            return this@TreeAdapter.areContentsTheSame(oldItem.value, newItem.value)
                        }

                        override fun getOldListSize() = oldChildren.size
                        override fun getNewListSize() = newChildren.size
                    })

                    launch(UI) {
                        diff.dispatchUpdatesTo(object : ListUpdateCallback {
                            override fun onChanged(position: Int, count: Int, payload: Any?) {
                                onRemoved(position, count)
                                onInserted(position, count)
                            }

                            override fun onMoved(fromPosition: Int, toPosition: Int) {
                                this@TreeAdapter.notifyItemMoved(oldPositions[fromPosition],
                                        nodeToPosition(newChildren[toPosition]))
                            }

                            override fun onInserted(position: Int, count: Int) {
                                @Suppress("NAME_SHADOWING")
                                for (child in newChildren.subList(position, position + count))
                                    startObserving(child)

                                val start = nodeToPosition(newChildren[position])
                                val end = nodeToPosition(newChildren[position + count - 1], false)
                                this@TreeAdapter.notifyItemRangeInserted(start, end - start)
                            }

                            override fun onRemoved(position: Int, count: Int) {
                                @Suppress("NAME_SHADOWING")
                                for (child in oldChildren.subList(position, position + count))
                                    stopObserving(child)

                                val start = oldPositions[position]
                                val end = oldPositions[position + count - 1] + oldChildren[position + count - 1].totalSize
                                this@TreeAdapter.notifyItemRangeRemoved(start, end - start)
                            }
                        })
                    }
                }
            }
            liveData.observe(lifecycleOwner, observer)
            observers[node] = liveData to observer
        }

        return TreeNode(parent, child).apply {
            startObserving(this)
        }
    }

    abstract fun provideChildren(item: T): LiveData<List<T>>
    abstract fun areItemsTheSame(old: T, new: T): Boolean
    abstract fun areContentsTheSame(old: T, new: T): Boolean


    // Helpers
    /**
     * Calculates the flat position of the given node.
     *
     * - `start == true`: actual position of the node
     * - `start == false`: position where the next node begins
     */
    protected fun nodeToPosition(node: TreeNode<T>, start: Boolean = true): Int {
        var position = 0
        var currentNode = node
        var parent = node.parent
        while (parent != null) {
            var sibling = prevNode(currentNode)
            while (sibling != null) {
                position += sibling.totalSize
                sibling = prevNode(sibling)
            }
            position += 1

            currentNode = parent
            parent = currentNode.parent
        }
        return if (start) position else position + node.totalSize
    }

    protected fun positionToNode(position: Int): TreeNode<T> {
        var currentNode: TreeNode<T> = _children.first()
        var currentPosition = 0
        while (true) {
            while (currentPosition < position) {
                currentPosition += currentNode.totalSize
                currentNode = nextNode(currentNode)!!
            }
            if (currentPosition == position)
                return currentNode

            currentNode = prevNode(currentNode)!!
            currentPosition -= currentNode.totalSize - 1
            currentNode = currentNode.firstChild!!
        }
    }

    private fun prevNode(node: TreeNode<T>): TreeNode<T>? {
        return if (node.parent != null) node.prevSibling
        else _children.getOrNull(_children.indexOf(node) - 1)
    }

    private fun nextNode(node: TreeNode<T>): TreeNode<T>? {
        return if (node.parent != null) node.nextSibling
        else {
            val index = _children.indexOf(node).takeIf { it >= 0 } ?: Int.MIN_VALUE
            _children.getOrNull(index + 1)
        }
    }


    abstract class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        protected var indentation = itemView.context.resources.getDimension(R.dimen.item_levelIndentation)

        open fun setLevel(level: Int) {
            val layoutParams = itemView.layoutParams as? RecyclerView.LayoutParams ?: RecyclerView.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                layoutParams.updateMarginsRelative(start = (level * indentation).toInt())
            else
                layoutParams.updateMargins(left = (level * indentation).toInt())
            itemView.layoutParams = layoutParams
        }
    }
}
