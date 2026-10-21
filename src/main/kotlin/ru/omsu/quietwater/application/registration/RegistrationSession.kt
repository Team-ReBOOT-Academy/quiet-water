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

    fun phase(): RegistrationPhase = synchronized(guard) { phase }

    fun credentials(): RegistrationCredentials? = synchronized(guard) { credentials }

    fun rejectionReason(): String? = synchronized(guard) { rejectionReason }

    fun canPublish(): Boolean = credentials() != null && phase() == RegistrationPhase.ACCEPTED

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
