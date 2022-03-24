# CredentialApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createCredential**](CredentialApi.md#createCredential) | **POST** /organizations/{organization_id}/users/{user_id}/credentials | Create a Credential
[**deleteCredential**](CredentialApi.md#deleteCredential) | **DELETE** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Delete a credential
[**getCredential**](CredentialApi.md#getCredential) | **GET** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Get a credential
[**updateCredential**](CredentialApi.md#updateCredential) | **PATCH** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Update a credential


<a name="createCredential"></a>
# **createCredential**
> Credential createCredential(user\_id, organization\_id, CreateCredentialRequest)

Create a Credential

    Create a Credential

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **CreateCredentialRequest** | [**CreateCredentialRequest**](../Models/CreateCredentialRequest.md)| Payload to create credential |

### Return type

[**Credential**](../Models/Credential.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteCredential"></a>
# **deleteCredential**
> BaseSuccessResponse deleteCredential(organization\_id, user\_id, id)

Delete a credential

    Delete a credential

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **user\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getCredential"></a>
# **getCredential**
> CredentialWithoutSecret getCredential(organization\_id, user\_id, id)

Get a credential

    Get a credential

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **user\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**CredentialWithoutSecret**](../Models/CredentialWithoutSecret.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateCredential"></a>
# **updateCredential**
> CredentialWithoutSecret updateCredential(organization\_id, user\_id, id, UpdateCredentialRequest)

Update a credential

    Update a credential

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

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

