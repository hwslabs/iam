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
*/package com.hypto.iam.server.models


/**
 * Payload to create organization * @param name  * @param description */
data class CreateOrganizationRequest (        val name: kotlin.String,    val description: kotlin.String? = null
) {
}