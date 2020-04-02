## SDK /hb API

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


* The hb interface is used by sdk to get the list of Headbidding Instances, and returns http status 200 for success, non-200 for failure

### POST request, the following parameters are spelled into the url address

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- |---|
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|

### The request content is a json + gzip + aes structure.The format of the json data before compression and encryption is as follows
* For parameters whose parameter values are only `0` and` 1`, if the value is `0`, you do not need to report

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||<a href="SDK_COMMON.md#baserequestfields">BaseRequestFields</a>||✔︎|
| pid | int32 | Placement ID | 2345|✔︎|
| iap | float | IAP, inAppPurchase |1|✖︎|
| `imprTimes` | int32 | placementImprTimes The number of times the placement was displayed by the user current day|5|✖︎|
| act | int8 | Trigger type for loading ads, [1:init,2:interval,3:adclose,4:manual] |3|✔︎|


### The returned content is a json + gzip structure.The format of the json data before compression is as follows

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| code | int32 | 0: success, not zero means failure| 0 |✔︎|
| msg | string | ErrorInfo| 0 |✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|
| ins | Array of int32 | Headbidding Instance ID List | [111,222] | ✔︎ |


* Resp, JSONObject

```json
{
    "code": 0,
    "ins": []
}
```


