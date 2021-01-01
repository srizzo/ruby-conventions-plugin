# Ruby Conventions Plugin

## Custom Navigate > Related Symbol... 

#### Ruby Related Types provider:

`cat .rubyconventions/go_to_related`
```ruby
#!/usr/bin/env ruby

require 'active_support/core_ext/string'

puts ENV['RCP_TEXT'].camelize
```
