# analytics-wizard
Wizard to provision a new GerritAnalytics stack

# Setup

this plugin is being developed against gerrit master (2.16), thus it relies on `gerrit-plugin-api-2.16-SNAPSHOT`

In order to make `gerrit-plugin-api-2.16-SNAPSHOT` available locally you can run the following from a gerrit checkout dir:

```
./tools/maven/api.sh install
```