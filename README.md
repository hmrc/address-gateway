
# address-gateway

`address-gateway` provides api-platform access to `address-insights*` services.

```mermaid
sequenceDiagram
address-gateway->>address-lookup: /lookup <postcode>
address-lookup->>address-search-api: /lookup <postcode>
address-search-api-->>address-lookup: response [address]
address-lookup-->>address-gateway: response [address]
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
