## SDK /init v1 API

### History
|Version|Description|
|---------|------|
| 1 |  |

### https://om.adtiming.com/init 

* POST 请求, 以下参数拼入url地址, 压缩内容放入 post body

| Name  |  Type	| Description	|Example| Required |
| ------- | -------| ----- | ----| ------ |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1|✔︎|
| sdkv | string | SDK Version Name| 1.0.1 |✔︎|
| k | string | appKey| higNI1z4a5l94D3ucZRn5zNZa00NuDTq|✔︎|

### 请求内容 json + gzip, 压缩前json数据格式如下
* 对于所有参数值只有`0`和`1`的参数, 如果值是`0`, 则不需要上报

| Name | Type | Description | Example | iOS | Android |
| --- | ---| --- | --- | --- | --- |
|...||<a href="SDK_COMMON.md#baserequestfields">BaseRequestFields</a>||✔︎|✔︎|
| btime | int64 | 开机时间点,单位毫秒 | 1567479919643 |✔︎|✔︎|
| ram | int64 | 总RAM大小, 单位B | 3456642 |✔︎|✔︎|
| adns | Array of <a href='#adnetwork'>AdNetwork</a> | 已集成的AdNetwork集合 | |✔︎|✔︎|
| ios| Object of <a href='#ios'>iOS</a> | iOS特有参数| |✔︎|✖︎|
| android| Object of <a href='#android'>Android</a> | Android特有参数| |✖︎|✔︎|


#### iOS

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| idfv | string | iOS IDFV |BBD6E1CD-8C4B-40CB-8A62-4BBC7AFE07D6 |✔︎|
| cpu_type| string | CPU架构 |ARM64_V8|✔︎|
| cpu_64bits| int32 | CPU架构是否64位,0:否,1:是 |1|✔︎|
| cpu_count| int32 | CPU数量 |6︎|✔︎|
| ui_width| int32 | UI宽,非屏幕像素宽 |375|✔︎|
| ui_height| int32 | UI高,非屏幕像素高 |812|✔︎|
| ui_scale| float | ui_screen |3.0|✔︎|
| hardware_name| string | 硬件名称 |D21AP︎|✔︎|
| device_name| string | 设备名称 |Xiaoming De iPhone|✔︎|
| cf_network| string | | ︎758.1.6|✔︎|
| darwin| string | Darwin 版本 |15.0.0 |✔︎|
| ra| string | 网络制式 |CTRadioAccessTechnologyLTE︎|✔︎|
| local_id| string | currentLocale.localeIdentifier |zh_CN︎|✔︎|
| tz_ab| string | systemTimeZone.abbreviation |GMT+8︎|✔︎|
| tdsg| string | 总容量GB |256|✔︎|
| rdsg| string | 可用容量GB|62.18|✔︎|


#### Android 

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| device | string | Build.DEVICE |lteub |✔︎|
| product | string | Build.PRODUCT |a6plteub |✔︎|
| screen_density| int32 | 像素密度. [0,1,2] |2|✔︎|
| screen_size| int32 | 像素密度. [1,2,3,4] |2|✔︎|
| cpu_abi| string |ro.product.cpu.abi |armeabi-v7a|✔︎|
| cpu_abi2| string | ro.product.cpu.abi2|armeabi|✔︎|
| cpu_abilist| string | ro.product.cpu.abilist | |✔︎|
| cpu_abilist32| string | ro.product.cpu.abilist32 | |✔︎|
| cpu_abilist64| string | ro.product.cpu.abilist64 | |✔︎|
| api_level| int32 | Android API Level | 26 |✔︎|
| d_dpi| int32 | DisplayMetrics.densityDpi |420 |✔︎|
| dim_size| int32 |WebViewBridge.getScreenMetrics().size |2|✔︎|
| xdp| string | x像素密度, DisplayMetrics.xdpi |268.941|✔︎|
| ydp| string | y像素密度, DisplayMetrics.ydpi |268.694|✔︎|
| dfpid| string | deviceFingerPrintId, getUniquePsuedoID | |✔︎|
| time_zone| string | TimeZone.getDefault().getID() |Asia/Shanghai︎|✔︎|
| arch| string | ro.arch| |✔︎|
| chipname| string | ro.chipname | |✔︎|
| bridge| string |ro.dalvik.vm.native.bridge | |✔︎|
| nativebridge| string |persist.sys.nativebridge | |✔︎|
| bridge_exec| string |ro.enable.native.bridge.exec | |✔︎|
| isax86_features| string | dalvik.vm.isa.x86.features| |✔︎|
| isa_x86_variant| string | dalvik.vm.isa.x86.variant| |✔︎|
| zygote| string |ro.zygote | |✔︎|
| mock_location| string |ro.allow.mock.location | |✔︎|
| isa_arm| string |ro.dalvik.vm.isa.arm | |✔︎|
| isa_arm_features| string |dalvik.vm.isa.arm.features | |✔︎|
| isa_arm_variant| string |dalvik.vm.isa.arm.variant | |✔︎|
| isa_arm64_features| string |dalvik.vm.isa.arm64.features | |✔︎|
| isa_arm64_variant| string | dalvik.vm.isa.arm64.variant| |✔︎|
| build_user| string |ro.build.user | |✔︎|
| kernel_qemu| string |ro.kernel.qemu | |✔︎|
| hardware| string | ro.hardware| |✔︎|
| sensor_size| int32 | 传感器数量 |18|✔︎|
| sensors| Array of <a href='#sensor'>Sensor</a> |传感器列表||✔︎|
| as| Object of <a href='#appsource'>AppSource</a> |app安装来源统计||✔︎|
| fb_id| string | FacebookID | |✔︎|
| tdm| int32 | 用户硬盘总容量, 单位M (不含SD Card) | 5837498317 |✔︎|


#### AdNetwork

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| id | int32 | <a href="SDK_COMMON.md#adnetwork">AdNetwork ID</a>| 1 | ✔︎ |
| adapterv | string | AdNetwork Adapter Version| 1.0.1 |✔︎|
| sdkv | string | AdNetwork SDK Version | 3.2.1 |✔︎|


#### Sensor

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| sT | int32 | 传感器类型 | 2 | ✔︎ |
| sV | string | | Invensense Inc.|✔︎|
| sVE | Array of float | | [43.411255, 4.421997, 33.135986] |✔︎|
| sVS | Array of float | | [43.411255, 4.421997, 33.135986] |✔︎|
| sN| string| |Invensense Magnetometer|✔︎|

#### AppSource

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| os | int32 | 系统自带安装数量 | 126 | ✔︎ |
| gp | int32 | GooglePlay 安装数量 | 35 |✔︎|
| other | int32 | 非GooglePlay安装数量 | 14 |✔︎|


### 返回内容 json + gzip, 压缩前json数据格式如下

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| d | int8 | debug 模式, 0关, 1开 | 1 | ✔︎ |
| api | Object of <a href="#apis">APIs</a> | API URLs |  | ✔︎ |
| events | Object of <a href="#events">Events</a> | 事件上报设置, 无值无需上报| |✖︎|
| ms | Array of <a href="#mediation">Mediation</a> | Mediation 列表,<br> **SDK初始化和Load时, 不要全部Mediation做初始化, 原则是按需初始化** |  |✔︎|
| pls | Array of <a href="#placement">Placement</a> | Placement 列表 |  |✔︎|

#### APIs

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| hb | string | Headbidding API URL | https://om.adtiming.com/hb |✖︎|
| wf | string | Waterfall API URL | https://om.adtiming.com/wf | ✔︎ |
| lr | string | Load&Ready API URL | https://om.adtiming.com/lr |✖︎ |
| ic | string | Incentivized Callback URL | https://om.adtiming.com/ic |✖︎ |
| iap | string | IAP URL | https://om.adtiming.com/iap |✖︎ |
| er | string | Error API URL(`No need to report errors if url is empty`) | https://om.adtiming.com/er | ✖︎ |

#### Events
* 事件上报设置, 其中 ids 字段表示需要上报的事件集合, 如果为空则不需要上报
* 事件上报有两种触发方式, `定量`(mn)和`定时`(ci). 无论哪种触发, 同时只做一个上报请求.

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| url | string | 上报地址 | https://om.adtiming.com/l | ✔︎ |
| mn | int32 | maxNumOfEventsToUpload 最大打包上报事件数,队列长度达到mn时做一次上报| 5 |✔︎|
| ci | int32 | checkInterval 定时检查事件队列间隔,单位秒,发现队列里有事件就做一次上报| 10 |✔︎|
| ids | Array of <a href="SDK_EVENT.md#eventid" target="_eventid">EventID</a> | 需要上报的EventID列表| [100,101] |✖︎|

#### Mediation

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id| int32 | <a href="SDK_COMMON.md#adnetwork">AdNetwork ID</a> | 1 | ✔︎ |
| n | string | AdNetwork Name| AdMob |✔︎|
| k | string | AdNetwork Key| 1234567 |✔︎|

#### Placement

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Placement ID | 2341 | ✔︎ |
| t | int8 | <a href="SDK_COMMON.md#adtype" target="_blank">AdType</a>| 3 |✔︎|
| main | int8 | Main Placement mark, 0:No, 1:Yes| 1 |✖︎|
| fi | int32 | frequencryInterval 两次load间隔最小值, 单位秒 | 0 |✖︎|
| fc | int32 | frequencryCap 0或无该属性不控制,n代表表示此广告位fu小时内该用户最多展示n次广告<br>(`仅限Banner&Native有效`)| 0 |✖︎|
| fu | int32 | frequencryUnit, 修饰 fc 的时间间隔, 单位小时<br>(`仅限Banner&Native有效`)| 0 |✖︎|
| scenes | Array of <a href="#scene">Scene</a> | Scene 列表||✔︎|
| bs | int32 | batchSize, Instance 分组大小, 组内并行, 组间串行<br>(`仅限Banner&Native有效`)<br>Max Parallel load count. 加载广告 Instance 最大并行加载数 |2|✖︎|
| fo | int8 | Fan Out 开关, 是否开启立即Ready模式 <br>(`仅限Banner&Native有效`)| 1 |✖︎|
| pt | int32 | 加载超时时间, 加载时单个AdNetwork允许的最大时长, 单位秒 | 30 |✔︎|
| cs | int8 | caches 库存大小, 缓存Ready的数量 | 3 |✔︎|
| rf | int32 | refresh, RewardVideo定时自动刷新补库存间隔,单位秒| 30 |✖︎|
| rlw | int32 | reload Waterfall, Banner展示状态下自动刷新间隔,单位秒|60 |✖︎|
| hb | int8 | HeadBidding 开关, 0:关,1:开| 1 |✖︎|
| ins | Array of <a href="#instance">Instance</a> | Instance 列表||✔︎|

#### Scene

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Scene ID | 0 | ✔︎ |
| n | string | Scene Name| Default |✔︎|
| isd | int8 | isDefault 是否是默认Scene| 1 |✖︎|
| fc | int32 | frequencry_cap 0或无该属性不控制,n代表表示此广告位fu小时内该用户最多展示n次广告| 0 |✖︎|
| fu | int32 | frequencry_unit, 修饰 fc 的时间间隔, 单位小时| 0 |✖︎|

#### Instance

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Instance ID | 2341 | ✔︎ |
| m | int32 | <a href="SDK_COMMON.md#adnetwork">AdNetwork ID</a>| 2 |✔︎|
| k | string | Mediation Placement ID/Key/ZoneId|  |✔︎|
| fc | int32 | frequencry_cap 0或无该属性不控制，n代表表示此广告位fu小时内该用户最多展示n次广告| 0 |✖︎|
| fu | int32 | frequencry_unit, 修饰 fc 的时间间隔, 单位小时| 0 |✖︎|
| fi | int32 | frequencry_interval 两次load间隔最小值, 单位秒| 2 |✖︎|
| hb | int8 | HeadBidding 开关, 0:关,1:开| 1 |✖︎|
| hbt | int32 | HeadBidding 超时时长,单位毫秒; 注:`如果值小于1000, 默认取5000`| 3000 |✖︎|


#### Response JSON Example

```json
{
  "d": 1,

  "events": {
    "url": "https://sdk.adtiming.com/l",
    "mn": 10,
    "ci": 10, 
    "ids": [100, 101, 102, 103, 110, 111, 112, 113, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 360, 361, 362, 363, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 500, 501, 502, 503, 600, 601, 602, 603, 604, 605, 700, 701, 702, 703, 704, 705, 706, 707]
   },


  "ms": [{
    "id": 1,
    "n": "AdMob",
    "k": "1234567"
  }],

  "pls": [{
    "id": 2341,
    "t": 1,
    "dm": 1,
    "vd": 0,
    "mi": 10,
    "ii": 10,
    "mk": 1,
    "vid": 0,
    "fi": 0,
    "bs": 3,
    "pt": 15,
    "fo": 1,
    "mpc": 2,
    "cs": 2,
    "rf": 30,
    "rlw": 20,

    "scenes": [{
      "id": 4567,
      "n": "Default",
      "isd": 1,
      "fu": 1,
      "fc": 10
    }],

    "ins": [{
      "id": 4567,
      "m": 2,
      "k": "aaaaa",
      "fc": 0,
      "fu": 0,
      "fi": 0
    }]
  
  }]
}
```
