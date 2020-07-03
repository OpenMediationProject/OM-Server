## SDK ic Interface

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


This interface is used for Rewarded Callback

### POST request, the following parameters are spelled into the url address, the encrypted content is put into the post body

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|


### The request content is a json + gzip structure.The format of the json data before compression is as follows

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| pid | int32 | Placement ID | 2345|✔︎|
| mid | int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork)| 1|✖︎|
| iid | int32 | InstanceID | 1111|✔︎|
| scene | int32 | sceneID |1123|✖︎|
| content | string | User data |{"uid": "aaaaa"}|✔︎|


* Resp, Empty Body, success with http status code 200
