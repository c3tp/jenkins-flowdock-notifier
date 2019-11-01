# jenkins-flowdock-notifier

A simple Groovy script which enables Flowdock notifications to be sent from a Jenkins Pipeline.  The current Flowdock notification plugin does not have any Pipeline integration--here is a stopgap.

Inspired by and lifted from [this thread and this comment specifically](https://github.com/jenkinsci/flowdock-plugin/issues/24#issuecomment-271784565).

## Prerequisites

The following must be available in your Jenkins installation to use this library:

* Pipeline plugins, of course
* [Pipeline: Shared Groovy Libraries](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Shared+Groovy+Libraries+Plugin) plugin
* An appropriate source code management plugin supported by the above, probably [the GitHub Branch Source](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Branch+Source+Plugin) plugin

## Installation

A Jenkins administrator must install the library as follows:

1. In _Manage Jenkins_ => _Configure System_, find the _Global Pipeline Libraries_ section.
2. Click on _Add_.
3. Specify:
  * Name: `flowdock-notifier`
  * Default version: `master`
4. For retrieval method, assuming GitHub, select _Modern SCM_ and then _GitHub_:
  * Owner: `c3tp`
  * Repository: `jenkins-flowdock-notifier`
5. Click _Save_ at the bottom.

## Use

You must declare use of the library somewhere before use:

```library 'flowdock-notifier'```

Then, call `flowdockNotify` in an appropriate place in your pipeline.  It needs to be a _step_-class block, including and probably most appropriately a _post_ block.  There are three arguments:

1. `this` - passes the script object to the notifier function to provide necessary context
2. `type` - tells the script if you want a flow message or inbox message. Only accepts string `message` or `inbox`
2. A sources API Flow Token.
3. A string containing list of tags (optional).

For example:

```
library 'flowdock-notifier'

pipeline {
    // ...
    post { 
        changed { 
            flowdockNotify this, 'type', 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX', '#test'
        }
    }
}
```

More info:

In order to get a flow_token to a source, you will need to create an application.
* Sources: https://www.flowdock.com/api/authentication#source-token
* Creating an application: https://www.flowdock.com/oauth/applications


