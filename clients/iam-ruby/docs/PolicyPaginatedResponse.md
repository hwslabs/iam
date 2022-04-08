# IamRuby::PolicyPaginatedResponse

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **data** | [**Array&lt;Policy&gt;**](Policy.md) |  | [optional] |
| **next_token** | **String** |  | [optional] |
| **context** | [**PaginationOptions**](PaginationOptions.md) |  | [optional] |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::PolicyPaginatedResponse.new(
  data: null,
  next_token: null,
  context: null
)
```

