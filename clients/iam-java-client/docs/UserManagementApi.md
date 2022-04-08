# UserManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createUser**](UserManagementApi.md#createUser) | **POST** /organizations/{organization_id}/users | Create a user
[**deleteUser**](UserManagementApi.md#deleteUser) | **DELETE** /organizations/{organization_id}/users/{user_name} | Delete a User
[**getUser**](UserManagementApi.md#getUser) | **GET** /organizations/{organization_id}/users/{user_name} | Gets a user entity associated with the organization
[**listUsers**](UserManagementApi.md#listUsers) | **GET** /organizations/{organization_id}/users | List users
[**updateUser**](UserManagementApi.md#updateUser) | **PATCH** /organizations/{organization_id}/users/{user_name} | Update a User


<a name="createUser"></a>
# **createUser**
> User createUser(organizationId, createUserRequest)

Create a user

User is an entity which represent a person who is part of the organization or account. This user entity can be created either through user name, password or the user can be federated through an identity provider like Google, Facebook or any SAML 2.0, OIDC identity provider. This is a sign-up api to create a new user in an organization.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserManagementApi apiInstance = new UserManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    CreateUserRequest createUserRequest = new CreateUserRequest(); // CreateUserRequest | Payload to create user
    try {
      User result = apiInstance.createUser(organizationId, createUserRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserManagementApi#createUser");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizationId** | **String**|  |
 **createUserRequest** | [**CreateUserRequest**](CreateUserRequest.md)| Payload to create user |

### Return type

[**User**](User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for CreateUserRequest |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="deleteUser"></a>
# **deleteUser**
> BaseSuccessResponse deleteUser(userName, organizationId)

Delete a User

Delete a User

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserManagementApi apiInstance = new UserManagementApi(defaultClient);
    String userName = "userName_example"; // String | 
    String organizationId = "organizationId_example"; // String | 
    try {
      BaseSuccessResponse result = apiInstance.deleteUser(userName, organizationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserManagementApi#deleteUser");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userName** | **String**|  |
 **organizationId** | **String**|  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="getUser"></a>
# **getUser**
> User getUser(userName, organizationId)

Gets a user entity associated with the organization

Get a User

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserManagementApi apiInstance = new UserManagementApi(defaultClient);
    String userName = "userName_example"; // String | 
    String organizationId = "organizationId_example"; // String | 
    try {
      User result = apiInstance.getUser(userName, organizationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserManagementApi#getUser");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userName** | **String**|  |
 **organizationId** | **String**|  |

### Return type

[**User**](User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for GetUserRequest |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="listUsers"></a>
# **listUsers**
> UserPaginatedResponse listUsers(organizationId, nextToken, pageSize)

List users

List users associated with the organization. This is a pagniated api which returns the list of users with a next page token.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserManagementApi apiInstance = new UserManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String nextToken = "eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ=="; // String | 
    String pageSize = "10"; // String | 
    try {
      UserPaginatedResponse result = apiInstance.listUsers(organizationId, nextToken, pageSize);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserManagementApi#listUsers");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizationId** | **String**|  |
 **nextToken** | **String**|  | [optional]
 **pageSize** | **String**|  | [optional]

### Return type

[**UserPaginatedResponse**](UserPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for list users request |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="updateUser"></a>
# **updateUser**
> User updateUser(userName, organizationId, updateUserRequest)

Update a User

Update a User

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserManagementApi apiInstance = new UserManagementApi(defaultClient);
    String userName = "userName_example"; // String | 
    String organizationId = "organizationId_example"; // String | 
    UpdateUserRequest updateUserRequest = new UpdateUserRequest(); // UpdateUserRequest | Payload to update user
    try {
      User result = apiInstance.updateUser(userName, organizationId, updateUserRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserManagementApi#updateUser");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userName** | **String**|  |
 **organizationId** | **String**|  |
 **updateUserRequest** | [**UpdateUserRequest**](UpdateUserRequest.md)| Payload to update user |

### Return type

[**User**](User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for UpdateUserRequest |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

