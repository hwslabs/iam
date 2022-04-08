# IamRuby::CreateOrganizationResponse

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization** | [**Organization**](Organization.md) |  | [optional] |
| **admin_user_credential** | [**Credential**](Credential.md) |  | [optional] |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::CreateOrganizationResponse.new(
  organization: null,
  admin_user_credential: null
)
```

