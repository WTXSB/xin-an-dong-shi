<template>
  <div class="dashboard">
    <el-row :gutter="20" class="statistic-row">
      <el-col :span="6">
        <el-card class="statistic-card">
          <div class="statistic-content">
            <div class="statistic-icon" style="background-color: #e6f7ff;">
              <i class="el-icon-user" style="color: #1890ff;"></i>
            </div>
            <div class="statistic-info">
              <span class="statistic-title">今日识别总量</span>
              <span class="statistic-value">{{ totalRecognitions }}<span class="statistic-unit">次</span></span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="statistic-card">
          <div class="statistic-content">
            <div class="statistic-icon" style="background-color: #f6ffed;">
              <i class="el-icon-smile" style="color: #52c41a;"></i>
            </div>
            <div class="statistic-info">
              <span class="statistic-title">积极情绪</span>
              <span class="statistic-value">{{ positiveEmotions }}<span class="statistic-unit">次</span></span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="statistic-card">
          <div class="statistic-content">
            <div class="statistic-icon" style="background-color: #fff7e6;">
              <i class="el-icon-sad" style="color: #fa8c16;"></i>
            </div>
            <div class="statistic-info">
              <span class="statistic-title">消极情绪</span>
              <span class="statistic-value">{{ negativeEmotions }}<span class="statistic-unit">次</span></span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="statistic-card">
          <div class="statistic-content">
            <div class="statistic-icon" style="background-color: #fff2f0;">
              <i class="el-icon-minus" style="color: #ff4d4f;"></i>
            </div>
            <div class="statistic-info">
              <span class="statistic-title">中性情绪</span>
              <span class="statistic-value">{{ neutralEmotions }}<span class="statistic-unit">次</span></span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="module-card">
          <template #header>
            <span>表情识别趋势 (近10天)</span>
          </template>
          <div ref="trendChart" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="module-card">
          <template #header>
            <span>情绪类型占比分析</span>
          </template>
          <div ref="categoryChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card class="module-card">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>情绪健康度</span>
              <el-tag v-if="consecutiveNegativeDays >= 3" type="danger" effect="dark" size="small">
                <i class="el-icon-warning"></i> 连续{{ consecutiveNegativeDays }}天负面情绪！
              </el-tag>
            </div>
          </template>
          <div ref="healthChart" class="chart-container" style="padding: 10px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="16">
        <el-card class="module-card">
          <template #header>
            <span>表情识别记录</span>
          </template>
          <el-table :data="emotionRecords" height="320" style="width: 100%">
            <el-table-column prop="recognitionTime" label="识别时间" width="160" />
            <el-table-column prop="emotionType" label="情绪类型">
              <template #default="scope">
                <el-tag 
                  :type="getEmotionTagType(scope.row.emotionType)"
                  effect="plain"
                >
                  {{ scope.row.emotionType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="140">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.confidence" 
                  :stroke-width="10" 
                  :show-text="false"
                  :color="scope.row.confidence >= 90 ? '#67c23a' : scope.row.confidence >= 70 ? '#e6a23c' : '#f56c6c'"
                />
                <span style="margin-left: 8px; font-size: 13px; font-weight: bold;">{{ scope.row.confidence }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="识别来源" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      title="⚠️ 情绪健康预警"
      v-model="warningVisible"
      width="420px"
      center
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div style="text-align: center; padding: 20px; font-size: 18px;">
        <i class="el-icon-warning" style="font-size: 60px; color: #ff4d4f; margin-bottom: 20px;"></i>
        <p><strong>检测到连续 {{ consecutiveNegativeDays }} 天</strong></p>
        <p>负面情绪（愤怒、悲伤等）占主导！</p>
        <p style="margin-top: 15px; color: #666;">建议及时寻求心理咨询或与亲友倾诉</p>
      </div>
      <template #footer>
        <el-button type="danger" @click="warningVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart, PieChart, RadarChart } from 'echarts/charts';
import { 
  TitleComponent, 
  TooltipComponent, 
  GridComponent, 
  LegendComponent, 
  RadarComponent 
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import request from '/@/utils/request'; // 确保路径正确
import { ElMessage } from 'element-plus';

echarts.use([
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  LineChart, PieChart, RadarChart, RadarComponent, CanvasRenderer
]);

// 统计数据
const totalRecognitions = ref(0);
const positiveEmotions = ref(0);
const negativeEmotions = ref(0);
const neutralEmotions = ref(0);

// 图表 DOM 引用
const trendChart = ref(null);
const categoryChart = ref(null);
const healthChart = ref(null);

// ECharts 实例
let trendChartInstance = null;
let categoryChartInstance = null;
let healthChartInstance = null;

// 数据变量
const dates = ref([]);
const totalData = ref([]);
const positiveData = ref([]);
const negativeData = ref([]);
const emotionData = ref([]);
const consecutiveNegativeDays = ref(0);
const warningVisible = ref(false);

const emotionRecords = ref([]);
const refreshTimer = ref(null);

const params = {
  search: '',
  pageNum: 1,
  pageSize: 10,
};

const getEmotionTagType = (emotionType) => {
  const map = {
    '高兴': 'success',
    '中性': '',
    '悲伤': 'info',
    '愤怒': 'danger',
    '厌恶': 'danger',
    '恐惧': 'warning',
    '惊讶': 'warning'
  };
  return map[emotionType] || 'info';
};

// 获取趋势数据
const getTrendData = async () => {
  try {
    const res = await request.get('/api/emotion/recent?days=10');
    if (res.code === '0') {
      dates.value = res.data.dates || [];
      totalData.value = res.data.totalData || [];
      positiveData.value = res.data.positiveData || [];
      negativeData.value = res.data.negativeData || [];

      // 计算连续负面天数
      let consecutive = 0;
      const len = Math.min(negativeData.value.length, positiveData.value.length);
      // 从最新的一天(数组末尾)往前倒推
      for (let i = len - 1; i >= 0; i--) {
        if (negativeData.value[i] > positiveData.value[i]) {
          consecutive++;
        } else {
          break;
        }
      }
      consecutiveNegativeDays.value = consecutive;

      if (consecutive >= 3 && !warningVisible.value) {
        warningVisible.value = true;
        ElMessage.error(`连续 ${consecutive} 天负面情绪占主导！`);
      }

      updateTrendChart();
      updateHealthChart(); // 趋势数据更新后也更新雷达图
    }
  } catch (e) { 
    console.error("获取趋势失败", e); 
  }
};

// 获取分类占比数据
const getEmotionData = async () => {
  try {
    const res = await request.get('/api/emotion/categories');
    if (res.code === '0') {
      // 确保数据格式正确
      if (res.data && res.data.emotionData) {
        emotionData.value = res.data.emotionData.map(item => ({ 
          name: item.emotion, 
          value: item.count 
        }));
        updateCategoryChart();
      }
    }
  } catch (e) { 
    console.error("获取分类失败", e); 
  }
};

// 获取今日统计
const getTodayStatistics = async () => {
  try {
    const res = await request.get('/api/emotion/today');
    if (res.code === '0') {
      totalRecognitions.value = res.data.total_recognitions || 0;
      positiveEmotions.value = res.data.positive_emotions || 0;
      negativeEmotions.value = res.data.negative_emotions || 0;
      neutralEmotions.value = res.data.neutral_emotions || 0;
    }
  } catch (e) { 
    console.error(e); 
  }
};

// 获取记录列表
const getEmotionRecords = async () => {
  try {
    const res = await request.get('/api/imgRecords', { params: params });
    if (res.code === '0') {
      // 安全处理 records 可能为空的情况
      const records = res.data.records || [];
      emotionRecords.value = records.slice(0, 7).map(record => {
        let emotionType = '未知';
        let confidence = 0;

        try {
           // 解析 JSON，增加容错
           const labels = record.label ? JSON.parse(record.label) : [];
           const confidences = record.confidence ? JSON.parse(record.confidence) : [];
           
           const emotionMap = {
            'happy': '高兴', 'neutral': '中性', 'sad': '悲伤',
            'angry': '愤怒', 'disgust': '厌恶', 'fear': '恐惧', 'surprise': '惊讶'
           };
           
           if(labels.length > 0) {
             const mainEmotion = labels[0];
             emotionType = emotionMap[mainEmotion] || mainEmotion;
           }
           
           if(confidences.length > 0) {
             confidence = Math.round(parseFloat(confidences[0]) * 100);
           }
        } catch(err) {
           console.error("解析记录JSON失败", err);
        }
        
        return {
          id: record.id,
          recognitionTime: record.startTime,
          emotionType: emotionType,
          confidence: confidence,
          source: `${record.username} (${record.kind})`
        };
      });
    }
  } catch (e) { 
    console.error('获取表情识别记录失败:', e); 
  }
};

// 更新趋势图 (折线图)
const updateTrendChart = () => {
  if (!trendChartInstance) return;
  trendChartInstance.setOption({
    // 关键修复：数据更新时，必须同时更新 xAxis.data
    xAxis: {
      data: [...dates.value] // 使用解构确保是纯数组
    },
    legend: { data: ['总识别次数', '积极情绪', '消极情绪'] },
    series: [
      { name: '总识别次数', data: [...totalData.value] },
      { name: '积极情绪', data: [...positiveData.value] },
      { name: '消极情绪', data: [...negativeData.value] } // 颜色在 init 中配置了，这里可以省略
    ]
  });
};

// 更新占比图 (饼图)
const updateCategoryChart = () => {
  if (!categoryChartInstance) return;
  categoryChartInstance.setOption({
    series: [{
      data: [...emotionData.value]
    }]
  });
};

// 更新雷达图
const updateHealthChart = () => {
  if (!healthChartInstance) return;

  const totalPositive = positiveData.value.reduce((a,b)=>a+b, 0);
  const totalNegative = negativeData.value.reduce((a,b)=>a+b, 0);
  
  // 计算情绪健康比例
  let ratio = 50;
  if (totalPositive + totalNegative > 0) {
    ratio = (totalPositive / (totalPositive + totalNegative)) * 100;
  }

  healthChartInstance.setOption({
    series: [{
      data: [{
        // 动态计算雷达图的各项指标
        value: [
          ratio.toFixed(0),                // 积极情绪
          85,                              // 情绪稳定 (模拟)
          ratio > 60 ? 90 : 60,            // 心理健康
          100 - (totalNegative / Math.max(totalPositive, 1) * 50), // 负面控制
          ratio                            // 幸福感
        ],
        name: '当前情绪状态',
        areaStyle: { 
          color: ratio > 60 ? 'rgba(82,196,26,0.4)' : 'rgba(255,77,79,0.4)' 
        }
      }]
    }]
  });
};

// 初始化图表配置
const initCharts = () => {
  if(trendChart.value) {
    trendChartInstance = echarts.init(trendChart.value);
    trendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['总识别次数', '积极情绪', '消极情绪'], top: 0 },
      grid: { top: 40, left: 10, right: 10, bottom: 10, containLabel: true },
      xAxis: { type: 'category', data: [] }, // 初始为空
      yAxis: { type: 'value' },
      series: [
        { name: '总识别次数', type: 'line', smooth: true, data: [], itemStyle: { color: '#8a2be2' } },
        { name: '积极情绪', type: 'line', smooth: true, data: [], itemStyle: { color: '#52c41a' } },
        { name: '消极情绪', type: 'line', smooth: true, data: [], itemStyle: { color: '#ff4d4f' } }
      ]
    });
  }

  if(categoryChart.value) {
    categoryChartInstance = echarts.init(categoryChart.value);
    categoryChartInstance.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'middle' },
      series: [{
        name: '情绪类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['65%', '50%'],
        data: [], // 初始为空
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
      }]
    });
  }

  if(healthChart.value) {
    healthChartInstance = echarts.init(healthChart.value);
    healthChartInstance.setOption({
      tooltip: {},
      radar: {
        radius: '60%',
        center: ['50%', '55%'],
        indicator: [
          { name: '积极情绪', max: 100 },
          { name: '情绪稳定', max: 100 },
          { name: '心理健康', max: 100 },
          { name: '负面控制', max: 100 },
          { name: '幸福感', max: 100 }
        ],
        axisName: { color: '#666' }
      },
      series: [{
        type: 'radar',
        data: [] // 初始为空
      }]
    });
  }
};

const handleResize = () => {
  trendChartInstance?.resize();
  categoryChartInstance?.resize();
  healthChartInstance?.resize();
};

const refreshAllData = async () => {
  await Promise.all([
    getTrendData(), 
    getEmotionData(), 
    getEmotionRecords(),
    getTodayStatistics()
  ]);
};

onMounted(async () => {
  // 使用 nextTick 确保 DOM 已经渲染完成
  await nextTick();
  initCharts();
  window.addEventListener('resize', handleResize);
  await refreshAllData();
  
  refreshTimer.value = setInterval(refreshAllData, 300000);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if(refreshTimer.value) clearInterval(refreshTimer.value);
  [trendChartInstance, categoryChartInstance, healthChartInstance].forEach(i => i?.dispose());
});
</script>

<style scoped>
.dashboard { background: #f0f2f5; padding: 20px; min-height: 100vh; }
.statistic-row .el-col { margin-bottom: 20px; }
.statistic-card { height: 100px; display: flex; align-items: center; }
.statistic-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); transform: translateY(-2px); transition: all 0.3s; }
.statistic-content { display: flex; align-items: center; width: 100%; padding: 0 20px; }
.statistic-icon { width: 56px; height: 56px; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin-right: 16px; font-size: 24px; flex-shrink: 0; }
.statistic-info { display: flex; flex-direction: column; overflow: hidden; }
.statistic-title { color: #8c8c8c; font-size: 14px; margin-bottom: 4px; white-space: nowrap; }
.statistic-value { font-size: 24px; font-weight: bold; color: #333; white-space: nowrap; }
.statistic-unit { font-size: 12px; color: #8c8c8c; margin-left: 4px; font-weight: normal; }

.chart-container { width: 100%; height: 300px; }
.module-card { margin-bottom: 20px; }
.module-card :deep(.el-card__header) { background: #f8f9fa; font-weight: 600; padding: 12px 20px; }

/* 兼容 Element Plus 的 Dialog 样式 */
:deep(.el-dialog__header) { background: #ff4d4f; padding: 15px; margin-right: 0; }
:deep(.el-dialog__title) { color: white; }
:deep(.el-dialog__close) { color: white; }
</style>