# IamRuby::ActionPaginatedResponse

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **data** | [**Array&lt;Action&gt;**](Action.md) |  | [optional] |
| **next_token** | **String** |  | [optional] |
| **context** | [**PaginationOptions**](PaginationOptions.md) |  | [optional] |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::ActionPaginatedResponse.new(
  data: null,
  next_token: null,
  context: null
)
```

