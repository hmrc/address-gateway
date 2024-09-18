# Overview

### Address Insights

This API enables your application to get an opinion of the riskiness of an address. UK addresses are fully supported and some non-UK addresses are also supported.

Given an address, the response will provide:

* Risk score from 0 (no risk) to 100 (high risk)
* Reason providing an indication of why the risk score has been allocated
* Correlation Id - so you can reference the transaction in any feedback

### What and how?
This endpoint checks addresses against a watch list of addresses known or suspected to have been involved in fraudulent activity. If the details are present on the list a score of 100 is returned. Otherwise, we return 0.

This logic will change and become more refined as more checks are added.

### How to use the results
The intended consumers of this API are services that need to check addresses for fraudulent behaviour.

### Request\response details

All requests must include a uniquely identifiable `user-agent` header. Please contact us for assistance when first connecting.  

* POST /insights
    * [Request](request-sample.json) ([schema](risk-insights-request.json))
    * [Response](insights-response-sample.json) ([schema](risk-response.json))
