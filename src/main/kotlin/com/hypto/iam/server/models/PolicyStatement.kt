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
 *  * @param resourceType  * @param action  * @param effect */
data class PolicyStatement(
    val resourceType: kotlin.String,
    val action: kotlin.String,
    val effect: PolicyStatement.Effect
) {
    /**
    *
    * Values: ALLOW,DENY
    */
    enum class Effect(val value: kotlin.String) {
        ALLOW("allow"),
        DENY("deny");
    }
}
