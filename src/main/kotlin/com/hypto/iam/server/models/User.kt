/**
* Hypto IAM
* APIs for Hypto IAM Service.
*
* The version of the OpenAPI document: 1.0.0
* Contact: engineering@hypto.in
*
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package com.hypto.iam.server.models

import java.io.Serializable
/**
 *
 * @param hrn
 * @param username
 * @param organizationId
 * @param email
 * @param status
 * @param verified
 * @param phone
 */
data class User(
    val hrn: kotlin.String,
    val username: kotlin.String,
    val organizationId: kotlin.String,
    val email: kotlin.String,
    val status: User.Status,
    val verified: kotlin.Boolean,
    val phone: kotlin.String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 123
    }
    /**
    *
    * Values: enabled,disabled
    */
    enum class Status(val value: kotlin.String) {
        enabled("enabled"),
        disabled("disabled");
    }
}
