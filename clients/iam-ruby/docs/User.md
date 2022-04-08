# IamRuby::User

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **hrn** | **String** |  |  |
| **username** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **email** | **String** |  |  |
| **phone** | **String** |  |  |
| **login_access** | **Boolean** |  | [optional] |
| **status** | **String** |  |  |
| **created_by** | **String** |  | [optional] |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::User.new(
  hrn: null,
  username: null,
  organization_id: null,
  email: null,
  phone: null,
  login_access: null,
  status: null,
  created_by: null
)
```

