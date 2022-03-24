# UserPolicyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**attachPolicies**](UserPolicyManagementApi.md#attachPolicies) | **PATCH** /organizations/{organization_id}/users/{user_name}/attach_policies | Attach policies to user
[**detachPolicies**](UserPolicyManagementApi.md#detachPolicies) | **PATCH** /organizations/{organization_id}/users/{user_name}/detach_policies | Detach policies from user
[**getUserPolicies**](UserPolicyManagementApi.md#getUserPolicies) | **GET** /organizations/{organization_id}/users/{user_name}/policies | List all policies associated with user


<a name="attachPolicies"></a>
# **attachPolicies**
> BaseSuccessResponse attachPolicies(user\_name, organization\_id, PolicyAssociationRequest)

Attach policies to user

    Attach policies to user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_name** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **PolicyAssociationRequest** | [**PolicyAssociationRequest**](../Models/PolicyAssociationRequest.md)| Payload to attach / detach a policy to a user / resource |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="detachPolicies"></a>
# **detachPolicies**
> BaseSuccessResponse detachPolicies(user\_name, organization\_id, PolicyAssociationRequest)

Detach policies from user

    Detach policies from user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_name** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **PolicyAssociationRequest** | [**PolicyAssociationRequest**](../Models/PolicyAssociationRequest.md)| Payload to attach / detach a policy to a user / resource |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="getUserPolicies"></a>
# **getUserPolicies**
> PolicyPaginatedResponse getUserPolicies(user\_name, organization\_id, nextToken, pageSize, sortOrder)

List all policies associated with user

    List all policies associated with user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_name** | **String**|  | [default to null]
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

