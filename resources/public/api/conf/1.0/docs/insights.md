# Overview

### Address Insights

This API enables your application to get an assessment of the reputation of an address. UK addresses are fully supported and some non-UK addresses are also supported.

Given an address, the response will provide:

* reputation - an assessment of the reputation and supporting data
* lookbackDays - the window in days that the data should be searched
* correlationId - so you can reference the transaction in any feedback

### How to use the results
The intended consumers of this API are services that need to check addresses for fraudulent behaviour.
