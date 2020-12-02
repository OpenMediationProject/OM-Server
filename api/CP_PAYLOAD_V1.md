# SKD Cross Promotion playload API

## API History

|Version|Description|
|----|----|
| 1 |  |

* API is used for sdk to payload cross-promotion ads, returning http status 200 for success, non-200 for no filling or failure
* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## POST request, the following parameters are spelled into the url query

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- |---|
| v | int32 | API Version|1| ✔︎|
| plat | int32 | 平台, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | sdk versioin name |5.6.1| ✔︎|

## Request body JSON

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| pid | int32 | PlacementID | 2345|✔︎|
| token | string | payload token | V1_eyJwbGF0Zm9yb... |✔︎|

## Response body is the same as: [CP_CL_V1.md](CP_CL_V1.md#campaign)
