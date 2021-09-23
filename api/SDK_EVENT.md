# SDK Event API

## API History

|Version|Date|Description|
|:----:|------|------|
| 1 |  |  |
| 1 | 2021-06-02 | 增加太极 uarx 上报事件 630-634 |

* The API is used for sdk event reporting, returning http status 2xx means success, non-2xx means failure,
* Events are packaged and reported and placed in events
* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## POST request, the following parameters are spelled into the url query

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1| ✔︎|
| sdkv | string | SDK Version Name |1.0.1| ✔︎|
| k | string | appKey| higNI1z4a5l94D3ucZRn5zNZa00NuDTq|✔︎|

## Request body JSON

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...|| [BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| events | Array of [Event](#event) | |[]|✔︎|

### Event

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
| reqId| string | AuctionID |BBD6E1CD8C4B40CB8A624BBC7AFE07D6|✖|
| ts | int64 | Client timestamp(millisecond) | 1567479919643 |✔︎|
| eid | int32 | [EventID](#eventid)|100 |✔︎|
| code | string | Error Code, from AdNetwork callback |1001|✖︎|
| msg | string | event message ||✖︎|
| pid | int32 | placementID|1111 |✖︎|
| mid | int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork)|1 |✖︎|
| iid | int32 | Instance ID |2222|✖︎|
| adapterv | string | Adapter Version|3.0.1 |✖︎|
| msdkv | string | AdNetowrk SDK Version |4.2.0 |✖︎|
| scene | int32 | SceneID |0 |✖︎|
| ot | int32 | Orientation, [1:vertical,2:horizontal]|1 |✖︎|
| duration| int32 |消耗时长,单位秒|6|✖︎|
| priority | int32 | instance 加载优先级 |2 |✖︎|
| cs | int32 | caches 库存大小 |3|✖︎|
| bid | int32 | 是否是Bid相关请求 |1|✖︎|
| price | float | BidResponse的价格 或 事件相关的 Revenue / Price |2.1|✖︎|
| cur | string | BidResponse的货币单位|USD|✖︎|
| abt | int32 | ABTest Mode | 0 |✖︎|
| ruleId | int32 | Mediation Rule ID |123|✖︎|
| abtId | int32 | A/B Test ID |0|✖︎|

#### EventID

| Name| ID | Descn|
| --- | ---| ---|
| INIT\_START | 100| 初始化开始 |
| INIT\_COMPLETE| 101| 初始化完成 |
| LOAD | 102 | ALL 加载 |
| DESTROY | 103 | ALL 销毁 |
| INIT\_FAILED| 104| 初始化失败 |
| RE\_INIT\_START| 105|  重新初始化开始 |
| RE\_INIT\_COMPLETE| 106|  重新初始化完成 |
| RE\_INIT\_FAILED| 107|  重新初始化失败 |
| REFRESH\_INTERVAL|110| 定时自动加载开始|
| ATTEMPT\_TO\_BRING\_NEW\_FEED |111| 程序尝试自动加载广告, 区别于LOAD |
| NO\_MORE\_OFFERS|112| 无可用广告|
| AVAILABLE\_FROM\_CACHE |113|缓存池中有可用广告|
| LOAD\_BLOCKED | 114 | ALL 加载无效, LOADING中 |
| APP\_PAUSE | 115 | App 进入后台 |
| APP\_RESUME | 116 | App 进入前台 |
|---| | |
| INSTANCE\_NOT\_FOUND| 200| Instance 缺失|
| INSTANCE\_INIT\_START | 201 | Instance 初始化开始|
| INSTANCE\_INIT\_FAILED | 202 | Instance 初始化失败|
| INSTANCE\_INIT\_SUCCESS |203| Instance 初始化成功|
| INSTANCE\_DESTROY| 204| Instance 销毁|
| INSTANCE\_LOAD | 205 | Instance 加载 |
| INSTANCE\_LOAD\_ERROR | 206 | Instance 加载失败 |
| INSTANCE\_LOAD\_NO_FILL | 207 | Instance 加载无填充|
| INSTANCE\_LOAD\_SUCCESS | 208 | Instance 加载成功|
| INSTANCE\_LOAD\_TIMEOUT| 211| Instance 加载超时|
|---| | |
| INSTANCE\_RELOAD | 260 | Instance 重新加载 |
| INSTANCE\_RELOAD\_ERROR | 261 | Instance 重新加载失败 |
| INSTANCE\_RELOAD\_NO_FILL | 262 | Instance 重新加载无填充 |
| INSTANCE\_RELOAD\_SUCCESS | 263 | Instance 重新加载成功 |
|---| | |
| INSTANCE\_BID\_REQUEST | 270 |HeadBidding Request|
| INSTANCE\_BID\_RESPONSE | 271 |HeadBidding Response|
| INSTANCE\_BID\_FAILED | 272 |HeadBidding Failed |
| INSTANCE\_BID\_WIN | 273 |HeadBidding WinNotice |
| INSTANCE\_BID\_LOSE | 274 |HeadBidding Lose |
| INSTANCE\_PAYLOAD\_REQUEST | 275 |Payload Request |
| INSTANCE\_PAYLOAD\_SUCCESS | 276 |Payload Success |
| INSTANCE\_PAYLOAD\_FAIL | 277 |Payload Fail |
|---| | |
| INSTANCE\_OPENED | 300 | Instance 打开|
| INSTANCE\_CLOSED | 301| Instance 关闭 |
| INSTANCE\_SHOW | 302 | Instance 调用 show|
| INSTANCE\_SHOW\_FAILED | 303 | Instance show 失败|
| INSTANCE\_SHOW\_SUCCESS | 304 | Instance show 成功|
| INSTANCE\_VISIBLE | 305 | Instance 可视|
| INSTANCE\_CLICKED | 306 | Instance 点击|
| INSTANCE\_VIDEO\_START| 307| Video 开始播放|
| INSTANCE\_VIDEO\_COMPLETED| 309| Video 播放完成|
| INSTANCE\_VIDEO\_REWARDED| 310| Video REWAREDE |
| INSTANCE\_PRESENT\_SCREEN | 313| Banner & Native 进入屏幕可见区|
| INSTANCE\_DISMISS\_SCREEN | 314| Banner & Native 离开屏幕可见区 |
| SCENE\_NOT\_FOUND|315|未找到 scene|
|---| | |
|SCENE\_CAPPED|400|Scene 达到 Cap 限制|
|INSTSANCE\_CAPPED|401| Instance 达到 Cap 限制 |
|PLACEMENT\_CAPPED|402| Placement 达到 Cap 限制 |
|SESSION\_CAPPED|403| Session 达到 Cap 限制 |
|---| | |
| CALLED\_LOAD | 500 | 开发者调用 load 方法|
| CALLED\_SHOW | 501 | 开发者调用 show 方法|
| CALLED\_IS\_READY_TRUE | 502 | 开发者调用 isReady 方法 且返回 true|
| CALLED\_IS\_READY_FALSE | 503 |开发者调用 isReady 方法 且返回 false|
| CALLED\_IS\_CAPPED\_TRUE| 504| 开发者调用 isXXXCapped 方法 且返回 true|
| CALLED\_IS\_CAPPED\_FALSE| 505| 开发者调用 isXXXCapped 方法 且返回 false|
|---| | |
| CALLBACK\_LOAD\_SUCCESS | 600 |API回调加载成功|
| CALLBACK\_LOAD\_ERROR | 601 |API回调加载失败|
| CALLBACK\_SHOW\_FAILED | 602 |API回调Show失败|
| CALLBACK\_CLICK | 603 |API回调 Clicked|
| CALLBACK\_LEAVE\_APP | 604 |API回调 离开App|
| CALLBACK\_PRESENT\_SCREEN | 605 |API回调 进入屏幕可见区|
| CALLBACK\_DISMISS\_SCREEN | 606 |API回调 离开屏幕可见区|
| CALLBACK\_SCENE\_CAPPED | 607 |API回调 Scene 达到Cap上限|
| CALLBACK\_REWARDED | 608 |API回调Rewarded |
|---| | |
| REPORT\_UAR_TOP10 | 630 |Firebase 上报 UAR Top10% |
| REPORT\_UAR_TOP20 | 631 |Firebase 上报 UAR Top20% |
| REPORT\_UAR_TOP30 | 632 |Firebase 上报 UAR Top30% |
| REPORT\_UAR_TOP40 | 633 |Firebase 上报 UAR Top40% |
| REPORT\_UAR_TOP50 | 634 |Firebase 上报 UAR Top50% |


* Resp, Body is empty, success with http status code=200

#### 请求示例

```json
{
  "ts": 1567062759669,
  "zo": 480,
  "appk": "9f196da5",
  "uid": "42640ceb-ae31-453e-a4ca-1b0c24a71198",
  "did": "42640ceb-ae31-453e-a4ca-1b0c24a71198",
  "lang": "zh",
  "jb": 1,
  "bundle": "com.ketchsoft.candy.cream.rain",
  "make": "motorola",
  "brand": "Google",
  "model": "Nexus 6",
  "osv": "6.0.1",
  "appv": "1.0",
  "sdkv": "5.5.6",
  "session": "bf0b1176-3a9c-4859-af5b-4ad66a0cd399",
  "contype": 5,
  "carrier": "123456",
  "fm": 17799,
  "battery": 35,
  "events": [
    {
      "ts": 1567062757245,
      "eid": 100
    },
    {
      "ts": 1567062757345,
      "eid": 111,
      "pid": 1639,
      "mid": 1,
      "iid": 3412,
      "adapterv": "1.0.1",
      "msdkv": "3.1.2",
      "priority": 1,
      "cs": 3
    },
    {
      "ts": 1567062757345,
      "eid": 204,
      "pid": 1639,
      "mid": 1,
      "iid": 3412,
      "adapterv": "1.0.1",
      "msdkv": "3.1.2",
      "duration": 2,
      "priority": 1,
      "cs": 3
    },
    {
      "ts": 1567062758133,
      "eid": 207,
      "pid": 1639,
      "mid": 1,
      "iid": 3412,
      "adapterv": "1.0.1",
      "msdkv": "3.1.2",
      "duration": 6,
      "priority": 1,
      "cs": 3
    }
  ]
}
```
