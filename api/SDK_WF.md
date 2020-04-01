## SDK /wf API

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


* wf接口用于sdk获取waterfall顺序, 返回http 状态 200 为成功, 非200为失败

### POST 请求, 以下参数拼入url地址

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- |---|
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|

### 请求内容 json + gzip + aes 结构, 压缩加密前json数据格式如下
* 对于所有参数值只有`0`和`1`的参数, 如果值是`0`, 则不需要上报

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||<a href="SDK_COMMON.md#baserequestfields">BaseRequestFields</a>||✔︎|
| pid | int32 | 广告位ID | 2345|✔︎|
| iap | float | IAP, inAppPurchase |1|✖︎|
| `imprTimes` | int32 | placementImprTimes 用户当天该广告位展示次数|5|✖︎|
| act | int8 | 加载请求触发类型, [1:init,2:interval,3:adclose,4:manual] |3|✔︎|
| bid | Array of <a href="#biddingresponse">BiddingResponse</a> | HeadBidding Response ||✖︎|

### BiddingResponse

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| iid | int32 | Instance ID | 0 |✔︎|
| price | float | winPrice | 3.6 | ✔︎ |
| cur | string | Currency Unit, default USD | USD |✖︎|


### 返回内容 json + gzip, 压缩前json数据格式如下

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| code | int32 | <a href="#respcode">RespCode</a>| 0 |✔︎|
| msg | string | ErrorInfo| 0 |✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|
| ins | Array of int32 | Instance ID List | [111,222] | ✔︎ |


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
    "ins": []
}
```


