# UsersApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**attachPolicies**](UsersApi.md#attachPolicies) | **PUT** /organizations/{organization_id}/users/{id}/attach_policies | Attach policies to user
[**createUser**](UsersApi.md#createUser) | **POST** /organizations/{organization_id}/users | Create a user
[**deleteUser**](UsersApi.md#deleteUser) | **DELETE** /organizations/{organization_id}/users/{id} | Delete a User
[**detachPolicies**](UsersApi.md#detachPolicies) | **PUT** /organizations/{organization_id}/users/{id}/detach_policies | Detach policies to user
[**getUser**](UsersApi.md#getUser) | **GET** /organizations/{organization_id}/users/{id} | Get a User
[**listUsers**](UsersApi.md#listUsers) | **GET** /organizations/{organization_id}/users | List users
[**updateUser**](UsersApi.md#updateUser) | **PATCH** /organizations/{organization_id}/users/{id} | Update a User


<a name="attachPolicies"></a>
# **attachPolicies**
> BaseSuccessResponse attachPolicies(id, organization\_id, PolicyAssociationRequest)

Attach policies to user

    Attach policies to user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **PolicyAssociationRequest** | [**PolicyAssociationRequest**](../Models/PolicyAssociationRequest.md)| Payload to attach / detach a policy to a user / resource |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="createUser"></a>
# **createUser**
> User createUser(organization\_id, CreateUserRequest)

Create a user

    Create a user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **CreateUserRequest** | [**CreateUserRequest**](../Models/CreateUserRequest.md)| Payload to create user |

### Return type

[**User**](../Models/User.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteUser"></a>
# **deleteUser**
> BaseSuccessResponse deleteUser(id, organization\_id)

Delete a User

    Delete a User

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="detachPolicies"></a>
# **detachPolicies**
> BaseSuccessResponse detachPolicies(id, organization\_id, PolicyAssociationRequest)

Detach policies to user

    Detach policies to user

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **PolicyAssociationRequest** | [**PolicyAssociationRequest**](../Models/PolicyAssociationRequest.md)| Payload to attach / detach a policy to a user / resource |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="getUser"></a>
# **getUser**
> User getUser(id, organization\_id)

Get a User

    Get a User

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]

### Return type

[**User**](../Models/User.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listUsers"></a>
# **listUsers**
> UserPaginatedResponse listUsers(organization\_id, nextToken, pageSize, sortOrder)

List users

    List users

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **nextToken** | **String**|  | [optional] [default to null]
 **pageSize** | **String**|  | [optional] [default to null]
 **sortOrder** | **String**|  | [optional] [default to null] [enum: asc, desc]

### Return type

[**UserPaginatedResponse**](../Models/UserPaginatedResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateUser"></a>
# **updateUser**
> User updateUser(id, organization\_id, UpdateUserRequest)

Update a User

    Update a User

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **UpdateUserRequest** | [**UpdateUserRequest**](../Models/UpdateUserRequest.md)| Payload to update user |

### Return type

[**User**](../Models/User.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

