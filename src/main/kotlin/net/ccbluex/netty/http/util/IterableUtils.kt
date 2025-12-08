package net.ccbluex.netty.http.util

inline fun <reified E> Iterable<*>.forEachIsInstance(action: (E) -> Unit) {
    for (it in this) {
        if (it is E) {
            action(it)
        }
    }
}
