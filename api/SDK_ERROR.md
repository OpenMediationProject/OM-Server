## SDK error 接口文档

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


接口用于 错误日志上报

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
| pid | int32 | 广告位ID | 2345|✖︎|
| mid | int32 | <a href="SDK_COMMON.md#adnetwork">AdNetwork ID</a> | 1|✖︎|
| iid | int32 | InstanceID | 1111|✖︎|
| scene | int32 | sceneID |1123|✖︎|
| tag | string | 错误标签 ||✔︎|
| error | string | 错误信息 ||✔︎|


* Resp, 空Body, 以 http 状态码 200 为成功

