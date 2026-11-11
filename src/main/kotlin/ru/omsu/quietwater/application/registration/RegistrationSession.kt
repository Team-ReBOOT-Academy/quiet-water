package ru.omsu.quietwater.application.registration

enum class RegistrationPhase {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class RegistrationCredentials(
    val accountId: String,
    val groupId: String,
    val sessionId: String,
    val secretKey: String
)

class RegistrationSession {

    private val guard = Any()
    private var phase: RegistrationPhase = RegistrationPhase.PENDING
    private var credentials: RegistrationCredentials? = null
    private var rejectionReason: String? = null
    private val requestIds = mutableSetOf<String>()

    fun phase(): RegistrationPhase = synchronized(guard) { phase }

    fun credentials(): RegistrationCredentials? = synchronized(guard) { credentials }

    fun rejectionReason(): String? = synchronized(guard) { rejectionReason }

    fun canPublish(): Boolean = credentials() != null && phase() == RegistrationPhase.ACCEPTED

    fun noteRequest(eventId: String) {
        synchronized(guard) {
            requestIds.add(eventId)
        }
    }

    fun owns(eventId: String?): Boolean {
        if (eventId.isNullOrBlank()) {
            return false
        }
        return synchronized(guard) { requestIds.contains(eventId) }
    }

    fun accept(accepted: RegistrationCredentials) {
        synchronized(guard) {
            phase = RegistrationPhase.ACCEPTED
            credentials = accepted
            rejectionReason = null
        }
    }

    fun reject(reason: String) {
        synchronized(guard) {
            phase = RegistrationPhase.REJECTED
            credentials = null
            rejectionReason = reason
        }
    }
}
