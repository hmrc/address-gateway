
# address-gateway

`address-gateway` provides api-platform access to `address-lookup` and `address-reputation` services.

`/lookup` flow:
```mermaid
sequenceDiagram
address-gateway->>address-lookup: /lookup <postcode>
address-lookup->>address-search-api: /lookup <postcode>
address-search-api-->>address-lookup: response [address]
address-lookup-->>address-gateway: response [address]
```

`/reputation/sa-reg` flow:
```mermaid
sequenceDiagram
address-gateway->>address-reputation: /reputation/sa-reg <postcode>
address-reputation->>address-insights: /reputation/sa-reg <postcode>
address-insights-->>address-reputation: response [insights + reputation]
address-reputation-->>address-gateway: response [insights + reputation]
```

`address-reputation`
```mermaid
sequenceDiagram
address-gateway->>address-reputation: /reputation/sa-reg <address>
address-reputation->>address-insights: /insights <postcode>
address-insights-->>address-reputation: response <address-insights>
address-reputation-->>address-gateway: response <address-reputation>
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
