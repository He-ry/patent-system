# 专利信息管理系统 API


**简介**:专利信息管理系统 API


**HOST**:http://localhost:8080


**联系人**:开发团队


**Version**:1.0.0


**接口路径**:/v3/api-docs/default


[TOC]






# 专利管理


## 添加专利


**接口地址**:`/api/patent/add`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>添加新的专利信息</p>



**请求示例**:


```javascript
{
  "title": "",
  "applicationNumber": "",
  "applicationDate": "",
  "patentType": "",
  "legalStatus": "",
  "college": "",
  "currentAssignee": "",
  "originalAssignee": "",
  "applicationYear": "",
  "ipcMainClass": "",
  "inventors": "",
  "technicalFields": "",
  "ipcClassifications": "",
  "patentValue": "",
  "technicalValue": "",
  "marketValue": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|patentAddRequest|添加专利请求|body|true|PatentAddRequest|PatentAddRequest|
|&emsp;&emsp;title|专利名称||true|string||
|&emsp;&emsp;applicationNumber|申请号||false|string||
|&emsp;&emsp;applicationDate|申请日期||false|string(date-time)||
|&emsp;&emsp;patentType|专利类型||false|string||
|&emsp;&emsp;legalStatus|法律状态||false|string||
|&emsp;&emsp;college|高校||false|string||
|&emsp;&emsp;currentAssignee|当前权利人||false|string||
|&emsp;&emsp;originalAssignee|原始权利人||false|string||
|&emsp;&emsp;applicationYear|申请年份||false|string||
|&emsp;&emsp;ipcMainClass|IPC主分类号||false|string||
|&emsp;&emsp;inventors|发明人（多个用|分隔）||false|string||
|&emsp;&emsp;technicalFields|技术领域（多个用|分隔）||false|string||
|&emsp;&emsp;ipcClassifications|IPC分类号（多个用|分隔）||false|string||
|&emsp;&emsp;patentValue|专利价值||false|string||
|&emsp;&emsp;technicalValue|技术价值||false|string||
|&emsp;&emsp;marketValue|市场价值||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultVoid|
|400|Bad Request|ResultVoid|
|500|Internal Server Error|ResultVoid|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-500**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 删除专利


**接口地址**:`/api/patent/delete/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据ID删除专利</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id|专利ID|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultVoid|
|400|Bad Request|ResultVoid|
|500|Internal Server Error|ResultVoid|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-500**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 导入专利


**接口地址**:`/api/patent/import`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,multipart/form-data`


**响应数据类型**:`*/*`


**接口描述**:<p>通过Excel文件批量导入专利数据</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|file|Excel文件|query|true|file||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultImportResult|
|400|Bad Request|ResultVoid|
|500|Internal Server Error|ResultVoid|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ImportResult|ImportResult|
|&emsp;&emsp;total|总数|integer(int32)||
|&emsp;&emsp;success|成功数|integer(int32)||
|&emsp;&emsp;skipped|跳过数|integer(int32)||
|&emsp;&emsp;duplicates|重复的申请号列表|array|string|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"success": 0,
		"skipped": 0,
		"duplicates": []
	}
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-500**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 获取专利列表


**接口地址**:`/api/patent/list`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询专利列表</p>



**请求示例**:


```javascript
{
  "page": 1,
  "size": 10,
  "keyword": "",
  "patentType": "",
  "legalStatus": "",
  "applicationYear": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|patentQueryRequest|专利查询请求|body|true|PatentQueryRequest|PatentQueryRequest|
|&emsp;&emsp;page|页码||false|integer(int32)||
|&emsp;&emsp;size|每页数量||false|integer(int32)||
|&emsp;&emsp;keyword|关键词搜索||false|string||
|&emsp;&emsp;patentType|专利类型||false|string||
|&emsp;&emsp;legalStatus|法律状态||false|string||
|&emsp;&emsp;applicationYear|申请年份||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultPageResponsePatentInfo|
|400|Bad Request|ResultVoid|
|500|Internal Server Error|ResultVoid|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponsePatentInfo|PageResponsePatentInfo|
|&emsp;&emsp;list|数据列表|array|PatentInfo|
|&emsp;&emsp;&emsp;&emsp;id||string||
|&emsp;&emsp;&emsp;&emsp;serialNumber||string||
|&emsp;&emsp;&emsp;&emsp;title||string||
|&emsp;&emsp;&emsp;&emsp;applicationDate||string||
|&emsp;&emsp;&emsp;&emsp;inventorCount||string||
|&emsp;&emsp;&emsp;&emsp;college||string||
|&emsp;&emsp;&emsp;&emsp;legalStatus||string||
|&emsp;&emsp;&emsp;&emsp;patentType||string||
|&emsp;&emsp;&emsp;&emsp;applicationNumber||string||
|&emsp;&emsp;&emsp;&emsp;grantDate||string||
|&emsp;&emsp;&emsp;&emsp;currentAssignee||string||
|&emsp;&emsp;&emsp;&emsp;originalAssignee||string||
|&emsp;&emsp;&emsp;&emsp;agency||string||
|&emsp;&emsp;&emsp;&emsp;currentAssigneeProvince||string||
|&emsp;&emsp;&emsp;&emsp;originalAssigneeProvince||string||
|&emsp;&emsp;&emsp;&emsp;originalAssigneeType||string||
|&emsp;&emsp;&emsp;&emsp;currentAssigneeType||string||
|&emsp;&emsp;&emsp;&emsp;applicationYear||string||
|&emsp;&emsp;&emsp;&emsp;publicationDate||string||
|&emsp;&emsp;&emsp;&emsp;grantYear||string||
|&emsp;&emsp;&emsp;&emsp;ipcMainClass||string||
|&emsp;&emsp;&emsp;&emsp;ipcMainClassInterpretation||string||
|&emsp;&emsp;&emsp;&emsp;strategicIndustryClassification||string||
|&emsp;&emsp;&emsp;&emsp;applicationFieldClassification||string||
|&emsp;&emsp;&emsp;&emsp;technicalSubjectClassification||string||
|&emsp;&emsp;&emsp;&emsp;expiryDate||string||
|&emsp;&emsp;&emsp;&emsp;citedPatents||string||
|&emsp;&emsp;&emsp;&emsp;citedIn5Years||string||
|&emsp;&emsp;&emsp;&emsp;claimsCount||string||
|&emsp;&emsp;&emsp;&emsp;patentValue||string||
|&emsp;&emsp;&emsp;&emsp;technicalValue||string||
|&emsp;&emsp;&emsp;&emsp;marketValue||string||
|&emsp;&emsp;&emsp;&emsp;transferEffectiveDate||string||
|&emsp;&emsp;&emsp;&emsp;licenseType||string||
|&emsp;&emsp;&emsp;&emsp;licenseCount||string||
|&emsp;&emsp;&emsp;&emsp;licenseEffectiveDate||string||
|&emsp;&emsp;&emsp;&emsp;transferor||string||
|&emsp;&emsp;&emsp;&emsp;transferee||string||
|&emsp;&emsp;&emsp;&emsp;createTime||string||
|&emsp;&emsp;&emsp;&emsp;updateTime||string||
|&emsp;&emsp;total|总数|integer(int64)||
|&emsp;&emsp;page|当前页|integer(int32)||
|&emsp;&emsp;size|每页数量|integer(int32)||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"list": [
			{
				"id": "",
				"serialNumber": "",
				"title": "",
				"applicationDate": "",
				"inventorCount": "",
				"college": "",
				"legalStatus": "",
				"patentType": "",
				"applicationNumber": "",
				"grantDate": "",
				"currentAssignee": "",
				"originalAssignee": "",
				"agency": "",
				"currentAssigneeProvince": "",
				"originalAssigneeProvince": "",
				"originalAssigneeType": "",
				"currentAssigneeType": "",
				"applicationYear": "",
				"publicationDate": "",
				"grantYear": "",
				"ipcMainClass": "",
				"ipcMainClassInterpretation": "",
				"strategicIndustryClassification": "",
				"applicationFieldClassification": "",
				"technicalSubjectClassification": "",
				"expiryDate": "",
				"citedPatents": "",
				"citedIn5Years": "",
				"claimsCount": "",
				"patentValue": "",
				"technicalValue": "",
				"marketValue": "",
				"transferEffectiveDate": "",
				"licenseType": "",
				"licenseCount": "",
				"licenseEffectiveDate": "",
				"transferor": "",
				"transferee": "",
				"createTime": "",
				"updateTime": ""
			}
		],
		"total": 0,
		"page": 0,
		"size": 0
	}
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-500**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 统计专利数量


**接口地址**:`/api/patent/statistics`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>根据指定字段统计专利数量，支持返回指定条数</p>



**请求示例**:


```javascript
{
  "field": "patentType",
  "limit": 10
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|statisticsRequest|统计请求|body|true|StatisticsRequest|StatisticsRequest|
|&emsp;&emsp;field|统计字段||true|string||
|&emsp;&emsp;limit|返回条数限制||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResultListMapStringObject|
|400|Bad Request|ResultVoid|
|500|Internal Server Error|ResultVoid|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": []
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


**响应状态码-500**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```