# SDK iap API

## API History

|Version|Description|
|------|------|
| 1 | API URL from /init Response |

* This interface is used to report in-app payment information
* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## POST request, the following parameters are spelled into the url query

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1|✔︎|
| sdkv | string | SDK Version Name |1.0.1|✔︎|
| k | string | appKey| higNI1z4a5l94D3ucZRn5zNZa00NuDTq|✔︎|

## Request body JSON

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...|| [BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| cur | string | Currency Unit | USD |✖︎|
| iap | float | iap amount | 2.5 |✔︎|
| iapt | float | iap total amount | 312.8 |✔︎|

## Response body JSON

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| iapUsd | float | iap total amount in USD | 315.9 | ✔︎ |
