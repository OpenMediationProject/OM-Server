## SDK /wf API

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


* The wf interface is used for SDK to obtain waterfall sequence, return http status 200 for success, non 200 for failure

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
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| pid | int32 | Placement ID | 2345|✔︎|
| iap | float | IAP, inAppPurchase |1|✖︎|
| `imprTimes` | int32 | placementImprTimes The number of times the placement was displayed by the user on that day|5|✖︎|
| act | int8 | Trigger type for loading ads, [1:init,2:interval,3:adclose,4:manual] |3|✔︎|
| bid | Array of [BidPrice](#bidprice) | BidPrice, include C2S & S2S ||✖︎|
| bids2s | Array of [BidderToken](#biddertoken) | S2S HeadBidding BidderTokens ||✖︎|
| ils | Array of [InstanceLoadStatus](#instanceloadstatus) | report Load error returned by AdNetwork||✖︎|

### BidPrice

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| iid | int32 | Instance ID | 0 |✔︎|
| price | float | winPrice | 3.6 | ✔︎ |
| cur | string | Currency Unit, default USD | USD |✖︎|

### BidderToken

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| iid | int32 | Instance ID | 0 |✔︎|
| token | string | AdNetwork BidderToken | | ✔︎ |

### InstanceLoadStatus

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| iid | int32 | Instance ID | 0 |✔︎|
| lts | int32 | The unix-timestamp of the last load initiated | 1567479919 |✔︎|
| dur | int32 | Loading time, in seconds | 10 |✔︎|
| code | string | Load Callback Event Code |1002 |✔︎ |
| msg | string | Load Callback Event Message | Ad was re-loaded too frequently| ✖︎ |


### The returned content is a json + gzip structure.The format of the json data before compression is as follows

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| code | int32 | [RespCode](#respcode)| 0 |✔︎|
| msg | string | ErrorInfo| 0 |✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|
| ins | Array of int32 | Instance ID List | [111,222] | ✔︎ |
| bidresp | Array of [S2SBidResponse](#s2sbidresponse) | S2SBidResponse with Payload | | ✖︎ |

### S2SBidResponse

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| iid | int32 | Instance ID | 0 |✔︎|
| nbr | int32 | NoBidReason Code | 0 | ✖︎ |
| err | string | NoBidReason Msg | | ✖︎ |
| price | float | bidPrice | 3.6 | ✖︎ |
| adm | string | Payload Token | | ✖︎ |
| nurl | string | WinNotice URL | | ✖︎ |
| lurl | string | LossNotice URL | | ✖︎ |
| expire | int | Expire time, in minutes | 30 | ✖︎ |

## RespCode
| code | key | msg |
|---|---|---|
| 0 | OK |  |
| 1 | EMPTY_DID | empty did |
| 10 | PUBLISHER_INVALID | publisher invalid |
| 20 | PUB_APP_INVALID | pub app invalid |
| 40 | WIFI_REQUIRED | wifi required |
| 50 | SDK_VERSION_DENIED | sdk version denied |
| 60 | OSV_DENIED | osv denied |
| 70 | MAKE_DENIED | make denied |
| 80 | BRAND_DENIED | brand denied |
| 90 | MODEL_DENIED | model denied |
| 100 | DID_DENIED | did denied |


* Resp, JSONObject

```json
{
    "code": 0,
    "ins": [123],
    "bidresp": [
        {
            "iid": 123, // Instance ID
            "price": 1.0,
            "adm": "PayloadToken",
            "nurl": "WinNotice URL",
            "lurl": "LossNotice URL"
        }
    ]
}
```


