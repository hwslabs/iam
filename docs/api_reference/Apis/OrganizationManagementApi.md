# OrganizationManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createOrganization**](OrganizationManagementApi.md#createOrganization) | **POST** /organizations | Creates an organization.
[**deleteOrganization**](OrganizationManagementApi.md#deleteOrganization) | **DELETE** /organizations/{id} | Delete an organization
[**getOrganization**](OrganizationManagementApi.md#getOrganization) | **GET** /organizations/{id} | Get an organization
[**updateOrganization**](OrganizationManagementApi.md#updateOrganization) | **PATCH** /organizations/{id} | Update an organization


<a name="createOrganization"></a>
# **createOrganization**
> CreateOrganizationResponse createOrganization(CreateOrganizationRequest)

Creates an organization.

    Organization is the top level entity. All resources (like user, actions, policies) are created and managed under an organization. This is a privileged api and only internal applications has access to create an Organization.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **CreateOrganizationRequest** | [**CreateOrganizationRequest**](../Models/CreateOrganizationRequest.md)| Payload to create organization |

### Return type

[**CreateOrganizationResponse**](../Models/CreateOrganizationResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteOrganization"></a>
# **deleteOrganization**
> BaseSuccessResponse deleteOrganization(id)

Delete an organization

    Delete an organization. This is a privileged api and only internal application will have access to delete organization.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getOrganization"></a>
# **getOrganization**
> Organization getOrganization(id)

Get an organization

    Get an organization and the metadata for the given organization.

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**|  | [default to null]

### Return type

[**Organization**](../Models/Organization.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

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

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

