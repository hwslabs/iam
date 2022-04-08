# IamRuby::CreateOrganizationRequest

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **name** | **String** |  |  |
| **description** | **String** |  | [optional] |
| **admin_user** | [**AdminUser**](AdminUser.md) |  |  |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::CreateOrganizationRequest.new(
  name: null,
  description: null,
  admin_user: null
)
```

