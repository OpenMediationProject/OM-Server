# SDK init API

## History

|Version|Date|Description|
|-----|------|------|
| 1 |  |  |
| 1 | 2021-06-02 | 增加太极 uarx 上报配置 |

* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## https://s.openmediation.com/init

## POST request, the following parameters are spelled into the url query

| Name  |  Type| Description|Example| Required |
| ------- | -------| ----- | ----| ------ |
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1|✔︎|
| sdkv | string | SDK Version Name| 1.0.1 |✔︎|
| k | string | appKey| higNI1z4a5l94D3ucZRn5zNZa00NuDTq|✔︎|

## Request body JSON

* For parameters those values are 0 and 1, if value is 0, no need to report

| Name | Type | Description | Example | iOS | Android |
| --- | ---| --- | --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|✔︎|
| btime | int64 | device boot time, millisecond | 1567479919643 |✔︎|✔︎|
| ram | int64 | total RAM size, Byte | 3456642 |✔︎|✔︎|
| adns | Array of [AdNetwork](#adnetwork) | integrated AdNetworks | |✔︎|✔︎|
| ios| Object of [iOS](#ios) | iOS-specific parameters| |✔︎|✖︎|
| android| Object of [Android](#android) | Android-specific parameters| |✖︎|✔︎|
| tags | Map<string, any>  | App defined Tags, used for rule match| { "sth":[2,6], "gender":1, "age":3 }|✖︎|

### iOS

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| idfv | string | iOS IDFV |BBD6E1CD-8C4B-40CB-8A62-4BBC7AFE07D6 |✔︎|
| cpu_type| string | CPU Arch |ARM64_V8|✔︎|
| cpu_64bits| int32 | CPU Arch 64-bit,0:否,1:是 |1|✔︎|
| cpu_count| int32 | CPU numbers |6︎|✔︎|
| ui_width| int32 | UI width, not with screen pixel |375|✔︎|
| ui_height| int32 | UI height, not with screen pixel |812|✔︎|
| ui_scale| float | ui_screen |3.0|✔︎|
| hardware_name| string | hardware name |D21AP︎|✔︎|
| device_name| string | device name |Xiaoming De iPhone|✔︎|
| cf_network| string | | ︎758.1.6|✔︎|
| darwin| string | Darwin version |15.0.0 |✔︎|
| ra| string | network format |CTRadioAccessTechnologyLTE︎|✔︎|
| local_id| string | currentLocale.localeIdentifier |zh_CN︎|✔︎|
| tz_ab| string | systemTimeZone.abbreviation |GMT+8︎|✔︎|
| tdsg| string | total storage, GB |256|✔︎|
| rdsg| string | available storage, GB|62.18|✔︎|

### Android

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| device | string | Build.DEVICE |lteub |✔︎|
| product | string | Build.PRODUCT |a6plteub |✔︎|
| screen_density| int32 | Pixel density. [0,1,2] |2|✔︎|
| screen_size| int32 | Pixel size. [1,2,3,4] |2|✔︎|
| cpu_abi| string |ro.product.cpu.abi |armeabi-v7a|✔︎|
| cpu_abi2| string | ro.product.cpu.abi2|armeabi|✔︎|
| cpu_abilist| string | ro.product.cpu.abilist | |✔︎|
| cpu_abilist32| string | ro.product.cpu.abilist32 | |✔︎|
| cpu_abilist64| string | ro.product.cpu.abilist64 | |✔︎|
| api_level| int32 | Android API Level | 26 |✔︎|
| d_dpi| int32 | DisplayMetrics.densityDpi |420 |✔︎|
| dim_size| int32 |WebViewBridge.getScreenMetrics().size |2|✔︎|
| xdp| string | x Pixel density, DisplayMetrics.xdpi |268.941|✔︎|
| ydp| string | y Pixel density, DisplayMetrics.ydpi |268.694|✔︎|
| dfpid| string | deviceFingerPrintId, getUniquePsuedoID | |✔︎|
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
| ifgp| int8 | if install from GP | 1 |✔︎|
| tdm| int32 | device total storage capacity, MB (without SD Card) | 5837498317 |✔︎|

### AdNetwork

| Name | Type | Description | Example | Required |
| --- | ---| --- | --- | --- |
| mid | int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork)| 1 | ✔︎ |
| adapterv | string | AdNetwork Adapter Version| 1.0.1 |✔︎|
| msdkv | string | AdNetwork SDK Version | 3.2.1 |✔︎|

## Response body JSON

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| d | int8 | debug model, 0-close, 1-open | 1 | ✔︎ |
| ri | int32 | The next initialization delay time, in minutes| 1440 |✔︎|
| api | Object of [APIs](#apis) | API URLs |  | ✔︎ |
| events | Object of [Events](#events) | events need to report, events with no value have no need to report| |✖︎|
| ms | Array of [Mediation](#mediation) | Mediation list,<br> **when SDK initialization and load. Do not initial all mediations, The principle is to initialize on demand** |  |✔︎|
| pls | Array of [Placement](#placement) | Placement list |  |✔︎|
| ics | int8 | Impression Callback switch, 0-off,1-on | 0 |✔︎|
| uarx | Array of float | UAR report topX, from left to right are top10%,top20%,top30%,top40%,top50% | [5.1, 4.3, 3.2, 2.1, 1.0] |✖︎|

### APIs

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| hb | string | Headbidding API URL | https://om.adtiming.com/hb |✖︎|
| wf | string | Waterfall API URL | https://om.adtiming.com/wf | ✔︎ |
| lr | string | Load&Ready API URL | https://om.adtiming.com/lr |✖︎ |
| ic | string | Incentivized Callback URL | https://om.adtiming.com/ic |✖︎ |
| iap | string | IAP URL | https://om.adtiming.com/iap |✖︎ |
| er | string | Error API URL(`No need to report errors if url is empty`) | https://om.adtiming.com/er | ✖︎ |
| cpcl | string | CrossPromotion Ad | https://om.adtiming.com/cp/cl | ✖︎ |
| cppl | string | CrossPromotion Payload | https://om.adtiming.com/cp/pl | ✖︎ |

### Events

* Event reporting settings, where the ids field indicates the set of events that need to be reported, and if empty, no reporting is required
* There are two triggering methods for event reporting, `quantitative` (mn) and` timing` (ci). No matter which trigger, only one report request is made at the same time.

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| url | string | report url | https://om.adtiming.com/l | ✔︎ |
| mn | int32 | maxNumOfEventsToUpload The maximum number of packet reporting events, once when the queue length reaches mn| 5 |✔︎|
| ci | int32 | checkInterval Check the event queue interval regularly, in seconds, and report it once when there are events in the queue| 10 |✔︎|
| ids | Array of [EventID](SDK_EVENT.md#eventid) | List of EventIDs to be reported| [100,101] |✖︎|
| fids | Array of [EventID](SDK_EVENT.md#eventid) | List of EventIDs to be reported immediately| [103,104] |✖︎|

### Mediation

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id| int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork) | 1 | ✔︎ |
| n | string | AdNetwork Name| ChartboostBid |✔︎|
| nn | string | AdNetwork Nickname| Helium |✖︎|
| k | string | AdNetwork Key| 1234567 |✔︎|

### Placement

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Placement ID | 2341 | ✔︎ |
| n | string | Placement Name | Banner01 | ✔︎ |
| t | int8 | [AdType](SDK_COMMON.md#adtype)| 3 |✔︎|
| main | int8 | Main Placement mark, 0:No, 1:Yes| 1 |✖︎|
| fi | int32 | frequencryInterval, Minimum load interval in seconds | 0 |✖︎|
| fc | int32 | frequencryCap, 0 or None This attribute means not controlled, n means that the user can show up to n ads in this ad placement within <fu> hours<br>(`Only valid for Banner & Native`)| 0 |✖︎|
| fu | int32 | frequencryUnit, Time interval for parameter fc in hours<br>(`Only valid for Banner & Native`)| 0 |✖︎|
| scenes | Array of [Scene](#scene) | Scene list||✔︎|
| bs | int32 | batchSize, Instance group size, means how many instances in a group.<br>(`Only valid for Banner & Native`)<br>Max Parallel load count. |2|✖︎|
| fo | int8 | Fan Out switch, Whether to turn on the Ready mode immediately <br>(`Only valid for Banner & Native`)| 1 |✖︎|
| pt | int32 | Load timeout, the maximum time allowed for a single AdNetwork during loading, in seconds | 30 |✔︎|
| cs | int8 | caches, Inventory size, number of cached Ready | 3 |✔︎|
| rf | int32 | refresh, RewardVideo&Interstitial Timed automatic refresh inventory replenishment interval in seconds| 30 |✖︎|
| rfs | map<int32, int32> | Automatic restocking threshold interval, key: continuous failure times, value: interval seconds | {3: 30, 7: 300, 10: 3600} |✖︎|
| rlw | int32 | reload Waterfall, Banner auto refresh interval in display state, unit is second|60 |✖︎|
| hb | int8 | HeadBidding switch, 0:close,1:open| 1 |✖︎|
| ins | Array of [Instance](#instance)| Instance list||✔︎|

#### Scene

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Scene ID | 0 | ✔︎ |
| n | string | Scene Name| Default |✔︎|
| isd | int8 | isDefault, whether default Scene| 1 |✖︎|
| fc | int32 | frequencry_cap, 0 or no this attribute means no control, n means that this user can show up to n ads at most within <fu> hour| 0 |✖︎|
| fu | int32 | frequencry_unit, Time interval for parameter fc in hours| 0 |✖︎|

#### Instance

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | int32 | Instance ID | 2341 | ✔︎ |
| n | string | Instance Name | AdMobBanner1 | ✔︎ |
| m | int32 | [AdNetwork ID](SDK_COMMON.md#adnetwork)| 2 |✔︎|
| k | string | Mediation Placement ID/Key/ZoneId|  |✔︎|
| fc | int32 | frequencry_cap, 0 or no this attribute means no control, n means that this user can show up to n ads at most within <fu> hour| 0 |✖︎|
| fu | int32 | frequencry_unit, Time interval for parameter fc in hours| 0 |✖︎|
| fi | int32 | frequencry_interval, Minimum load interval in seconds| 2 |✖︎|
| hb | int8 | HeadBidding switch, 0:close,1:open| 1 |✖︎|
| hbt | int32 | HeadBidding Timeout duration in milliseconds; Note: `If the value is less than 1000, the default is 5000`| 3000 |✖︎|

## Response JSON Example

```json
{
  "d": 1,
  "ics": 1, //Impression callback switch is on
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
  }, {
    "id": 17,
    "n": "ChartboostBid",
    "nn": "Helium",
    "k": "xcdq241d"
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
