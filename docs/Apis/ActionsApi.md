# ActionsApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**listActions**](ActionsApi.md#listActions) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions | List actions


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

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

