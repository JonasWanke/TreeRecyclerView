package com.jonaswanke.treerecyclerview


data class TreeNode<T>(
    val parent: TreeNode<T>?,
    val value: T
) {
    var children: List<TreeNode<T>> = emptyList()

    val totalSize: Int
        get() {
            return if (expanded) 1 + children.sumBy { it.totalSize }
            else 1
        }
    val level: Int get() = parent?.level?.plus(1) ?: 0

    val firstChild: TreeNode<T>? get() = children.firstOrNull()
    val lastChild: TreeNode<T>? get() = children.lastOrNull()
    val prevSibling: TreeNode<T>?
        get() {
            val siblings = parent?.children ?: return null
            val index = siblings.indexOf(this).takeIf { it >= 0 } ?: Int.MAX_VALUE
            return siblings.getOrNull(index - 1)
        }
    val nextSibling: TreeNode<T>?
        get() {
            val siblings = parent?.children ?: return null
            val index = siblings.indexOf(this).takeIf { it >= 0 } ?: Int.MIN_VALUE
            return siblings.getOrNull(index + 1)
        }


    var expanded: Boolean = true
    fun expandAll() {
        expanded = true
        for (child in children)
            child.expandAll()
    }
}
