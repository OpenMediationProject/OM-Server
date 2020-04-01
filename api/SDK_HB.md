## SDK /hb API

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


* hb 接口用于 sdk 获取 Headbidding Instance 列表, 返回 http 状态 200 为成功, 非200为失败

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


### 返回内容 json + gzip, 压缩前json数据格式如下

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| code | int32 | 为0表示成功, 非0失败 | 0 |✔︎|
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


