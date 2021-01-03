# Ruby Conventions Plugin

Custom Ruby conventions for IntelliJ.

Currently:

- Use a custom script to provide Ruby Type Definitions for `Navigate > Related Symbol...`

### Related Ruby Type Definitions provider

Example:

`cat .rubyconventions/go_to_related`
```ruby
#!/usr/bin/env ruby

require 'active_support/core_ext/string'

# get a selection and 
symbol = ENV['RCP_TEXT'].camelize.gsub(/Service|Controller|Repository|Presenter/, '')

# provide related types names
puts symbol
puts symbol + "Service"
puts symbol + "Controller"
puts symbol + "Repository"
puts symbol + "Presenter"
```

### Symbolic Call Type Inference provider

Example:

###### `cat .rubyconventions/symbolic_type_inference`
```bash
#!/usr/bin/env bash

set -Eeuo pipefail
echo $RCP_TEXT | gsed -E 's/(^|_)([a-z])/\U\2/g'
```
