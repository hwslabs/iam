# ResourceActionManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAction**](ResourceActionManagementApi.md#createAction) | **POST** /organizations/{organization_id}/resources/{resource_name}/actions | Create an action
[**deleteAction**](ResourceActionManagementApi.md#deleteAction) | **DELETE** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Delete an action
[**getAction**](ResourceActionManagementApi.md#getAction) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Get an action
[**listActions**](ResourceActionManagementApi.md#listActions) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions | List actions
[**updateAction**](ResourceActionManagementApi.md#updateAction) | **PATCH** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Update an action


<a name="createAction"></a>
# **createAction**
> Action createAction(organization\_id, resource\_name, CreateActionRequest)

Create an action

    Create an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **CreateActionRequest** | [**CreateActionRequest**](../Models/CreateActionRequest.md)| Payload to create action |

### Return type

[**Action**](../Models/Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteAction"></a>
# **deleteAction**
> BaseSuccessResponse deleteAction(organization\_id, resource\_name, action\_name)

Delete an action

    Delete an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **action\_name** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getAction"></a>
# **getAction**
> Action getAction(organization\_id, resource\_name, action\_name)

Get an action

    Get an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **action\_name** | **String**|  | [default to null]

### Return type

[**Action**](../Models/Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listActions"></a>
# **listActions**
> ActionPaginatedResponse listActions(organization\_id, resource\_name, nextToken, pageSize, sortOrder)

List actions

    List actions

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **nextToken** | **String**|  | [optional] [default to null]
 **pageSize** | **String**|  | [optional] [default to null]
 **sortOrder** | **String**|  | [optional] [default to null] [enum: asc, desc]

### Return type

[**ActionPaginatedResponse**](../Models/ActionPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateAction"></a>
# **updateAction**
> Action updateAction(organization\_id, resource\_name, action\_name, UpdateActionRequest)

Update an action

    Update an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **action\_name** | **String**|  | [default to null]
 **UpdateActionRequest** | [**UpdateActionRequest**](../Models/UpdateActionRequest.md)| Payload to update action |

### Return type

[**Action**](../Models/Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

