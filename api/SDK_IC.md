# SDK ic API

## API History

|Version|Description|
|------|------|
| 1 | API URL from /init Response |

* This API is used for Rewarded Callback
* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## POST request, the following parameters are spelled into the url query

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|

## Request body JSON

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| pid | int32 | Placement ID | 2345|✔︎|
| mid | int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork)| 1|✖︎|
| iid | int32 | InstanceID | 1111|✔︎|
| scene | int32 | sceneID |1123|✖︎|
| content | string | User data |{"uid": "aaaaa"}|✔︎|

* Resp, Empty Body, success with http status code 200
