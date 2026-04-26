const express = require('express');
const puppeteer = require('puppeteer');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3001;
const OUTPUT_DIR = process.env.OUTPUT_DIR || '../reports/charts';

app.use(cors());
app.use(express.json({ limit: '10mb' }));

let browser = null;

async function initBrowser() {
    if (!browser) {
        browser = await puppeteer.launch({
            headless: 'new',
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-gpu'
            ]
        });
    }
    return browser;
}

const HTML_TEMPLATE = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
    <style>
        * { margin: 0; padding: 0; }
        body { 
            background: #fff; 
            display: flex; 
            justify-content: center; 
            align-items: center;
            min-height: 100vh;
        }
        #chart { 
            width: {{width}}px; 
            height: {{height}}px; 
        }
    </style>
</head>
<body>
    <div id="chart"></div>
    <script>
        const chart = echarts.init(document.getElementById('chart'));
        chart.setOption({{option}});
    </script>
</body>
</html>
`;

function buildEChartsOption(chartType, title, data, options = {}) {
    const colors = [
        '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
        '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48b8d0'
    ];

    const baseOption = {
        color: colors,
        title: {
            text: title,
            left: 'center',
            top: 20,
            textStyle: {
                fontSize: 18,
                fontWeight: 'bold',
                color: '#333'
            }
        },
        tooltip: {
            trigger: chartType === 'pie' ? 'item' : 'axis',
            backgroundColor: 'rgba(50, 50, 50, 0.9)',
            borderColor: '#333',
            textStyle: { color: '#fff' }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '15%',
            top: '15%',
            containLabel: true
        }
    };

    if (!data || data.length === 0) {
        return baseOption;
    }

    const keys = Object.keys(data[0]);
    const categoryKey = keys.find(k => !isValueKey(k)) || keys[0];
    const valueKey = keys.find(k => isValueKey(k)) || keys[1];

    const categories = data.map(row => formatCategory(row[categoryKey]));
    const values = data.map(row => extractNumber(row[valueKey]));

    switch (chartType) {
        case 'bar':
            return {
                ...baseOption,
                xAxis: {
                    type: 'category',
                    data: categories,
                    axisLabel: {
                        rotate: categories.some(c => c.length > 4) ? 30 : 0,
                        color: '#666'
                    },
                    axisLine: { lineStyle: { color: '#ddd' } }
                },
                yAxis: {
                    type: 'value',
                    axisLine: { show: false },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                series: [{
                    type: 'bar',
                    data: values,
                    barMaxWidth: 60,
                    itemStyle: {
                        borderRadius: [4, 4, 0, 0]
                    },
                    label: {
                        show: true,
                        position: 'top',
                        color: '#666'
                    }
                }]
            };

        case 'horizontal_bar':
            return {
                ...baseOption,
                xAxis: {
                    type: 'value',
                    axisLine: { show: false },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                yAxis: {
                    type: 'category',
                    data: categories.reverse(),
                    axisLabel: { color: '#666' },
                    axisLine: { lineStyle: { color: '#ddd' } }
                },
                series: [{
                    type: 'bar',
                    data: values.reverse(),
                    barMaxWidth: 40,
                    itemStyle: {
                        borderRadius: [0, 4, 4, 0]
                    },
                    label: {
                        show: true,
                        position: 'right',
                        color: '#666'
                    }
                }]
            };

        case 'line':
            return {
                ...baseOption,
                xAxis: {
                    type: 'category',
                    data: categories,
                    axisLabel: { color: '#666' },
                    axisLine: { lineStyle: { color: '#ddd' } },
                    boundaryGap: false
                },
                yAxis: {
                    type: 'value',
                    axisLine: { show: false },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                series: [{
                    type: 'line',
                    data: values,
                    smooth: true,
                    symbol: 'circle',
                    symbolSize: 8,
                    lineStyle: { width: 3 },
                    areaStyle: {
                        color: {
                            type: 'linear',
                            x: 0, y: 0, x2: 0, y2: 1,
                            colorStops: [
                                { offset: 0, color: 'rgba(84, 112, 198, 0.3)' },
                                { offset: 1, color: 'rgba(84, 112, 198, 0.05)' }
                            ]
                        }
                    },
                    label: {
                        show: true,
                        position: 'top',
                        color: '#666'
                    }
                }]
            };

        case 'pie':
            const pieData = data.map(row => ({
                name: formatCategory(row[categoryKey]),
                value: extractNumber(row[valueKey])
            }));
            return {
                ...baseOption,
                tooltip: {
                    trigger: 'item',
                    formatter: '{b}: {c} ({d}%)'
                },
                legend: {
                    orient: 'vertical',
                    left: 'left',
                    top: 'middle'
                },
                series: [{
                    type: 'pie',
                    radius: ['40%', '70%'],
                    center: ['60%', '50%'],
                    data: pieData,
                    itemStyle: {
                        borderRadius: 6,
                        borderColor: '#fff',
                        borderWidth: 2
                    },
                    label: {
                        show: true,
                        formatter: '{b}\\n{d}%',
                        color: '#666'
                    },
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0,
                            shadowColor: 'rgba(0, 0, 0, 0.3)'
                        }
                    }
                }]
            };

        case 'scatter':
            return {
                ...baseOption,
                xAxis: {
                    type: 'value',
                    axisLine: { lineStyle: { color: '#ddd' } },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                yAxis: {
                    type: 'value',
                    axisLine: { show: false },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                series: [{
                    type: 'scatter',
                    data: data.map(row => [extractNumber(row[keys[0]]), extractNumber(row[keys[1]])]),
                    symbolSize: 12,
                    itemStyle: {
                        opacity: 0.7
                    }
                }]
            };

        default:
            return {
                ...baseOption,
                xAxis: {
                    type: 'category',
                    data: categories,
                    axisLabel: { color: '#666' }
                },
                yAxis: {
                    type: 'value',
                    axisLine: { show: false },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                series: [{
                    type: 'bar',
                    data: values,
                    barMaxWidth: 60,
                    itemStyle: { borderRadius: [4, 4, 0, 0] },
                    label: { show: true, position: 'top', color: '#666' }
                }]
            };
    }
}

function isValueKey(key) {
    const lower = key.toLowerCase();
    return lower.includes('count') || lower.includes('数量') ||
           lower.includes('value') || lower.includes('total') ||
           lower === 'cnt' || lower === 'num';
}

function formatCategory(value) {
    if (value == null) return '';
    let str = String(value).trim();
    if (str.length > 15) {
        str = str.substring(0, 15) + '...';
    }
    return str;
}

function extractNumber(value) {
    if (value == null) return 0;
    if (typeof value === 'number') return value;
    try {
        const str = String(value).replace(/[,，]/g, '');
        return parseFloat(str) || 0;
    } catch {
        return 0;
    }
}

app.post('/render', async (req, res) => {
    const { chartType, title, data, width = 900, height = 550, option: customOption } = req.body;

    try {
        const browser = await initBrowser();
        const page = await browser.newPage();

        await page.setViewport({ width: width + 100, height: height + 100, deviceScaleFactor: 2 });

        let option;
        if (customOption) {
            option = customOption;
        } else {
            option = buildEChartsOption(chartType, title, data);
        }

        const html = HTML_TEMPLATE
            .replace('{{width}}', width)
            .replace('{{height}}', height)
            .replace('{{option}}', JSON.stringify(option));

        await page.setContent(html, { waitUntil: 'networkidle0' });

        await page.waitForSelector('#chart canvas', { timeout: 5000 });
        await new Promise(resolve => setTimeout(resolve, 500));

        const chartElement = await page.$('#chart');
        const imageBuffer = await chartElement.screenshot({
            type: 'png',
            omitBackground: true
        });

        await page.close();

        res.set('Content-Type', 'image/png');
        res.send(imageBuffer);

    } catch (error) {
        console.error('Render error:', error);
        res.status(500).json({ error: error.message });
    }
});

app.post('/render-and-save', async (req, res) => {
    const { chartType, title, data, width = 900, height = 550, option: customOption, filename } = req.body;

    try {
        const browser = await initBrowser();
        const page = await browser.newPage();

        await page.setViewport({ width: width + 100, height: height + 100, deviceScaleFactor: 2 });

        let option;
        if (customOption) {
            option = customOption;
        } else {
            option = buildEChartsOption(chartType, title, data);
        }

        const html = HTML_TEMPLATE
            .replace('{{width}}', width)
            .replace('{{height}}', height)
            .replace('{{option}}', JSON.stringify(option));

        await page.setContent(html, { waitUntil: 'networkidle0' });

        await page.waitForSelector('#chart canvas', { timeout: 5000 });
        await new Promise(resolve => setTimeout(resolve, 500));

        const chartElement = await page.$('#chart');
        const imageBuffer = await chartElement.screenshot({
            type: 'png',
            omitBackground: true
        });

        await page.close();

        // Puppeteer v23+ returns Uint8Array, not Buffer
        const pngBuffer = Buffer.isBuffer(imageBuffer) ? imageBuffer : Buffer.from(imageBuffer);
        const outputDir = path.resolve(__dirname, OUTPUT_DIR);
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        const imageFilename = filename || `${Date.now()}-${Math.random().toString(36).substr(2, 9)}.png`;
        const filePath = path.join(outputDir, imageFilename);
        fs.writeFileSync(filePath, pngBuffer);

        res.json({
            success: true,
            filename: imageFilename,
            path: filePath.replace(/\\/g, '/'),
            url: `/reports/charts/${imageFilename}`
        });

    } catch (error) {
        console.error('Render and save error:', error);
        res.status(500).json({ error: error.message });
    }
});

app.post('/render-base64', async (req, res) => {
    const { chartType, title, data, width = 900, height = 550, option: customOption } = req.body;

    try {
        const browser = await initBrowser();
        const page = await browser.newPage();

        await page.setViewport({ width: width + 100, height: height + 100, deviceScaleFactor: 2 });

        let option;
        if (customOption) {
            option = customOption;
        } else {
            option = buildEChartsOption(chartType, title, data);
        }

        const html = HTML_TEMPLATE
            .replace('{{width}}', width)
            .replace('{{height}}', height)
            .replace('{{option}}', JSON.stringify(option));

        await page.setContent(html, { waitUntil: 'networkidle0' });

        await page.waitForSelector('#chart canvas', { timeout: 5000 });
        await new Promise(resolve => setTimeout(resolve, 500));

        const chartElement = await page.$('#chart');
        const imageBuffer = await chartElement.screenshot({
            type: 'png',
            omitBackground: true
        });

        await page.close();

        // Puppeteer v23+ returns Uint8Array, not Buffer — ensure proper base64
        const pngBuffer = Buffer.isBuffer(imageBuffer) ? imageBuffer : Buffer.from(imageBuffer);
        const base64 = pngBuffer.toString('base64');

        res.json({
            success: true,
            base64: base64,
            dataUrl: `data:image/png;base64,${base64}`
        });

    } catch (error) {
        console.error('Render base64 error:', error);
        res.status(500).json({ error: error.message });
    }
});

// Save-rendered endpoint (for Java backend — saves directly to shared charts directory)
app.post('/render-save', async (req, res) => {
    const { title, width = 1200, height = 680, option: customOption, filename } = req.body;

    try {
        const browser = await initBrowser();
        const page = await browser.newPage();

        await page.setViewport({ width: width + 100, height: height + 100, deviceScaleFactor: 2 });

        const html = HTML_TEMPLATE
            .replace('{{width}}', width)
            .replace('{{height}}', height)
            .replace('{{option}}', JSON.stringify(customOption));

        await page.setContent(html, { waitUntil: 'networkidle0' });

        await page.waitForSelector('#chart canvas', { timeout: 5000 });
        await new Promise(resolve => setTimeout(resolve, 500));

        const chartElement = await page.$('#chart');
        const imageData = await chartElement.screenshot({
            type: 'png',
            omitBackground: true
        });

        await page.close();

        // Puppeteer v23+ returns Uint8Array — convert to Buffer for writing
        const pngBuffer = Buffer.isBuffer(imageData) ? imageData : Buffer.from(imageData);
        const outputDir = path.resolve(__dirname, OUTPUT_DIR);
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        const imageFilename = filename || `${Date.now()}-${Math.random().toString(36).substr(2, 9)}.png`;
        const filePath = path.join(outputDir, imageFilename);
        fs.writeFileSync(filePath, pngBuffer);

        res.json({
            success: true,
            filename: imageFilename,
            filePath: filePath.replace(/\\/g, '/')
        });

    } catch (error) {
        console.error('Render save error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

process.on('SIGINT', async () => {
    if (browser) {
        await browser.close();
    }
    process.exit(0);
});

app.listen(PORT, () => {
    console.log(`ECharts render server running on port ${PORT}`);
    console.log(`Output directory: ${path.resolve(__dirname, OUTPUT_DIR)}`);
});
