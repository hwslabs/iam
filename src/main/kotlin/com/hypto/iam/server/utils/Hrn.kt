package com.hypto.iam.server.utils

// TODO: Introduce a "service" entity to replace "iam". For services like ledger / waller / etc.
// Ref: https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
data class Hrn(
    val organization: String,
    val resourceType: String,
    val resourceInstance: String?
) {
    companion object {
        private const val HRN_COMPONENTS = 4
        fun of(organization: String, resourceType: String, resourceInstance: String?): Hrn {
            return Hrn(organization, resourceType, resourceInstance)
        }

        fun of(hrnString: String): Hrn {
            // hws:iam:id1:user/user1
            // TODO: Validate if the input is indeed an HRN string
            try {
                val hrnArray = hrnString.split(":").reversed()
                if (hrnArray.size != HRN_COMPONENTS || hrnArray.any { it.isEmpty() }) { throw HrnParseException() }

                // TODO: Decide what kind of regex usage we should allow (prefix *, suffix *, etc.)
                // and add validations accordingly
                val resourceArray = hrnArray[0].split("/", limit = 2)
                return Hrn(hrnArray[1], resourceArray[0], resourceArray.getOrNull(1))
            } catch (_: Exception) {
                throw HrnParseException()
            }
        }
    }

    private var hrnStr: String

    init {
        val instanceSection = resourceInstance?.let { "/$it" }
        hrnStr = "hws:iam:$organization:$resourceType$instanceSection"
    }

    override fun toString(): String {
        return hrnStr
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hrn

        if (hrnStr != other.hrnStr) return false

        return true
    }

    override fun hashCode(): Int {
        return hrnStr.hashCode()
    }
}

class HrnParseException(message: String?) : Exception(message) {
    constructor() : this(null)
}
