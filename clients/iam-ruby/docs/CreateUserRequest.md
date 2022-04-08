# IamRuby::CreateUserRequest

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **username** | **String** |  |  |
| **password_hash** | **String** |  |  |
| **email** | **String** |  |  |
| **phone** | **String** |  | [optional] |
| **status** | **String** |  |  |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::CreateUserRequest.new(
  username: null,
  password_hash: null,
  email: null,
  phone: null,
  status: null
)
```

