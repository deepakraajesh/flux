## Config

Config is a package to collate all configurations in your
project into a single injectable instance `Config`. It loads
application properties, system properties and other service urls.

### Service Healthchecks
All `System` properties defined with the suffix `.url` are
picked up by `Config`. A property suffixed with `.url` should also
have a `.healthcheck` suffix as another property, eg: 
`remote-service.url` & `remote-service.healthcheck` so the
latter can be used for health-check of that service.

The following http `GET` request can be used to perform
health-check: `/services/healthcheck`.

#### HealthCheck Response
```json
{
    "code": 500,
    "healthCheck": [
        {
            "code": 200,
            "service": "Hagrid",
            "response": "success",
            "healthCheckUrl": "http://search.unbxd.io/monitor"
        },
        {
            "code": 200,
            "service": "gimli",
            "response": "success",
            "healthCheckUrl": "http://gimli.ssdev.unbxd.io/api/getHealthStatus.do"
        },
        {
            "code": 500,
            "service": "consumer",
            "response": "HealthCheck failed for consumer with exception: Failed to connect to localhost/0:0:0:0:0:0:0:1:8339",
            "healthCheckUrl": "http://localhost:8339"
        },
        {
            "code": 200,
            "service": "relevancy",
            "response": "success",
            "healthCheckUrl": "http://relevance-workflow.ssdev.unbxd.io/v1.0/relevancy/live"
        },
        {
            "code": 200,
            "service": "search",
            "response": "success",
            "healthCheckUrl": "http://search.unbxd.io/monitor"
        },
        {
            "code": 302,
            "service": "console-backend",
            "response": "<html><body>You are being <a href=\"https://ss-console-ui.unbxd.io\">redirected</a>.</body></html>",
            "healthCheckUrl": "https://ss-console-nam.unbxd.io/"
        },
        {
            "code": 200,
            "service": "console",
            "response": "success",
            "healthCheckUrl": "https://ss-console-ui.unbxd.io/"
        },
        {
            "code": 200,
            "service": "ssobase",
            "response": "success",
            "healthCheckUrl": "https://ss-console-ui.unbxd.io/"
        },
        {
            "code": 200,
            "service": "feed",
            "response": "success",
            "healthCheckUrl": "http://a6302a7495c6511ea9c560655d5c5dfc-2108947926.us-west-1.elb.amazonaws.com/api/monitor"
        },
        {
            "code": 200,
            "service": "pim",
            "response": "success",
            "healthCheckUrl": "https://ss-pim.unbxd.io/"
        },
        {
            "code": 500,
            "service": "pim-searchapp",
            "response": "HealthCheck failed for pim-searchapp with exception: viper.ssdev.unbxd.io: nodename nor servname provided, or not known",
            "healthCheckUrl": "https://viper.ssdev.unbxd.io/"
        }
    ]
}
```

### Usage
We'd have to bind `ConfigModule` as the the first binding in
the parent `AbstractModule` to be able to inject `Config` as
this is a `guice` package.
