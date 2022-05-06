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
 * Administrator user details for the organization
 * @param username
 * @param passwordHash
 * @param email
 * @param phone
 */
data class AdminUser(
    val username: String,
    val passwordHash: String,
    val email: String,
    val phone: String,
    val verified: Boolean?
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 123
    }
}
