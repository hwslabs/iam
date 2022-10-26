# VerifyEmailRequest
## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**email** | **String** |  | [default to null]
**organizationId** | **String** |  | [optional] [default to null]
**purpose** | **String** |  | [default to null]
**metadata** | **Map** | Additional metadata to be sent along with the request. Every purpose requires different metadata. - signup :     if user provides admin user and org details in metadata, they don&#39;t need to be provided in the request body during CreateOrganization request.     Supported metadata keys:     1. name : string (required): name of the organization     2. description : string (optional) - description of the organization     3. rootUserPassword : string (required) - password of the root user     4. rootUserName : string (optional) - name of the root user     5. rootUserPreferredUsername : string (optional) - preferred username of the root user     6. rootUserPhone : string (optional) - phone number of the root user  | [optional] [default to null]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

