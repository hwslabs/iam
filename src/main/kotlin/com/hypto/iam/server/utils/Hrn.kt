package com.hypto.iam.server.utils

// TODO: Introduce a "service" entity to replace "iam". For services like ledger / waller / etc.
// Ref: https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
class Hrn(private val organization: String, private val resourceType: String, private val resourceInstance: String) {
    override fun toString(): String {
        return "hws:iam:$organization:$resourceType/$resourceInstance"
    }
}
