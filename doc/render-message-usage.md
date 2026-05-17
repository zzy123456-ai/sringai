# RenderMessage 使用示例

## 1. 流式响应（SSE，步步追加）

### 第 1 块 — 文字提示

```java
RenderMessage msg = RenderMessage.chunk(List.of(
        new TextPart("正在分析销售数据...")
));
```

序列化输出：

```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "parts": [
    {"kind": "text", "text": "正在分析销售数据..."}
  ],
  "streaming": true,
  "done": false
}
```

### 第 2 块 — 追加柱状图

```java
RenderMessage msg = RenderMessage.chunk(List.of(
        new BarChartPart("月度销售额",
                List.of("1月", "2月", "3月", "4月"),
                List.of(12000, 18500, 15200, 21000))
));
```

```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "parts": [
    {
      "kind": "barChart",
      "title": "月度销售额",
      "labels": ["1月", "2月", "3月", "4月"],
      "values": [12000, 18500, 15200, 21000]
    }
  ],
  "streaming": true,
  "done": false
}
```

### 第 3 块 — 追加表格

```java
RenderMessage msg = RenderMessage.chunk(List.of(
        new TablePart(
                List.of("月份", "销量(万)", "同比"),
                List.of(
                        List.of("1月", "120", "+12%"),
                        List.of("2月", "185", "+8%"),
                        List.of("3月", "152", "-3%")
                )
        )
));
```

```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "parts": [
    {
      "kind": "table",
      "headers": ["月份", "销量(万)", "同比"],
      "rows": [
        ["1月", "120", "+12%"],
        ["2月", "185", "+8%"],
        ["3月", "152", "-3%"]
      ]
    }
  ],
  "streaming": true,
  "done": false
}
```

### 第 4 块 — 追加饼图

```java
RenderMessage msg = RenderMessage.chunk(List.of(
        new PieChartPart("渠道占比",
                List.of(new PieItem("线上", 70), new PieItem("线下", 30)))
));
```

```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "parts": [
    {
      "kind": "pieChart",
      "title": "渠道占比",
      "items": [
        {"name": "线上", "value": 70},
        {"name": "线下", "value": 30}
      ]
    }
  ],
  "streaming": true,
  "done": false
}
```

### 第 5 块 — 结束块

```java
RenderMessage msg = RenderMessage.finish(List.of(
        new TextPart("分析完成：2月为销售高峰，线上渠道占比超七成，建议加大线上投放力度。")
));
```

```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "parts": [
    {
      "kind": "text",
      "text": "分析完成：2月为销售高峰，线上渠道占比超七成，建议加大线上投放力度。"
    }
  ],
  "streaming": true,
  "done": true
}
```

---

## 2. 单次完整响应

```java
RenderMessage msg = RenderMessage.complete(List.of(
        new TextPart("分析结果如下："),
        new TablePart(
                List.of("月份", "销量"),
                List.of(List.of("1月", "120"), List.of("2月", "185"))
        ),
        new BarChartPart("月度销售", List.of("1月", "2月"), List.of(120, 185)),
        new PieChartPart("渠道占比", List.of(new PieItem("线上", 70), new PieItem("线下", 30))),
        new TextPart("建议加大线上投放力度。")
));
```

```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "parts": [
    {"kind": "text", "text": "分析结果如下："},
    {
      "kind": "table",
      "headers": ["月份", "销量"],
      "rows": [["1月", "120"], ["2月", "185"]]
    },
    {
      "kind": "barChart",
      "title": "月度销售",
      "labels": ["1月", "2月"],
      "values": [120, 185]
    },
    {
      "kind": "pieChart",
      "title": "渠道占比",
      "items": [{"name": "线上", "value": 70}, {"name": "线下", "value": 30}]
    },
    {"kind": "text", "text": "建议加大线上投放力度。"}
  ],
  "streaming": false,
  "done": true
}
```

---

## 3. 链式构建（推荐）

```java
RenderMessage msg = new RenderMessage()
        .addText("分析结果如下：")
        .addTable(
                List.of("月份", "销量"),
                List.of(List.of("1月", "120"), List.of("2月", "185"))
        )
        .addBarChart("月度趋势", List.of("1月", "2月", "3月"), List.of(120, 185, 152))
        .addPieChart("渠道占比", List.of(new PieItem("线上", 70), new PieItem("线下", 30)))
        .addLineChart("增长趋势", List.of("1月", "2月", "3月"), List.of(10, 50, 80))
        .addImage("https://example.com/chart.png", "趋势图")
        .addText("建议加大线上投放力度。");
msg.setStreaming(false);
msg.setDone(true);
```

---

## 4. 前端 SSE 接收伪代码

```js
const container = document.getElementById('render-area');

eventSource.onmessage = (e) => {
    const msg = JSON.parse(e.data);
    msg.parts.forEach(part => {
        switch(part.kind) {
            case 'text':
                container.appendChild(<TextBlock text={part.text} />);
                break;
            case 'table':
                container.appendChild(<DataTable headers={part.headers} rows={part.rows} />);
                break;
            case 'barChart':
                container.appendChild(<BarChart title={part.title} labels={part.labels} values={part.values} />);
                break;
            case 'pieChart':
                container.appendChild(<PieChart title={part.title} items={part.items} />);
                break;
            case 'lineChart':
                container.appendChild(<LineChart title={part.title} labels={part.labels} values={part.values} />);
                break;
            case 'image':
                container.appendChild(<ImageView src={part.url} alt={part.alt} />);
                break;
        }
    });
    if (msg.done) eventSource.close();
};
```

---

## 5. 各类型字段速查

| kind | 类 | 字段 |
|------|-----|------|
| `text` | `TextPart` | `text: String` |
| `table` | `TablePart` | `headers: List<String>`, `rows: List<List<String>>` |
| `barChart` | `BarChartPart` | `title: String`, `labels: List<String>`, `values: List<Number>` |
| `pieChart` | `PieChartPart` | `title: String`, `items: List<PieItem{name, value}>` |
| `lineChart` | `LineChartPart` | `title: String`, `labels: List<String>`, `values: List<Number>` |
| `image` | `ImagePart` | `url: String`, `alt: String` |

## 6. RenderMessage 便捷方法

| 静态方法 | 说明 |
|----------|------|
| `complete(parts)` | 单次完整响应，`streaming=false, done=true` |
| `chunk(parts)` | 流式中间块，`streaming=true, done=false` |
| `finish(parts)` | 流式结束块，`streaming=true, done=true` |

| 链式方法 | 说明 |
|----------|------|
| `addText(text)` | 添加文字块 |
| `addTable(headers, rows)` | 添加表格 |
| `addBarChart(title, labels, values)` | 添加柱状图 |
| `addPieChart(title, items)` | 添加饼图 |
| `addLineChart(title, labels, values)` | 添加折线图 |
| `addImage(url, alt)` | 添加图片 |
