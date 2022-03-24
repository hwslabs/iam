# PolicyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createPolicy**](PolicyManagementApi.md#createPolicy) | **POST** /organizations/{organization_id}/policies | Create a policy
[**deletePolicy**](PolicyManagementApi.md#deletePolicy) | **DELETE** /organizations/{organization_id}/policies/{policy_name} | Delete a policy
[**getPolicy**](PolicyManagementApi.md#getPolicy) | **GET** /organizations/{organization_id}/policies/{policy_name} | Get a policy
[**listPolicies**](PolicyManagementApi.md#listPolicies) | **GET** /organizations/{organization_id}/policies | List policies
[**updatePolicy**](PolicyManagementApi.md#updatePolicy) | **PATCH** /organizations/{organization_id}/policies/{policy_name} | Update a policy


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

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deletePolicy"></a>
# **deletePolicy**
> BaseSuccessResponse deletePolicy(organization\_id, policy\_name)

Delete a policy

    Delete a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **policy\_name** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getPolicy"></a>
# **getPolicy**
> Policy getPolicy(organization\_id, policy\_name)

Get a policy

    Get a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **policy\_name** | **String**|  | [default to null]

### Return type

[**Policy**](../Models/Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

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

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updatePolicy"></a>
# **updatePolicy**
> Policy updatePolicy(organization\_id, policy\_name, UpdatePolicyRequest)

Update a policy

    Update a policy

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **policy\_name** | **String**|  | [default to null]
 **UpdatePolicyRequest** | [**UpdatePolicyRequest**](../Models/UpdatePolicyRequest.md)| Payload to update policy |

### Return type

[**Policy**](../Models/Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

