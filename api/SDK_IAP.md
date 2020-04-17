## SDK iap Interface

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


This interface is used to report in-app payment information

### POST request, the following parameters are spelled into the url address, the encrypted content is put into the post body

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|
| k | string | appKey| higNI1z4a5l94D3ucZRn5zNZa00NuDTq|✔︎|

### The request content is a json + gzip structure.The format of the json data before compression is as follows

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||<a href="SDK_COMMON.md#baserequestfields">BaseRequestFields</a>||✔︎|
| cur | string | Currency Unit | USD |✖︎|
| iap | float | iap amount | 2.5 |✔︎|
| iapt | float | iap total amount | 312.8 |✔︎|


### The returned content is a json + gzip structure.The format of the json data before compression is as follows

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| iapUsd | float | iap total amount in USD | 315.9 | ✔︎ |



