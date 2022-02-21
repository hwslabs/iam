package com.hypto.iam.server.utils

// TODO: Introduce a "service" entity to replace "iam". For services like ledger / waller / etc.
// Ref: https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
data class Hrn(
    val organization: String,
    val resourceType: String,
    val resourceInstance: String
) {
    companion object {
        fun of(organization: String, resourceType: String, resourceInstance: String): Hrn {
            return Hrn(organization, resourceType, resourceInstance)
        }

        fun of(hrnString: String): Hrn {
            // TODO: Validate if the input is indeed an HRN string
            val arnArray = hrnString.split(":").reversed()
            val resourceArray = arnArray[0].split("/")
            return Hrn(arnArray[1], resourceArray[0], resourceArray[1])
        }
    }

    private val hrnStr = "hws:iam:$organization:$resourceType/$resourceInstance"

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
