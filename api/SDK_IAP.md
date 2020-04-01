## SDK iap 接口文档

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


接口用于 app内付费信息上报

### POST 请求, 以下参数拼入url地址, 加密内容放入post body

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|
| k | string | appKey| higNI1z4a5l94D3ucZRn5zNZa00NuDTq|✔︎|

### 请求内容 json + gzip 结构, 压缩前json数据格式如下

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||<a href="SDK_COMMON.md#baserequestfields">BaseRequestFields</a>||✔︎|
| cur | string | Currency Unit | USD |✖︎|
| iap | float | iap金额 | 2.5 |✔︎|
| iapt | float | iap总金额 | 312.8 |✔︎|


### 返回内容 json + gzip, 压缩前json数据格式如下

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| iapUsd | float | iap总金额,单位USD | 315.9 | ✔︎ |



