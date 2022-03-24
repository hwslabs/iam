# ResourceManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createResource**](ResourceManagementApi.md#createResource) | **POST** /organizations/{organization_id}/resources | Create a resource name in an organization.
[**deleteResource**](ResourceManagementApi.md#deleteResource) | **DELETE** /organizations/{organization_id}/resources/{id} | Delete a resource
[**getResource**](ResourceManagementApi.md#getResource) | **GET** /organizations/{organization_id}/resources/{id} | Get the resource details
[**listResources**](ResourceManagementApi.md#listResources) | **GET** /organizations/{organization_id}/resources | List Resources
[**updateResource**](ResourceManagementApi.md#updateResource) | **PATCH** /organizations/{organization_id}/resources/{id} | Update a resource


<a name="createResource"></a>
# **createResource**
> Resource createResource(organization\_id, CreateResourceRequest)

Create a resource name in an organization.

    Creates a resource name. Access policies can be associated with the instances of these resources. ex - \\\&quot;Wallet\\\&quot; is a resource name in the organization org - \\\&quot;Org#1\\\&quot; and \\\&quot;wallet#1\\\&quot; is the instance of the resource \\\&quot;Wallet\\\&quot;. Policies on which user to access the wallet#1 can be created by the user having privilege to access the resource.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **CreateResourceRequest** | [**CreateResourceRequest**](../Models/CreateResourceRequest.md)| Payload to create resource |

### Return type

[**Resource**](../Models/Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

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

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getResource"></a>
# **getResource**
> Resource getResource(organization\_id, id)

Get the resource details

    Gets the resource details associated with the organization

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]

### Return type

[**Resource**](../Models/Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listResources"></a>
# **listResources**
> ResourcePaginatedResponse listResources(organization\_id, nextToken, pageSize, sortOrder)

List Resources

    List all the resource names in an organization.

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

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateResource"></a>
# **updateResource**
> Resource updateResource(organization\_id, id, UpdateResourceRequest)

Update a resource

    Update resource name of the organization

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]
 **id** | **String**|  | [default to null]
 **UpdateResourceRequest** | [**UpdateResourceRequest**](../Models/UpdateResourceRequest.md)| Payload to update resource |

### Return type

[**Resource**](../Models/Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

