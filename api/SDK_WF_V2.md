# SDK waterfall API

## API History

|Version|Description|
|------|------|
| 1 | see [WF_V1](SDK_WF.md) |
| 2 | support impression revenue callback|


* API URL from /init Response
* The wf API is used for SDK to obtain waterfall sequence, return http status 200 for success, non 200 for failure
* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## POST request, the following parameters are spelled into the url address

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- |---|
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|

## Request body JSON

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| pid | int32 | Placement ID | 2345|✔︎|
| iap | float | IAP, inAppPurchase |1|✖︎|
| imprTimes | int32 | placementImprTimes The number of times the placement was displayed by the user on that day|5|✖︎|
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

### Response body JSON

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| code | int32 | [RespCode](#respcode)| 0 |✔︎|
| msg | string | ErrorInfo| 0 |✖︎|
| rule | Object of [MediationRule](#mediationrule) | Mediation Rule | |✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|
| ins | Array of [Instance](#instance) | Sorted Instance List with revenue | | ✔︎ |
| bidresp | Array of [S2SBidResponse](#s2sbidresponse) | S2SBidResponse with Payload | | ✖︎ |

#### MediationRule

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Rule ID | 0 |✔︎|
| n | string | Rule Name | testRule|✔︎|
| t | int8 | Rule Type, 0:Auto,1:Manual | 1| ✔︎ |
| i | int32 | Rule Priority, Only available when type is Manual | 0| ✔︎ |

#### Instance

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Instance ID | 0 |✔︎|
| i | int32 | Priority In Rule, 0 for Auto or Bid | 0 | ✔︎ |
| r | float | Revenue, bidPrice or ecpm | 3.6 | ✖︎ |
| rp | int8 | Revenue Precision, 0:undisclosed,1:exact,2:estimated,3:defined | 0 | ✖︎ |

#### S2SBidResponse

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
  "rule": {
    "id": 1012,
    "n": "testRule",
    "t": 1,
    "i": 2
  },
  "ins": [
    {
      "id": 234,
      "i": 2,
      "r": 1.1,
      "rp": 1
    },
    {
      "id": 123,
      "i": 0,
      "r": 1.0,
      "rp": 1
    }
  ],
  "bidresp": [
    {
      "iid": 123,
      "price": 1.0,
      "adm": "PayloadToken",
      "nurl": "WinNotice URL",
      "lurl": "LossNotice URL"
    }
  ]
}
```
