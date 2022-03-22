# OrganizationApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createOrganization**](OrganizationApi.md#createOrganization) | **POST** /organizations | Create an organization
[**deleteOrganization**](OrganizationApi.md#deleteOrganization) | **DELETE** /organizations/{id} | Delete an organization
[**getOrganization**](OrganizationApi.md#getOrganization) | **GET** /organizations/{id} | Get an organization
[**updateOrganization**](OrganizationApi.md#updateOrganization) | **PATCH** /organizations/{id} | Update an organization


<a name="createOrganization"></a>
# **createOrganization**
> Organization createOrganization(CreateOrganizationRequest)

Create an organization

    Create an organization

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **CreateOrganizationRequest** | [**CreateOrganizationRequest**](../Models/CreateOrganizationRequest.md)| Payload to create organization |

### Return type

[**Organization**](../Models/Organization.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteOrganization"></a>
# **deleteOrganization**
> BaseSuccessResponse deleteOrganization(id)

Delete an organization

    Delete an organization

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getOrganization"></a>
# **getOrganization**
> Organization getOrganization(id)

Get an organization

    Get an organization

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]

### Return type

[**Organization**](../Models/Organization.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateOrganization"></a>
# **updateOrganization**
> Organization updateOrganization(id, UpdateOrganizationRequest)

Update an organization

    Update an organization

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]
 **UpdateOrganizationRequest** | [**UpdateOrganizationRequest**](../Models/UpdateOrganizationRequest.md)| Payload to update organization |

### Return type

[**Organization**](../Models/Organization.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

