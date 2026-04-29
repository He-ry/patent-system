# 专利系统 API 文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **字符编码**: `UTF-8`

---

## 1. 对话接口

### 1.1 创建对话并发送消息

**POST** `/api/chat`

发送消息并获取流式响应（SSE）。

**请求体**:
```json
{
  "conversationId": "string (可选，不传则创建新对话)",
  "content": "string (用户消息内容)"
}
```

**响应**: Server-Sent Events (SSE)

```
event: content
data: {"type":"content","content":"文本片段"}

event: done
data: {"type":"done","conversationId":"xxx","messageId":"xxx"}

event: error
data: {"type":"error","message":"错误信息"}
```

**前端调用示例**:
```javascript
const eventSource = new EventSource(
  `/api/chat?conversationId=${convId}&content=${encodeURIComponent(content)}`
);

eventSource.addEventListener('content', (e) => {
  const data = JSON.parse(e.data);
  // 追加文本到界面
});

eventSource.addEventListener('done', (e) => {
  const data = JSON.parse(e.data);
  // 对话完成，获取 conversationId 和 messageId
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  // 处理错误
  eventSource.close();
});
```

---

### 1.2 获取对话列表

**GET** `/api/conversations`

**响应**:
```json
[
  {
    "id": "conv-uuid",
    "title": "对话标题",
    "status": "active",
    "summary": "对话摘要",
    "createdAt": "2026-04-18T10:00:00",
    "updatedAt": "2026-04-18T11:00:00"
  }
]
```

---

### 1.3 获取对话详情

**GET** `/api/conversation/{id}`

**响应**:
```json
{
  "id": "conv-uuid",
  "title": "对话标题",
  "status": "active",
  "summary": "对话摘要",
  "createdAt": "2026-04-18T10:00:00",
  "updatedAt": "2026-04-18T11:00:00",
  "messages": [
    {
      "id": "msg-uuid",
      "conversationId": "conv-uuid",
      "role": "user",
      "content": "用户消息内容",
      "messageOrder": 1,
      "likes": 0,
      "dislikes": 0,
      "createdAt": "2026-04-18T10:00:00",
      "references": []
    },
    {
      "id": "msg-uuid-2",
      "conversationId": "conv-uuid",
      "role": "assistant",
      "content": "AI回复内容",
      "messageOrder": 2,
      "likes": 0,
      "dislikes": 0,
      "createdAt": "2026-04-18T10:01:00",
      "references": [
        {
          "id": "ref-uuid",
          "messageId": "msg-uuid-2",
          "docId": "patent-123",
          "docTitle": "专利标题",
          "content": "引用内容",
          "relevanceScore": 0.95
        }
      ]
    }
  ]
}
```

---

### 1.4 删除对话

**DELETE** `/api/conversation/{id}`

**响应**: `200 OK`

---

## 2. 报告接口

### 2.1 查询对话的所有报告

**GET** `/api/report/conversation/{conversationId}`

**响应**:
```json
[
  {
    "id": "report-uuid",
    "title": "发明人排名前十专利分析报告",
    "reportType": "statistical",
    "downloadUrl": "/api/report/download/report-uuid",
    "summary": "本报告包含以下分析内容...",
    "sectionCount": 3,
    "createdAt": "2026-04-18T15:30:00"
  }
]
```

---

### 2.2 查询单个报告详情

**GET** `/api/report/{reportId}`

**响应**:
```json
{
  "id": "report-uuid",
  "title": "发明人排名前十专利分析报告",
  "reportType": "statistical",
  "downloadUrl": "/api/report/download/report-uuid",
  "summary": "本报告包含以下分析内容...",
  "sectionCount": 3,
  "createdAt": "2026-04-18T15:30:00"
}
```

---

### 2.3 下载报告

**GET** `/api/report/download/{reportId}`

**响应**: Word 文件流

**Content-Type**: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`

**Content-Disposition**: `attachment; filename="报告标题.docx"`

**前端调用示例**:
```javascript
// 方式1：直接下载
window.open(`/api/report/download/${reportId}`, '_blank');

// 方式2：使用 fetch 下载
fetch(`/api/report/download/${reportId}`)
  .then(res => res.blob())
  .then(blob => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = '报告.docx';
    a.click();
    window.URL.revokeObjectURL(url);
  });
```

---

### 2.4 删除报告

**DELETE** `/api/report/{reportId}`

**响应**: `200 OK`

---

## 3. 专利接口

### 3.1 获取专利列表

**GET** `/api/patents`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认20 |
| keyword | string | 否 | 关键词搜索 |
| college | string | 否 | 学院筛选 |
| patentType | string | 否 | 专利类型 |
| legalStatus | string | 否 | 法律状态 |
| year | int | 否 | 申请年份 |

**响应**:
```json
{
  "content": [
    {
      "id": "patent-uuid",
      "title": "专利标题",
      "applicationNumber": "CN202310001234.5",
      "applicationYear": 2023,
      "college": "信息学院",
      "legalStatus": "已授权",
      "patentType": "发明专利",
      "patentValue": 250000.00,
      "inventorCount": 4,
      "citedPatents": 15
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

---

### 3.2 获取专利详情

**GET** `/api/patent/{id}`

**响应**:
```json
{
  "id": "patent-uuid",
  "title": "专利标题",
  "applicationNumber": "CN202310001234.5",
  "applicationDate": "2023-01-15",
  "applicationYear": 2023,
  "college": "信息学院",
  "legalStatus": "已授权",
  "patentType": "发明专利",
  "patentValue": 250000.00,
  "technicalValue": 80000.00,
  "marketValue": 170000.00,
  "citedPatents": 15,
  "citedIn5Years": 10,
  "claimsCount": 12,
  "inventorCount": 4,
  "ipcMainClass": "H04L29/06",
  "ipcMainClassInterpretation": "电通信技术",
  "strategicIndustryClassification": "新一代信息技术",
  "applicationFieldClassification": "通信技术",
  "agency": "武汉东喻专利代理事务所",
  "inventors": ["张三", "李四", "王五", "赵六"]
}
```

---

### 3.3 导入专利数据

**POST** `/api/patents/import`

**Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | Excel文件 (.xlsx) |

**响应**:
```json
{
  "success": true,
  "message": "导入成功",
  "importedCount": 100,
  "errorCount": 2,
  "errors": [
    {
      "row": 5,
      "message": "申请号格式错误"
    }
  ]
}
```

---

### 3.4 导出专利数据

**GET** `/api/patents/export`

**查询参数**: 同专利列表查询参数

**响应**: Excel 文件流

**Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

---

## 4. 统计接口

### 4.1 专利趋势统计

**GET** `/api/statistics/trend`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startYear | int | 否 | 起始年份 |
| endYear | int | 否 | 结束年份 |
| college | string | 否 | 学院筛选 |

**响应**:
```json
[
  { "year": 2020, "count": 50 },
  { "year": 2021, "count": 65 },
  { "year": 2022, "count": 80 },
  { "year": 2023, "count": 95 }
]
```

---

### 4.2 学院专利统计

**GET** `/api/statistics/college`

**响应**:
```json
[
  { "college": "信息学院", "count": 150 },
  { "college": "机械学院", "count": 120 },
  { "college": "材料学院", "count": 80 }
]
```

---

### 4.3 专利类型分布

**GET** `/api/statistics/type`

**响应**:
```json
[
  { "type": "发明专利", "count": 200 },
  { "type": "实用新型", "count": 150 },
  { "type": "外观设计", "count": 50 }
]
```

---

## 5. 错误响应

所有接口错误时返回统一格式：

```json
{
  "timestamp": "2026-04-18T15:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "具体错误信息",
  "path": "/api/xxx"
}
```

**常见错误码**:
| 状态码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 6. 对话示例

### 6.1 普通查询

**用户**: "近五年最有价值的专利是什么？"

**AI响应**: 返回专利价值最高的专利信息

---

### 6.2 统计分析

**用户**: "统计发明人排名前十的专利情况"

**AI响应**: 返回统计结果，并生成可下载的分析报告

---

### 6.3 生成报告

**用户**: "请生成近五年专利价值分析报告"

**AI响应**: 
```
报告已生成！

标题：近五年专利价值分析报告
下载链接：/api/report/download/xxx

报告包含 3 个分析章节。
```

**前端处理**:
1. 解析响应内容，提取下载链接
2. 调用 `/api/report/conversation/{conversationId}` 获取报告列表
3. 显示报告下载按钮

---

## 7. 前端集成建议

### 7.1 对话界面

```javascript
// 发送消息
async function sendMessage(conversationId, content) {
  const params = new URLSearchParams();
  if (conversationId) params.append('conversationId', conversationId);
  params.append('content', content);
  
  const eventSource = new EventSource(`/api/chat?${params}`);
  
  let fullContent = '';
  
  return new Promise((resolve, reject) => {
    eventSource.addEventListener('content', (e) => {
      const data = JSON.parse(e.data);
      fullContent += data.content;
      // 更新UI显示
      updateMessageUI(fullContent);
    });
    
    eventSource.addEventListener('done', (e) => {
      const data = JSON.parse(e.data);
      eventSource.close();
      resolve({
        conversationId: data.conversationId,
        messageId: data.messageId,
        content: fullContent
      });
    });
    
    eventSource.addEventListener('error', (e) => {
      eventSource.close();
      reject(e);
    });
  });
}
```

### 7.2 报告下载

```javascript
// 检查是否有新报告
async function checkReports(conversationId) {
  const response = await fetch(`/api/report/conversation/${conversationId}`);
  const reports = await response.json();
  return reports;
}

// 下载报告
function downloadReport(reportId) {
  window.open(`/api/report/download/${reportId}`, '_blank');
}
```

### 7.3 报告提示组件

当检测到对话生成了报告时，显示提示：

```jsx
function MessageWithReport({ message, conversationId }) {
  const [reports, setReports] = useState([]);
  
  useEffect(() => {
    checkReports(conversationId).then(setReports);
  }, [conversationId]);
  
  return (
    <div>
      <div>{message.content}</div>
      {reports.length > 0 && (
        <div className="report-list">
          <h4>📎 生成的报告</h4>
          {reports.map(report => (
            <div key={report.id} className="report-item">
              <span>{report.title}</span>
              <button onClick={() => downloadReport(report.id)}>
                下载
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

---

## 8. 注意事项

1. **SSE连接**: 对话接口使用SSE，需要正确处理流式响应
2. **报告生成**: 报告生成是异步的，在对话完成后调用报告接口获取
3. **文件下载**: 下载接口返回文件流，建议使用 `window.open` 或创建 `<a>` 标签下载
4. **编码**: 所有请求和响应均使用 UTF-8 编码
5. **跨域**: 开发环境需配置 CORS，生产环境建议使用反向代理
