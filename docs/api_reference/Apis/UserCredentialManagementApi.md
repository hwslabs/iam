# UserCredentialManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createCredential**](UserCredentialManagementApi.md#createCredential) | **POST** /organizations/{organization_id}/users/{user_id}/credentials | Create a new credential for a user
[**deleteCredential**](UserCredentialManagementApi.md#deleteCredential) | **DELETE** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Delete a credential
[**getCredential**](UserCredentialManagementApi.md#getCredential) | **GET** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Gets credential for the user
[**updateCredential**](UserCredentialManagementApi.md#updateCredential) | **PATCH** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Update the status of credential


<a name="createCredential"></a>
# **createCredential**
> Credential createCredential(user\_id, organization\_id, CreateCredentialRequest)

Create a new credential for a user

    Create a new credential for a user. This API returns the credential&#39;s secret key, which will be available only in the response of this API.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **CreateCredentialRequest** | [**CreateCredentialRequest**](../Models/CreateCredentialRequest.md)| Payload to create credential |

### Return type

[**Credential**](../Models/Credential.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteCredential"></a>
# **deleteCredential**
> BaseSuccessResponse deleteCredential(organization\_id, user\_id, id)

Delete a credential

    Delete a credential associated with the user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **user\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getCredential"></a>
# **getCredential**
> CredentialWithoutSecret getCredential(organization\_id, user\_id, id)

Gets credential for the user

    Gets credential for the user, given the credential id

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **user\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**CredentialWithoutSecret**](../Models/CredentialWithoutSecret.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateCredential"></a>
# **updateCredential**
> CredentialWithoutSecret updateCredential(organization\_id, user\_id, id, UpdateCredentialRequest)

Update the status of credential

    Update the status of credential to ACTIVE/INACTIVE. Credentials which are marked INACTIVE cannot be used to fetch short-term tokens.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **user\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]
 **UpdateCredentialRequest** | [**UpdateCredentialRequest**](../Models/UpdateCredentialRequest.md)| Payload to update credential |

### Return type

[**CredentialWithoutSecret**](../Models/CredentialWithoutSecret.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

