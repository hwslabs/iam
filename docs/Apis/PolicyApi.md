# PolicyApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createPolicy**](PolicyApi.md#createPolicy) | **POST** /organizations/{organization_id}/policies | Create a policy
[**deletePolicy**](PolicyApi.md#deletePolicy) | **DELETE** /organizations/{organization_id}/policies/{id} | Delete a policy
[**getPolicy**](PolicyApi.md#getPolicy) | **GET** /organizations/{organization_id}/policies/{id} | Get a policy
[**getUserPolicies**](PolicyApi.md#getUserPolicies) | **GET** /organizations/{organization_id}/users/{user_id}/policies | List policies of a user
[**listPolicies**](PolicyApi.md#listPolicies) | **GET** /organizations/{organization_id}/policies | List policies
[**updatePolicy**](PolicyApi.md#updatePolicy) | **PATCH** /organizations/{organization_id}/policies/{id} | Update a policy


<a name="createPolicy"></a>
# **createPolicy**
> Policy createPolicy(organization\_id, CreatePolicyRequest)

Create a policy

    Create a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **CreatePolicyRequest** | [**CreatePolicyRequest**](../Models/CreatePolicyRequest.md)| Payload to create policy |

### Return type

[**Policy**](../Models/Policy.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deletePolicy"></a>
# **deletePolicy**
> BaseSuccessResponse deletePolicy(organization\_id, id)

Delete a policy

    Delete a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getPolicy"></a>
# **getPolicy**
> Policy getPolicy(organization\_id, id)

Get a policy

    Get a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**Policy**](../Models/Policy.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getUserPolicies"></a>
# **getUserPolicies**
> PolicyPaginatedResponse getUserPolicies(user\_id, organization\_id, nextToken, pageSize, sortOrder)

List policies of a user

    List policies of a user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **nextToken** | **String**|  | [optional] [default to null]
 **pageSize** | **String**|  | [optional] [default to null]
 **sortOrder** | **String**|  | [optional] [default to null] [enum: asc, desc]

### Return type

[**PolicyPaginatedResponse**](../Models/PolicyPaginatedResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listPolicies"></a>
# **listPolicies**
> PolicyPaginatedResponse listPolicies(organization\_id, nextToken, pageSize, sortOrder)

List policies

    List policies

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **nextToken** | **String**|  | [optional] [default to null]
 **pageSize** | **String**|  | [optional] [default to null]
 **sortOrder** | **String**|  | [optional] [default to null] [enum: asc, desc]

### Return type

[**PolicyPaginatedResponse**](../Models/PolicyPaginatedResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updatePolicy"></a>
# **updatePolicy**
> Policy updatePolicy(organization\_id, id, UpdatePolicyRequest)

Update a policy

    Update a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]
 **UpdatePolicyRequest** | [**UpdatePolicyRequest**](../Models/UpdatePolicyRequest.md)| Payload to update policy |

### Return type

[**Policy**](../Models/Policy.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

