# IamRuby::ResourcePaginatedResponse

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **data** | [**Array&lt;Resource&gt;**](Resource.md) |  | [optional] |
| **next_token** | **String** |  | [optional] |
| **context** | [**PaginationOptions**](PaginationOptions.md) |  | [optional] |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::ResourcePaginatedResponse.new(
  data: null,
  next_token: null,
  context: null
)
```

