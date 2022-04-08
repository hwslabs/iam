# IamRuby::Policy

## Properties

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **name** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **hrn** | **String** |  |  |
| **version** | **Integer** |  |  |
| **statements** | [**Array&lt;PolicyStatement&gt;**](PolicyStatement.md) |  |  |

## Example

```ruby
require 'iam-ruby'

instance = IamRuby::Policy.new(
  name: null,
  organization_id: null,
  hrn: null,
  version: null,
  statements: null
)
```

