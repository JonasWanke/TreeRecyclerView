@file:Suppress("TooManyFunctions")

package com.jonaswanke.treerecyclerview.utils

import androidx.lifecycle.*


// Mapping
fun <T, R> LiveData<T>.map(func: (T) -> R): LiveData<R> = Transformations.map(this, func)

fun <T, R> LiveData<T>.switchMap(func: (T) -> LiveData<R>): LiveData<R> {
    return Transformations.switchMap(this, func)
}
