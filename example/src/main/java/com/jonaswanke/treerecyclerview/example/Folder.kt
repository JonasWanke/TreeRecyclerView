package com.jonaswanke.treerecyclerview.example

import android.content.Context


data class Folder(val parent: Folder?, val name: String)

fun Context.createDummyFolder(parent: Folder?, number: Int): Folder {
    return Folder(null, getString(R.string.folder_name, number))
}
