# ResourceApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createResource**](ResourceApi.md#createResource) | **POST** /organizations/{organization_id}/resources | Create a resource
[**deleteResource**](ResourceApi.md#deleteResource) | **DELETE** /organizations/{organization_id}/resources/{id} | Delete a resource
[**getResource**](ResourceApi.md#getResource) | **GET** /organizations/{organization_id}/resources/{id} | Get a resource
[**listResources**](ResourceApi.md#listResources) | **GET** /organizations/{organization_id}/resources | List Resources
[**updateResource**](ResourceApi.md#updateResource) | **PATCH** /organizations/{organization_id}/resources/{id} | Update a resource


<a name="createResource"></a>
# **createResource**
> Resource createResource(organization\_id, CreateResourceRequest)

Create a resource

    Create a resource

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **CreateResourceRequest** | [**CreateResourceRequest**](../Models/CreateResourceRequest.md)| Payload to create resource |

### Return type

[**Resource**](../Models/Resource.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteResource"></a>
# **deleteResource**
> BaseSuccessResponse deleteResource(organization\_id, id)

Delete a resource

    Delete a resource

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

<a name="getResource"></a>
# **getResource**
> Resource getResource(organization\_id, id)

Get a resource

    Get a resource

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**Resource**](../Models/Resource.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listResources"></a>
# **listResources**
> ResourcePaginatedResponse listResources(organization\_id, nextToken, pageSize, sortOrder)

List Resources

    List Resources

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **nextToken** | **String**|  | [optional] [default to null]
 **pageSize** | **String**|  | [optional] [default to null]
 **sortOrder** | **String**|  | [optional] [default to null] [enum: asc, desc]

### Return type

[**ResourcePaginatedResponse**](../Models/ResourcePaginatedResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateResource"></a>
# **updateResource**
> Resource updateResource(organization\_id, id, UpdateResourceRequest)

Update a resource

    Update a resource

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]
 **UpdateResourceRequest** | [**UpdateResourceRequest**](../Models/UpdateResourceRequest.md)| Payload to update resource |

### Return type

[**Resource**](../Models/Resource.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

