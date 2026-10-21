package ru.omsu.quietwater.adapter.kafka

class ProcessedEventIds(private val capacity: Int) {

    private val seen = LinkedHashSet<String>()

    fun alreadySeen(eventId: String): Boolean = synchronized(seen) { eventId in seen }

    fun remember(eventId: String) {
        synchronized(seen) {
            if (!seen.add(eventId)) {
                return
            }
            val iterator = seen.iterator()
            while (seen.size > capacity && iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }
}
