# SDK Cross Promotion /cp/cl API

## API History

|Version|Description|
|----|----|
| 1 |  |

* API is used for sdk to load cross-promotion ads, returning http status 200 for success, non-200 for no filling or failure
* Request header `Content-Type: application/json` is reqiured
* The API accepts request body compression, in either `gzip` or `deflate` format, specified by the request header `Content-Encoding`, which is mandatory in this case.

## POST request, the following parameters are spelled into the url query

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- |---|
| v | int32 | API Version|1| ✔︎|
| plat | int32 | Platform, 0:iOS,1:Android|1|✔︎|
| sdkv | string | SDK Version Name |1.0.1|✔︎|

## Request body JSON

| Name|Type|Description|Example|Required|
| --- | ---| --- | --- | --- |
|...||[BaseRequestFields](SDK_COMMON.md#baserequestfields)||✔︎|
| pid | int32 | PlacementID | 2345|✔︎|
| iid | int32 | InstanceID | 1234|✔︎|
| iap | float | inAppPurchase |1|✖︎|
| imprTimes | int32 | The number of impressions of the placement today|5|✖︎|
| ng | int32 | noGooglePlay mark |1|✖︎|
| act | int8 | Trigger type for loading ads, [1:init,2:interval,3:adclose,4:manual] |3|✔︎|

## Response body JSON

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| code | int32 | [ResponseCode](#responsecode)| 0 |✔︎|
| campaigns | Array of [Campaign](#campaign) | campaign 列表,默认只返回一个| |✖︎|

### Campaign

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| cid | string | Campaign ID | 10010 |✔︎|
| crid | string | Creative ID |1111|✔︎|
| title | string | Title | |✖︎|
| imgs | Array of string | image list | |✖︎|
| video | Object of [Video](#video) | Video Object | |✖︎|
| adtype | int8 | [AdType](SDK_COMMON.md#adtype)|2 |✔︎|
| link | string | click through url | https://play.google.com/store/apps/details?id=xxxxxx|✔︎|
| iswv | int8 | Open link with webview, 0:No,1:Yes | 1 |✖︎|
| clktks | Array of string | click track urls |[] |✖︎|
| imptks | Array of string | impression track urls |[] |✔︎|
| app | Object of [App](#app) | App information | |✖︎|
| descn | string | Description | |✖︎|
| resources | Array of string | List of resources to be loaded | |✖︎|
| ps | string | The basic parameter string when the event is reported, used for splicing | |✖︎|
| mk | Object of [AdMark](#admark) | AdMark, no need to display admark if this field is empty| |✖︎|
| expire | int32 | campaign expire time in seconds | |✔︎|
| rt | int8 | Retargeting mark| 1 |✖︎|
| ska | Object of [SkAdNetwork](#skadnetwork) | iOS 14 SkAdNetwork | |✖︎|
| r | float | Revenue, bidPrice or ecpm | 3.6 | ✖︎ |
| rp | int8 | Revenue Precision, 0:undisclosed,1:exact,2:estimated,3:defined | 0 | ✖︎ |

#### App

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| id | string | AppID |Android PackageName, iOS numeric ID |✔︎|
| name | string | AppName| 1 |✔︎|
| icon | string | AppIcon URL |https://cdn.openmediation.com/xxx.jpg|✖︎|
| rating | float | AppRating, 评分 |4.8 |✖︎|

#### Video

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| url | string | Video url | https://cdn.openmediation.com/a.mp4 |✔︎|
| dur | int32 | Video Duration |30|✖︎|
| skip | int8 | Indicates if the player will allow the video to be skipped, where 0 = no, 1 = yes. | 0 |✔︎|
| ves | int32 | Object of [VideoEvents](#videoevents) | |✖︎|

#### AdMark

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| logo | string | logo url, 空值时使用默认logo | https://cdn.openmediation.com/cp/logo.png |✖︎|
| link | string | 点击跳转地址, 使用系统浏览器打开; 空值时不跳转. | https://ad.openmediation.com/policy.html |✖︎|

#### SkAdNetwork

| Name | Type | Description | Example | Necessary |
| --- | ---| --- | --- | --- |
| adNetworkPayloadVersion | string | | 2.0 |✔︎|
| adNetworkId | string |  | xxxxxxx.skadnetwork |✔︎|
| adNetworkCampaignId | int8 | [1-100] | 99 |✔︎|
| adNetworkNonce | string | nonce string |a932ocx81vh139|✔︎|
| adNetworkSourceAppStoreIdentifier | string | bundle |123441|✔︎|
| adNetworkImpressionTimestamp | int64 | |1567479919643|✔︎|
| adNetworkAttributionSignature | string | sign ||✔︎|

#### ResponseCode

| Code | Key | Message |
|---|---|---|
| 1 | EMPTY_DID | empty did |
| 10 | PUBLISHER_INVALID | publisher invalid |
| 20 | PUB_APP_INVALID | pub app invalid |
| 30 | SIZE_INVALID | size invalid |
| 40 | WIFI_REQUIRED | wifi required |
| 50 | SDK_VERSION_DENIED | sdk version denied |
| 60 | OSV_DENIED | osv denied |
| 70 | MAKE_DENIED | make denied |
| 80 | BRAND_DENIED | brand denied |
| 90 | MODEL_DENIED | model denied |
| 100 | DID_DENIED | did denied |
| 110 | PERIOD_DENIED | empty did |
| 120 | RANDOM_NOFILL | period denied |
| 120 | RANDOM_NOFILL |random nofill|
| 130 | DEV_CAMPAIGN_LOST | dev_campaign lost|
| 140 | NO_CAMPAIGN_PC | no campaign for plat_country |
| 150 | NO_CAMPAIGN_AVALIABLE | no campaigns avaliable |

* Response body example

```json
{
    "code": 0,
    "campaigns": [{
        "cid": "92887631",
        "crid": "150682719",
        "descn": "Click it, you will be amazed by its music!",
        "title": "To tell you the truth",
        "imgs": [
            "https://cdn.openmediation.com/img/fbe4dcb7c5e3fd9f731e52a6280de52.jpg"
        ],
        "app": {
            "id": "com.cmplay.dancingline",
            "name": "Dancing Line",
            "icon": "https://cdn.openmediation.com/creative/cf65906218b9d765532.png",
            "rating": 4.8,
            "size": 20
        },
        "video": {
            "url": "https://cdn.openmediation.com/video/f93e5e358b151b3afb4286adb6dfe2.mp4",
            "dur": 30
        },
        "adtype": 2,
        "link": "https://play.google.com/store/apps/details?id=xxxxxx",
        "clktks": ["https://xxx.xxx.com/clk?place=1121&s=6&mb=&con_type=2&sdkv=4.6.0&cr="],
        "imptks": ["https://xxx.xxx.com/imp?place=1121&s=6&mb=&con_type=2&sdkv=4.6.0&cr="],
        "resources": [
            "https://cdn.openmediation.com/cp/vd.html",
            "https://cdn.openmediation.com/cp/ec.html"
        ],
        "expire": 7200,
        "ska": {
            "adNetworkPayloadVersion": "2.0",
            "adNetworkId": "xxxxxxxx.skadnetwork",
            "adNetworkCampaignId": 100,
            "adNetworkNonce": "a932ocx81vh139",
            "adNetworkSourceAppStoreIdentifier": "11234414",
            "adNetworkImpressionTimestamp": 1567479919643,
            "adNetworkAttributionSignature": "13dfDH13fzqFEqewr1"
        },
        "mk": {
            "logo": "https://cdn.openmediation.com/cp/logo.png",
            "link": "https://ad.openmediation.com/policy.html"
        },
        "ps": "cy=US&make=&brand=&osv=%3F%3F&reqid=986f5c31-7313-42c0-be42-c33e2c4e4ce8&ts=1542952969566&cid=92887631&crid=150682719&eid=113&did=dbb45b1b-a6a3-4550-aed8-6f907a1114fe&size=300X250&pid=1121&s=6&mb=&contype=2&sdkv=6.6.0"
    }]
}
```
