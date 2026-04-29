# 对话接口 API 文档

### 基础信息

- **Base URL**: `/api/chat`
- **Content-Type**: `application/json`

---

## 1. 发送对话消息（SSE 流式）

**接口**: `POST /api/chat/chat`

**Content-Type**: `application/json`

**Accept**: `text/event-stream`

### 请求体

```json
{
  "conversationId": "会话ID，首次对话不传",
  "content": "用户消息内容"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversationId | String | 否 | 会话ID，不传则创建新会话 |
| content | String | 是 | 用户消息内容 |

### 响应

SSE 流式事件，格式如下：

```
data: {"type":"start","conversationId":"xxx"}

data: {"type":"content","text":"根据"}

data: {"type":"content","text":"检索到的"}

data: {"type":"references","references":[...]}

data: {"type":"done","conversationId":"xxx","messageId":"xxx"}
```

### 事件类型

| type | 说明 | 字段 |
|------|------|------|
| start | 会话开始 | conversationId |
| content | 内容片段（逐字返回） | text |
| references | 引用信息 | references |
| done | 对话完成 | conversationId, messageId |
| error | 错误 | error |

### 前端对接示例

```typescript
const response = await fetch('/api/chat/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ conversationId, content })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  
  const text = decoder.decode(value);
  const lines = text.split('\n');
  for (const line of lines) {
    if (line.startsWith('data:')) {
      const json = JSON.parse(line.substring(5));
      switch (json.type) {
        case 'content':
          appendMessageText(json.text);
          break;
        case 'done':
          console.log('对话完成');
          break;
        case 'error':
          console.error('错误:', json.error);
          break;
      }
    }
  }
}
```

---

## 2. 获取会话列表

**接口**: `GET /api/chat/conversations`

### 响应

```json
[
  {
    "id": "uuid",
    "userId": "用户ID",
    "title": "会话标题",
    "status": "active",
    "patentIds": "关联专利ID",
    "summary": "会话摘要",
    "uploadedFiles": "上传文件信息",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:30:00",
    "messageCount": 10
  }
]
```

---

## 3. 获取单个会话详情

**接口**: `GET /api/chat/conversation/{id}`

### 响应

```json
{
  "id": "uuid",
  "title": "会话标题",
  "status": "active",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:30:00",
  "messages": [
    {
      "id": "msg-uuid",
      "conversationId": "conv-uuid",
      "role": "user",
      "content": "用户消息内容",
      "messageOrder": 1,
      "likes": 0,
      "dislikes": 0,
      "createdAt": "2024-01-01T10:00:00",
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
      "createdAt": "2024-01-01T10:00:05",
      "references": [
        {
          "id": "ref-uuid",
          "messageId": "msg-uuid-2",
          "docId": "专利ID",
          "docTitle": "专利标题",
          "content": "引用的专利内容片段",
          "relevanceScore": 0.95
        }
      ]
    }
  ]
}
```

### role 字段值

| 值 | 说明 |
|------|------|
| user | 用户消息 |
| assistant | AI 回复 |

---

## 4. 删除会话

**接口**: `DELETE /api/chat/conversation/{id}`

### 响应

HTTP 200（无返回体）

---

## 5. 手动触发专利索引

**接口**: `POST /api/chat/index/patents`

### 响应

```json
{
  "indexed": 1000,
  "message": "专利索引完成"
}
```

---

## 数据类型定义（TypeScript）

```typescript
// 会话
interface ConversationVO {
  id: string;
  userId?: string;
  title: string;
  status: string;
  patentIds?: string;
  summary?: string;
  uploadedFiles?: string;
  createdAt: string;
  updatedAt: string;
  messages?: MessageVO[];
  messageCount?: number;
}

// 消息
interface MessageVO {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant';
  content: string;
  messageOrder: number;
  likes: number;
  dislikes: number;
  briefSummary?: string;
  createdAt: string;
  references?: MessageReference[];
}

// 消息引用
interface MessageReference {
  id: string;
  messageId: string;
  docId: string;
  docTitle: string;
  content: string;
  relevanceScore: number;
}

// SSE 事件
interface ChatEventVO {
  type: 'start' | 'content' | 'references' | 'done' | 'error';
  conversationId?: string;
  messageId?: string;
  text?: string;
  references?: ReferenceGroup[];
  error?: string;
}

interface ReferenceGroup {
  docId: string;
  docTitle: string;
  items: RefItem[];
  count: number;
}

interface RefItem {
  id: string;
  content: string;
  score: number;
}

// 请求体
interface ChatRequest {
  conversationId?: string;
  content: string;
}
```

---

## 接口汇总

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/chat` | POST | 发送消息（SSE流式） |
| `/api/chat/conversations` | GET | 获取会话列表 |
| `/api/chat/conversation/{id}` | GET | 获取会话详情 |
| `/api/chat/conversation/{id}` | DELETE | 删除会话 |
| `/api/chat/index/patents` | POST | 触发专利索引 |
