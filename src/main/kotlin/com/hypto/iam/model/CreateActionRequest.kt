/**
* Hypto IAM
* APIs for Hypto IAM Service.
*
* OpenAPI spec version: 1.0.0
* Contact: engineering@hypto.in
*
* NOTE: This class is auto generated by the swagger code generator program.
* https://github.com/swagger-api/swagger-codegen.git
* Do not edit the class manually.
*/package com.hypto.iam.model


/**
 * Payload to create action * @param name  * @param description */
data class CreateActionRequest (        val name: kotlin.String,    val description: kotlin.String? = null
) {
}
