# ActionApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAction**](ActionApi.md#createAction) | **POST** /organizations/{organization_id}/resources/{resource_name}/actions | Create an action
[**deleteAction**](ActionApi.md#deleteAction) | **DELETE** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Delete an action
[**getAction**](ActionApi.md#getAction) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Get an action
[**updateAction**](ActionApi.md#updateAction) | **PATCH** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Update an action


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

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteAction"></a>
# **deleteAction**
> BaseSuccessResponse deleteAction(organization\_id, resource\_name, id)

Delete an action

    Delete an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getAction"></a>
# **getAction**
> Action getAction(organization\_id, resource\_name, id)

Get an action

    Get an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**Action**](../Models/Action.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateAction"></a>
# **updateAction**
> Action updateAction(organization\_id, resource\_name, id, UpdateActionRequest)

Update an action

    Update an action

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **resource\_name** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]
 **UpdateActionRequest** | [**UpdateActionRequest**](../Models/UpdateActionRequest.md)| Payload to update action |

### Return type

[**Action**](../Models/Action.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

