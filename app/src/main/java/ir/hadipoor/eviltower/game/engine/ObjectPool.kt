package ir.hadipoor.eviltower.game.engine

/** Tiny reusable pool used by the engine for transient visual objects. */
class ObjectPool<T>(private val factory: () -> T, initialSize: Int = 0) {
    private val free = ArrayDeque<T>().apply { repeat(initialSize) { add(factory()) } }
    fun obtain(): T = if (free.isEmpty()) factory() else free.removeFirst()
    fun recycle(value: T) { free.addLast(value) }
    val size: Int get() = free.size
}
