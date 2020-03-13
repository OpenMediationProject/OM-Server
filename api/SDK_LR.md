## SDK lr 接口文档

### API History
|Version|Description|
|------|------|
| 1 | API URL from /init Response |


接口用于sdk Load和Ready的计数上报, 返回http 状态 200 为成功, 非200为失败, 无返回body 失败无需重发

### POST 请求, 以下参数拼入url地址, 加密内容放入post body

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|


### 请求内容 json + gzip 结构, 压缩前json数据格式如下
* 对于所有参数值只有`0`和`1`的参数, 如果值是`0`, 则不需要上报

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||<a href="SDK_COMMON.md#baserequestfields">BaseRequestFields</a>||✔︎|
| type | int32 | <a href="#type">Type</a>|3|︎✔︎|
| pid | int32 | 广告位ID | 2345|✔︎|
| mid | int32 | <a href="SDK_COMMON.md#adnetwork">AdNetwork ID</a> | 1|✔︎|
| iid | int32 | InstanceID | 1111|✔︎|
| act | int8 | 加载请求触发类型, [1:init,2:interval,3:adclose,4:manual] |3|✔︎|
| scene | int32 | sceneID |1123|✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|


* Resp, 空Body, 以 http 状态码 200 为成功


#### Type

|Value|Description|
| --- | ---| 
| 1 | Init (无需上报, 取自 init 接口)|
| 2 | TYPE\_WATERFALL\_REQUEST (无需上报, 取自wf接口)|
| 3 | TYPE\_WATERFALL\_FILLED |
| 4 | TYPE\_INSTANCE\_REQUEST |
| 5 | TYPE\_INSTANCE\_FILLED |
| 6 | TYPE\_INSTANCE\_IMPR |
| 7 | TYPE\_INSTANCE\_CLICK |
| 8 | TYPE\_VIDEO\_START |
| 9 | TYPE\_VIDEO\_COMPLETE |