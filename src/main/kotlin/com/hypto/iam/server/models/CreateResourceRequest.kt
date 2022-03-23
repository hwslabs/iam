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
 * Payload to create resource
 * @param name
 * @param description
 */
data class CreateResourceRequest(
    val name: kotlin.String,
    val description: kotlin.String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 123
    }
}