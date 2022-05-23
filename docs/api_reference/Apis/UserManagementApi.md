# UserManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createUser**](UserManagementApi.md#createUser) | **POST** /organizations/{organization_id}/users | Create a user
[**deleteUser**](UserManagementApi.md#deleteUser) | **DELETE** /organizations/{organization_id}/users/{user_name} | Delete a User
[**getUser**](UserManagementApi.md#getUser) | **GET** /organizations/{organization_id}/users/{user_name} | Gets a user entity associated with the organization
[**listUsers**](UserManagementApi.md#listUsers) | **GET** /organizations/{organization_id}/users | List users
[**resetPassword**](UserManagementApi.md#resetPassword) | **POST** /organizations/{organization_id}/users/resetPassword | Reset Password
[**updateUser**](UserManagementApi.md#updateUser) | **PATCH** /organizations/{organization_id}/users/{user_name} | Update a User


<a name="createUser"></a>
# **createUser**
> User createUser(organization\_id, CreateUserRequest)

Create a user

    User is an entity which represent a person who is part of the organization or account. This user entity can be created either through user name, password or the user can be federated through an identity provider like Google, Facebook or any SAML 2.0, OIDC identity provider. This is a sign-up api to create a new user in an organization.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **CreateUserRequest** | [**CreateUserRequest**](../Models/CreateUserRequest.md)| Payload to create user |

### Return type

[**User**](../Models/User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteUser"></a>
# **deleteUser**
> BaseSuccessResponse deleteUser(user\_name, organization\_id)

Delete a User

    Delete a User

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_name** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getUser"></a>
# **getUser**
> User getUser(user\_name, organization\_id)

Gets a user entity associated with the organization

    Get a User

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_name** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]

### Return type

[**User**](../Models/User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listUsers"></a>
# **listUsers**
> UserPaginatedResponse listUsers(organization\_id, nextToken, pageSize)

List users

    List users associated with the organization. This is a pagniated api which returns the list of users with a next page token.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **nextToken** | **String**|  | [optional] [default to null]
 **pageSize** | **String**|  | [optional] [default to null]

### Return type

[**UserPaginatedResponse**](../Models/UserPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="resetPassword"></a>
# **resetPassword**
> BaseSuccessResponse resetPassword(organization\_id, ResetPasswordRequest)

Reset Password

    Reset Password

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **ResetPasswordRequest** | [**ResetPasswordRequest**](../Models/ResetPasswordRequest.md)| Payload to reset password |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="updateUser"></a>
# **updateUser**
> User updateUser(user\_name, organization\_id, UpdateUserRequest)

Update a User

    Update a User

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user\_name** | **String**|  | [default to null]
 **organization\_id** | **String**|  | [default to null]
 **UpdateUserRequest** | [**UpdateUserRequest**](../Models/UpdateUserRequest.md)| Payload to update user |

### Return type

[**User**](../Models/User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

