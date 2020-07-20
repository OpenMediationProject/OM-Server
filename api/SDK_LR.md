## SDK lr Interface

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


This interface is used to report the counts of sdk load and ready. It returns http status 200 indicates success, non-200 indicates failure. If no body is returned, no resend is required.

### POST request, the following parameters are spelled into the url address, the encrypted content is put into the post body

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|


### The request content is a json + gzip structure.The format of the json data before compression is as follows
* For all parameters whose parameter values are only `0` and` 1`, if the value is `0`, you do not need to report

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| type | int32 | [Type](#type)|3|︎✔︎|
| pid | int32 | Placement ID | 2345|✔︎|
| mid | int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork) | 1|✔︎|
| iid | int32 | InstanceID | 1111|✔︎|
| act | int8 | Trigger type for loading ads, [1:init,2:interval,3:adclose,4:manual] |3|✔︎|
| scene | int32 | sceneID |1123|✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|
| bid | int8 | 是否是Bid相关请求 [0,1] |1|✖︎|

* Resp, Body is empty, Success with http status code=200


#### Type

|Value|Description|
| --- | ---|
| 1 | Init (No need to report, taken from init interface)|
| 2 | TYPE\_WATERFALL\_REQUEST (No need to report, taken from wf interface)|
| 3 | TYPE\_WATERFALL\_FILLED |
| 4 | TYPE\_INSTANCE\_REQUEST |
| 5 | TYPE\_INSTANCE\_FILLED |
| 6 | TYPE\_INSTANCE\_IMPR |
| 7 | TYPE\_INSTANCE\_CLICK |
| 8 | TYPE\_VIDEO\_START |
| 9 | TYPE\_VIDEO\_COMPLETE |
