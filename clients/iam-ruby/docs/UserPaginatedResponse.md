# IamRuby::UserPaginatedResponse

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **data** | [**Array&lt;User&gt;**](User.md) |  | [optional] |
| **next_token** | **String** |  | [optional] |
| **context** | [**PaginationOptions**](PaginationOptions.md) |  | [optional] |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::UserPaginatedResponse.new(
  data: null,
  next_token: null,
  context: null
)
```

