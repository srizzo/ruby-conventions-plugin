# Ruby Conventions Plugin

Custom Ruby conventions on IntelliJ.

Currently:

- Use a custom script to provide References and Usage Searches
- Use a custom script to provide Ruby Type Definitions for `Navigate > Related Symbol...`
- Use a custom script to provide Symbolic Call Type Inference

### Reference Contributor

Example:

`cat .rubyconventions/references`

```bash
#!/usr/bin/env bash

# given an identifier
name="$(echo $RCP_TEXT | gsed -E 's/Service|Controller|Repository|Presenter//g' | gsed -E 's/(^|_)([a-z])/\U\2/g')"

# provide pontential references
echo "${name}Service"
echo "${name}Controller"
echo "${name}Repository"
echo "${name}Presenter"
```
    
### Reference Search

Example:

`cat .rubyconventions/referenced_as`

```ruby
#!/usr/bin/env ruby

require 'active_support/core_ext/string'

# given an class name
# provide pontential references
puts ENV['RCP_TEXT'].delete(':').underscore
```

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

`cat .rubyconventions/symbolic_type_inference`

```bash
#!/usr/bin/env bash

set -Eeuo pipefail
echo $RCP_TEXT | gsed -E 's/(^|_)([a-z])/\U\2/g'
```
