import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { GraphCanvasECharts } from './GraphCanvasECharts';
import {
  Activity,
  Database,
  Focus,
  GitBranch,
  Maximize2,
  Minus,
  Move,
  Network,
  RefreshCw,
  Search,
  Sparkles,
  Target,
  Users,
  Wrench,
  Plus,
} from 'lucide-react';

interface GraphNode {
  id: string;
  label: string;
  type: string;
  size?: number;
  properties?: Record<string, unknown>;
}

interface GraphEdge {
  id: string;
  source: string;
  target: string;
  type: string;
  directed?: boolean;
  weight?: number;
  properties?: Record<string, unknown>;
}

interface GraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
  summary?: Record<string, unknown>;
}

interface GraphOverview {
  nodeCounts: Record<string, number>;
  relationshipCounts: Record<string, number>;
  topInventors: Array<{ name: string; value: number }>;
  topTopics: Array<{ name: string; value: number }>;
  topIndustries: Array<{ name: string; value: number }>;
}

interface SearchHit {
  type: string;
  label: string;
  entityKey: string;
}

type ViewMode = 'coInventor' | 'topicCooccurrence' | 'inventor' | 'topic' | 'highValue';
type ActionType = 'full' | 'incremental' | 'analysis' | 'stats';
type Position = { x: number; y: number };
type Viewport = { scale: number; offsetX: number; offsetY: number };

const API_BASE = '/api/graph';
const SVG_WIDTH = 1040;
const SVG_HEIGHT = 760;

const NODE_COLORS: Record<string, string> = {
  Patent: '#0d9488',
  Inventor: '#6366f1',
  TechTopic: '#ea580c',
  IPCInterpretation: '#a855f7',
  IPC: '#7c3aed',
  CPC: '#8b5cf6',
  ApplicationField: '#0284c7',
  StrategicIndustry: '#be123c',
  College: '#64748b',
  AssigneeOrg: '#059669',
  Problem: '#b45309',
  Effect: '#0369a1',
  Unknown: '#94a3b8',
};

const NODE_TYPE_LABELS: Record<string, string> = {
  Patent: '专利',
  Inventor: '发明人',
  TechTopic: '技术主题',
  IPCInterpretation: 'IPC释义',
  IPC: 'IPC',
  CPC: 'CPC',
  ApplicationField: '应用领域',
  StrategicIndustry: '战略产业',
  College: '学院',
  AssigneeOrg: '权利人机构',
  Problem: '技术问题',
  Effect: '技术效果',
  Unknown: '实体',
};

const RELATION_LABELS: Record<string, string> = {
  CO_INVENTS: '共同发明',
  FOCUSES_ON: '聚焦主题',
  CO_OCCURS_WITH: '主题共现',
  MAPS_TO_IPC: '映射 IPC',
  APPLIES_TO: '应用到',
  BELONGS_TO_INDUSTRY_ANALYSIS: '对应产业',
  RESEARCHES_TOPIC: '研究主题',
  OWNS_TOPIC: '拥有主题',
  INVENTED: '发明',
  HAS_TOPIC: '包含主题',
  HAS_IPC: '包含 IPC',
  HAS_CPC: '包含 CPC',
  HAS_APPLICATION_FIELD: '包含应用领域',
  BELONGS_TO_INDUSTRY: '归属产业',
  BELONGS_TO_COLLEGE: '归属学院',
  ASSIGNED_TO_CURRENT: '当前权利人',
  ASSIGNED_TO_ORIGINAL: '原始权利人',
  TRANSFER_FROM: '转让方',
  TRANSFER_TO: '受让方',
  SOLVES: '解决问题',
  ACHIEVES: '实现效果',
};

const PROPERTY_LABELS: Record<string, string> = {
  name: '名称',
  patentCount: '专利数',
  grantedCount: '授权量',
  highValuePatentCount: '高价值专利',
  avgPatentValue: '平均价值',
  title: '标题',
  patentType: '专利类型',
  legalStatus: '法律状态',
  applicationYear: '申请年',
  grantYear: '授权年',
  patentValue: '专利价值',
  technicalValue: '技术价值',
  marketValue: '市场价值',
  citedPatents: '被引用数',
  claimsCount: '权利要求数',
  college: '学院',
  code: '代码',
  applicationNumber: '申请号',
  currentAssignee: '当前权利人',
  originalAssignee: '原始权利人',
  agency: '代理机构',
  ipcMainClass: 'IPC主分类',
  _entityKey: '标识',
  source: '数据源',
  seq: '序号',
};

const VIEW_OPTIONS: Array<{
  key: ViewMode;
  label: string;
  hint: string;
  icon: React.ComponentType<{ className?: string }>;
}> = [
  { key: 'coInventor', label: '发明人协作网络', hint: '找合作圈和核心协作者', icon: Users },
  { key: 'topicCooccurrence', label: '技术主题共现', hint: '看主题组合和热点搭配', icon: GitBranch },
  { key: 'inventor', label: '发明人画像', hint: '围绕单个发明人展开分析', icon: Network },
  { key: 'topic', label: '主题画像', hint: '围绕技术主题展开映射关系', icon: Sparkles },
  { key: 'highValue', label: '高价值专利网络', hint: '聚焦高价值专利及相关实体', icon: Target },
];

const SEARCH_TYPE_OPTIONS = [
  { value: '', label: '全部实体' },
  { value: 'inventor', label: '发明人' },
  { value: 'topic', label: '技术主题' },
  { value: 'patent', label: '专利' },
  { value: 'ipc', label: 'IPC' },
  { value: 'cpc', label: 'CPC' },
  { value: 'applicationField', label: '应用领域' },
  { value: 'industry', label: '战略产业' },
  { value: 'college', label: '学院' },
  { value: 'assignee', label: '权利人机构' },
  { value: 'ipcInterpretation', label: 'IPC释义' },
];

const DEFAULT_VIEWPORT: Viewport = {
  scale: 1,
  offsetX: 0,
  offsetY: 0,
};

export const GraphVisualization: React.FC = () => {
  const [overview, setOverview] = useState<GraphOverview | null>(null);
  const [graphData, setGraphData] = useState<GraphData>({ nodes: [], edges: [] });
  const [viewMode, setViewMode] = useState<ViewMode>('highValue');
  const [keyword, setKeyword] = useState('');
  const [searchType, setSearchType] = useState('');
  const [suggestions, setSuggestions] = useState<SearchHit[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);
  const [selectedEdge, setSelectedEdge] = useState<GraphEdge | null>(null);
  const [edgePatentTitles, setEdgePatentTitles] = useState<Map<string, string>>(new Map());
  const [hoveredNodeId, setHoveredNodeId] = useState<string | null>(null);
  const [hoveredEdgeId, setHoveredEdgeId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<ActionType | null>(null);
  const [statusMessage, setStatusMessage] = useState('图谱已切换到关系分析模式。');
  const [minWeight, setMinWeight] = useState(2);
  const [limit, setLimit] = useState(40);
  const [minPatentValue, setMinPatentValue] = useState(50000);
  const [pathFromType, setPathFromType] = useState('inventor');
  const [pathFromId, setPathFromId] = useState('');
  const [pathToType, setPathToType] = useState('topic');
  const [pathToId, setPathToId] = useState('');
  const [pathMaxDepth, setPathMaxDepth] = useState(6);
  const [pathLoading, setPathLoading] = useState(false);
  const [viewport, setViewport] = useState<Viewport>(DEFAULT_VIEWPORT);
  const [rightWidth, setRightWidth] = useState(340);
  const resizeRef = useRef<{ startX: number; startW: number } | null>(null);

  const handleResizeStart = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    resizeRef.current = { startX: e.clientX, startW: rightWidth };
    const handleMove = (ev: MouseEvent) => {
      if (!resizeRef.current) return;
      const w = resizeRef.current.startW - (ev.clientX - resizeRef.current.startX);
      setRightWidth(Math.max(220, Math.min(560, w)));
    };
    const handleUp = () => {
      resizeRef.current = null;
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', handleUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', handleUp);
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  }, [rightWidth]);

  const fetchOverview = useCallback(async () => {
    const response = await fetch(`${API_BASE}/overview`);
    const result = await response.json();
    if (result.code === 200) {
      setOverview(result.data);
    }
  }, []);

  const fetchSuggestions = useCallback(async (nextKeyword: string, nextType: string) => {
    if (!nextKeyword.trim()) {
      setSuggestions([]);
      return;
    }

    try {
      const params = new URLSearchParams();
      params.set('keyword', nextKeyword.trim());
      params.set('limit', '8');
      if (nextType) {
        params.set('type', nextType);
      }
      const response = await fetch(`${API_BASE}/search?${params.toString()}`);
      const result = await response.json();
      if (result.code === 200) {
        setSuggestions(result.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch suggestions', error);
    }
  }, []);

  const resetViewport = useCallback(() => {
    setViewport(DEFAULT_VIEWPORT);
  }, []);

  const fetchGraph = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.set('limit', String(limit));

      let url = '';
      if (viewMode === 'coInventor') {
        params.set('minWeight', String(minWeight));
        if (keyword.trim()) params.set('keyword', keyword.trim());
        url = `${API_BASE}/network/co-inventor?${params.toString()}`;
      } else if (viewMode === 'topicCooccurrence') {
        params.set('minWeight', String(minWeight));
        if (keyword.trim()) params.set('keyword', keyword.trim());
        url = `${API_BASE}/network/topic-cooccurrence?${params.toString()}`;
      } else if (viewMode === 'inventor') {
        if (!keyword.trim()) {
          setGraphData({ nodes: [], edges: [], summary: { empty: true, mode: viewMode } });
          setLoading(false);
          return;
        }
        url = `${API_BASE}/inventor/${encodeURIComponent(keyword.trim())}?${params.toString()}`;
      } else if (viewMode === 'topic') {
        if (!keyword.trim()) {
          setGraphData({ nodes: [], edges: [], summary: { empty: true, mode: viewMode } });
          setLoading(false);
          return;
        }
        url = `${API_BASE}/topic/${encodeURIComponent(keyword.trim())}?${params.toString()}`;
      } else {
        params.set('minPatentValue', String(minPatentValue));
        if (keyword.trim()) params.set('keyword', keyword.trim());
        url = `${API_BASE}/network/high-value?${params.toString()}`;
      }

      const response = await fetch(url);
      const result = await response.json();
      if (result.code === 200) {
        setGraphData(result.data);
        setSelectedNode(null);
        setHoveredNodeId(null);
        setHoveredEdgeId(null);
        resetViewport();
      } else {
        setStatusMessage('图谱数据加载失败，请检查查询条件。');
      }
    } catch (error) {
      console.error('Failed to fetch graph data', error);
      setStatusMessage('图谱数据加载失败，请稍后重试。');
    } finally {
      setLoading(false);
    }
  }, [keyword, limit, minPatentValue, minWeight, resetViewport, viewMode]);

  useEffect(() => {
    fetchOverview();
  }, [fetchOverview]);

  useEffect(() => {
    fetchGraph();
  }, [fetchGraph]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchSuggestions(keyword, searchType);
    }, 180);
    return () => window.clearTimeout(timer);
  }, [fetchSuggestions, keyword, searchType]);

  // Fetch patent titles when an edge with patentIds is selected
  useEffect(() => {
    const patentIds = selectedEdge?.properties?.patentIds;
    if (Array.isArray(patentIds) && patentIds.length > 0) {
      fetch(`${API_BASE}/patent-titles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(patentIds),
      })
        .then((res) => res.json())
        .then((result) => {
          if (result.code === 200 && Array.isArray(result.data)) {
            const map = new Map<string, string>();
            result.data.forEach((item: any) => {
              if (item.id && item.title) map.set(item.id, item.title);
            });
            setEdgePatentTitles(map);
          }
        })
        .catch(() => {});
    } else {
      setEdgePatentTitles(new Map());
    }
  }, [selectedEdge]);

  const runAction = async (action: ActionType, endpoint: string, fallbackMessage: string) => {
    setActionLoading(action);
    try {
      const response = await fetch(`${API_BASE}${endpoint}`, { method: 'POST' });
      const result = await response.json();
      if (result.code === 200) {
        setStatusMessage(result.data?.message || fallbackMessage);
        await fetchOverview();
        await fetchGraph();
      } else {
        setStatusMessage('图谱操作失败，请检查后端返回。');
      }
    } catch (error) {
      console.error('Failed to run graph action', error);
      setStatusMessage('图谱操作失败，请检查后端与 Neo4j 连接。');
    } finally {
      setActionLoading(null);
    }
  };

  const runPathQuery = async () => {
    if (!pathFromId.trim() || !pathToId.trim()) {
      setStatusMessage('路径分析需要填写起点和终点。');
      return;
    }
    setPathLoading(true);
    try {
      const params = new URLSearchParams();
      params.set('fromType', pathFromType);
      params.set('fromId', pathFromId.trim());
      params.set('toType', pathToType);
      params.set('toId', pathToId.trim());
      params.set('maxDepth', String(pathMaxDepth));

      const response = await fetch(`${API_BASE}/path?${params.toString()}`);
      const result = await response.json();
      if (result.code === 200) {
        setGraphData(result.data);
        setSelectedNode(null);
        setHoveredNodeId(null);
        setHoveredEdgeId(null);
        resetViewport();
        setStatusMessage(`路径已找到：${pathFromId.trim()} → ${pathToId.trim()}（${result.data?.edges?.length || 0} 步）。`);
      } else {
        setStatusMessage('未找到路径，请尝试增大搜索深度。');
      }
    } catch (error) {
      console.error('Failed to fetch shortest path', error);
      setStatusMessage('路径分析失败。');
    } finally {
      setPathLoading(false);
    }
  };

  const applySuggestion = (hit: SearchHit) => {
    setKeyword(hit.label);
    setShowSuggestions(false);
    if (hit.type === 'Inventor') {
      setViewMode('inventor');
      setSearchType('inventor');
    } else if (hit.type === 'TechTopic') {
      setViewMode('topic');
      setSearchType('topic');
    } else if (hit.type === 'Patent') {
      setViewMode('highValue');
      setSearchType('patent');
    }
  };

  const centerNodeId = useMemo(() => {
    const centerType = graphData.summary?.centerType;
    const centerId = graphData.summary?.centerId;
    if (!centerType || !centerId) return null;
    return (
      graphData.nodes.find(
        (node) => node.type === centerType && node.properties?._entityKey === centerId,
      )?.id || null
    );
  }, [graphData]);

  const selectedNodeId = selectedNode?.id || null;
  const activeNodeId = hoveredNodeId || selectedNodeId;

  const neighborNodeIds = useMemo(() => {
    if (!activeNodeId) return new Set<string>();
    const ids = new Set<string>([activeNodeId]);
    graphData.edges.forEach((edge) => {
      if (edge.source === activeNodeId) ids.add(edge.target);
      if (edge.target === activeNodeId) ids.add(edge.source);
    });
    return ids;
  }, [activeNodeId, graphData.edges]);

  const totalNodeCount = useMemo(
    () => Object.values(overview?.nodeCounts || {}).reduce((sum, value) => sum + value, 0),
    [overview],
  );

  const totalEdgeCount = useMemo(
    () => Object.values(overview?.relationshipCounts || {}).reduce((sum, value) => sum + value, 0),
    [overview],
  );

  const selectedView = VIEW_OPTIONS.find((item) => item.key === viewMode);

  const quickActions = useMemo(() => {
    if (viewMode === 'inventor') return overview?.topInventors || [];
    if (viewMode === 'topic' || viewMode === 'topicCooccurrence') return overview?.topTopics || [];
    if (viewMode === 'highValue') return overview?.topIndustries || [];
    return overview?.topInventors || [];
  }, [overview, viewMode]);

  return (
    <div className="h-full bg-[#f5f5f0] text-slate-800 flex flex-col">
      <div className="border-b border-slate-200/60 bg-white/90 backdrop-blur-md">
        <div className="px-8 py-4 flex items-start justify-between gap-6">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center">
              <Network className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-lg font-semibold tracking-tight text-slate-800">知识图谱</h1>
              <p className="text-xs text-slate-400">
                关系分析 · 高价值网络 · 路径探索
              </p>
            </div>
          </div>
          <div className="max-w-xl text-right text-xs text-slate-400 leading-5">{statusMessage}</div>
        </div>
      </div>

      <div className="flex-1 overflow-hidden grid" style={{ gridTemplateColumns: `300px minmax(0,1fr) ${rightWidth}px` }}>
        <aside className="border-r border-slate-200/60 bg-white/70 p-5 overflow-y-auto">
          <SectionTitle icon={Database} title="概览" />
          <div className="grid grid-cols-2 gap-2 mb-6">
            <MetricCard label="节点" value={totalNodeCount} />
            <MetricCard label="关系" value={totalEdgeCount} />
            <MetricCard label="发明人" value={overview?.nodeCounts?.Inventor || 0} />
            <MetricCard label="主题" value={overview?.nodeCounts?.TechTopic || 0} />
          </div>

          <SectionTitle icon={Activity} title="视图" />
          <div className="space-y-1 mb-6">
            {VIEW_OPTIONS.map((option) => {
              const Icon = option.icon;
              const active = option.key === viewMode;
              return (
                <button
                  key={option.key}
                  onClick={() => setViewMode(option.key)}
                  className={`w-full rounded-lg border px-3 py-2 text-left transition-all ${
                    active
                      ? 'bg-slate-100 text-slate-700 border-slate-200'
                      : 'bg-transparent text-slate-500 border-transparent hover:bg-slate-50 hover:text-slate-600'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <Icon className="w-3.5 h-3.5" />
                    <span className="text-xs font-medium">{option.label}</span>
                  </div>
                </button>
              );
            })}
          </div>

          <SectionTitle icon={Search} title="检索" />
          <div className="space-y-3 mb-6">
            <div>
              <label className="block text-xs text-slate-500 mb-1.5">搜索类型</label>
              <select
                value={searchType}
                onChange={(event) => setSearchType(event.target.value)}
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-xs text-slate-600 focus:outline-none focus:border-slate-400"
              >
                {SEARCH_TYPE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="relative">
              <label className="block text-xs text-slate-500 mb-1.5">关键词</label>
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  value={keyword}
                  onChange={(event) => {
                    setKeyword(event.target.value);
                    setShowSuggestions(true);
                  }}
                  onFocus={() => setShowSuggestions(true)}
                  placeholder={
                    viewMode === 'inventor'
                      ? '输入发明人姓名'
                      : viewMode === 'topic'
                        ? '输入技术主题'
                        : viewMode === 'highValue'
                          ? '输入专利、人名或主题'
                          : '输入关键词过滤图谱'
                  }
                  className="w-full rounded-lg border border-slate-200 bg-white pl-9 pr-3 py-2.5 text-xs text-slate-600 placeholder-slate-400 focus:outline-none focus:border-slate-400"
                />
              </div>

              {showSuggestions && suggestions.length > 0 && (
                <div className="absolute z-20 mt-1.5 w-full rounded-lg border border-slate-200 bg-white shadow-lg overflow-hidden">
                  {suggestions.map((hit) => (
                    <button
                      key={`${hit.type}-${hit.entityKey}`}
                      onClick={() => applySuggestion(hit)}
                      className="w-full px-3.5 py-2.5 text-left hover:bg-slate-50 border-b border-slate-100 last:border-b-0"
                    >
                      <div className="text-xs font-medium text-slate-700">{hit.label}</div>
                      <div className="text-[10px] text-slate-400">
                        {NODE_TYPE_LABELS[hit.type] || hit.type}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {(viewMode === 'coInventor' || viewMode === 'topicCooccurrence') && (
              <FieldBlock label="最小权重">
                <input
                  type="number"
                  min={1}
                  value={minWeight}
                  onChange={(event) => setMinWeight(Number(event.target.value) || 1)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-xs text-slate-600 focus:outline-none focus:border-slate-400"
                />
              </FieldBlock>
            )}

            {viewMode === 'highValue' && (
              <FieldBlock label="最低专利价值">
                <input
                  type="number"
                  min={0}
                  step={1000}
                  value={minPatentValue}
                  onChange={(event) => setMinPatentValue(Number(event.target.value) || 0)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-xs text-slate-600 focus:outline-none focus:border-slate-400"
                />
              </FieldBlock>
            )}

            <FieldBlock label="返回规模">
              <input
                type="number"
                min={10}
                max={200}
                step={10}
                value={limit}
                onChange={(event) => setLimit(Number(event.target.value) || 40)}
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-xs text-slate-600 focus:outline-none focus:border-slate-400"
              />
            </FieldBlock>

            <button
              onClick={fetchGraph}
              className="w-full rounded-lg bg-slate-900 text-white px-3 py-2.5 text-xs font-medium hover:bg-slate-800 transition-colors"
            >
              刷新
            </button>
          </div>

          <SectionTitle icon={Wrench} title="维护" />
          <div className="grid grid-cols-2 gap-2 mb-6">
            <ActionButton
              label="全量重建"
              hint="清空后重建"
              loading={actionLoading === 'full'}
              onClick={() => runAction('full', '/sync', '知识图谱已按新模型完成全量重建')}
            />
            <ActionButton
              label="增量同步"
              hint="事实层 upsert"
              loading={actionLoading === 'incremental'}
              onClick={() => runAction('incremental', '/sync/incremental', '知识图谱已完成增量同步')}
            />
            <ActionButton
              label="重建分析层"
              hint="重算分析关系"
              loading={actionLoading === 'analysis'}
              onClick={() => runAction('analysis', '/rebuild/analysis', '知识图谱分析层已重建')}
            />
            <ActionButton
              label="刷新统计"
              hint="重算节点属性"
              loading={actionLoading === 'stats'}
              onClick={() => runAction('stats', '/rebuild/stats', '知识图谱统计属性已刷新')}
            />
          </div>

          <SectionTitle icon={Target} title="路径分析" />
          <div className="space-y-2">
            <div className="text-[11px] text-slate-400 leading-relaxed mb-1">查询两个实体间的最短关联路径。</div>
            <div className="grid grid-cols-2 gap-2">
              <FieldBlock label="起点类型">
                <select
                  value={pathFromType}
                  onChange={(e) => setPathFromType(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 focus:outline-none focus:border-slate-400"
                >
                  {SEARCH_TYPE_OPTIONS.filter((i) => i.value).map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </FieldBlock>
              <FieldBlock label="终点类型">
                <select
                  value={pathToType}
                  onChange={(e) => setPathToType(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 focus:outline-none focus:border-slate-400"
                >
                  {SEARCH_TYPE_OPTIONS.filter((i) => i.value).map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </FieldBlock>
            </div>
            <FieldBlock label="起点名称">
              <input
                value={pathFromId}
                onChange={(e) => setPathFromId(e.target.value)}
                placeholder="输入名称"
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 placeholder-slate-400 focus:outline-none focus:border-slate-400"
              />
            </FieldBlock>
            <FieldBlock label="终点名称">
              <input
                value={pathToId}
                onChange={(e) => setPathToId(e.target.value)}
                placeholder="输入名称"
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 placeholder-slate-400 focus:outline-none focus:border-slate-400"
              />
            </FieldBlock>
            <button
              onClick={runPathQuery}
              disabled={pathLoading}
              className="w-full rounded-lg bg-slate-900 text-white px-3 py-2 text-xs font-medium hover:bg-slate-800 disabled:opacity-30 transition-colors"
            >
              {pathLoading ? '查找路径...' : '查找最短路径'}
            </button>
          </div>
        </aside>

        <main className="relative overflow-hidden bg-white">
          <div className="relative h-full p-4">
            <div className="h-full rounded-[24px] border border-slate-200/60 bg-white shadow-sm flex flex-col">
              <div className="shrink-0 px-5 py-3 border-b border-slate-100 flex items-center justify-between gap-4">
                <div>
                  <h3 className="text-sm font-medium text-slate-700">{selectedView?.label || '路径分析结果'}</h3>
                  <p className="text-xs text-slate-400">
                    {String(graphData.summary?.nodeCount ?? 0)} 个节点 · {String(graphData.summary?.edgeCount ?? 0)} 条关系
                  </p>
                </div>
              </div>

              <div className="flex-1 min-h-0 relative">
                <GraphCanvasECharts
                  nodes={graphData.nodes}
                  edges={graphData.edges}
                  centerNodeId={centerNodeId}
                  loading={loading}
                  empty={!!graphData.summary?.empty}
                  onNodeHover={setHoveredNodeId}
                  onNodeClick={setSelectedNode}
                  onEdgeClick={setSelectedEdge}
                  quickActions={quickActions}
                  onQuickAction={(name) => {
                    setKeyword(name);
                    if (viewMode === 'coInventor') setViewMode('inventor');
                    if (viewMode === 'topicCooccurrence') setViewMode('topic');
                  }}
                />
              </div>
            </div>
          </div>
        </main>

        <aside className="border-l border-slate-200/60 bg-white p-6 overflow-y-auto relative">
          {/* Resize handle */}
          <div
            onMouseDown={handleResizeStart}
            className="absolute left-0 top-0 w-1.5 h-full cursor-col-resize hover:bg-blue-400/30 active:bg-blue-400/50 transition-colors z-10 -translate-x-full"
          />
          <div className="mb-4">
            <SectionTitle icon={Sparkles} title="详情" />
            {selectedEdge ? (
              <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="mb-4 pb-4 border-b border-slate-100">
                  <div className="text-[11px] uppercase tracking-wider text-slate-400 mb-1.5">
                    关系 · {RELATION_LABELS[selectedEdge.type] || selectedEdge.type}
                  </div>
                  <div className="text-sm font-medium text-slate-800 leading-snug">
                    {graphData.nodes.find(n => n.id === selectedEdge.source)?.label || selectedEdge.source}
                    <span className="text-slate-300 mx-1.5">→</span>
                    {graphData.nodes.find(n => n.id === selectedEdge.target)?.label || selectedEdge.target}
                  </div>
                </div>

                <div className="space-y-3 text-sm">
                  {Object.entries(selectedEdge.properties || {}).map(([key, value]) => {
                    if (key === 'patentIds' && Array.isArray(value)) {
                      return (
                        <div key={key} className="pb-3 border-b border-slate-50 last:border-b-0 last:pb-0">
                          <div className="text-slate-400 break-all text-xs mb-2">共同专利 ({value.length})</div>
                          <div className="space-y-1.5">
                            {value.slice(0, 8).map((pid: string, i: number) => (
                              <div key={i} className="text-xs text-slate-600 leading-relaxed">
                                <span className="text-slate-300 mr-1">{i + 1}.</span>
                                {edgePatentTitles.get(pid) || pid}
                              </div>
                            ))}
                            {value.length > 8 && (
                              <div className="text-xs text-slate-400 pt-1">...还有 {value.length - 8} 个</div>
                            )}
                          </div>
                        </div>
                      );
                    }
                    return (
                      <div key={key} className="grid grid-cols-[110px_minmax(0,1fr)] gap-3 pb-3 border-b border-slate-50 last:border-b-0 last:pb-0">
                        <div className="text-slate-400 break-all text-xs">{PROPERTY_LABELS[key] || key}</div>
                        <div className="text-slate-700 break-all text-xs font-medium">{String(value)}</div>
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : selectedNode ? (
              <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="mb-4 pb-4 border-b border-slate-100">
                  <div className="text-[11px] uppercase tracking-wider text-slate-400 mb-1.5">
                    {NODE_TYPE_LABELS[selectedNode.type] || selectedNode.type}
                  </div>
                  <div className="text-base font-semibold text-slate-800 leading-snug">{selectedNode.label}</div>
                </div>

                <div className="space-y-3 text-sm">
                  {Object.entries(selectedNode.properties || {}).map(([key, value]) => (
                    <div
                      key={key}
                      className="grid grid-cols-[110px_minmax(0,1fr)] gap-3 pb-3 border-b border-slate-50 last:border-b-0 last:pb-0"
                    >
                      <div className="text-slate-400 break-all text-xs">{PROPERTY_LABELS[key] || key}</div>
                      <div className="text-slate-700 break-all text-xs font-medium">{String(value)}</div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-6 text-sm text-slate-400 text-center">
                点击节点查看详情
              </div>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
};

const SectionTitle = ({
  icon: Icon,
  title,
}: {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
}) => (
  <div className="flex items-center gap-2 mb-2.5">
    <Icon className="w-3.5 h-3.5 text-slate-400" />
    <h2 className="text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-400">{title}</h2>
  </div>
);

const FieldBlock = ({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) => (
  <div>
    <label className="block text-xs text-slate-500 mb-1.5">{label}</label>
    {children}
  </div>
);

const MetricCard = ({ label, value }: { label: string; value: number }) => (
  <div className="rounded-lg bg-slate-50 border border-slate-200 p-3.5">
    <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1.5">{label}</div>
    <div className="text-lg font-semibold tracking-tight text-slate-700">{value}</div>
  </div>
);

const ActionButton = ({
  label,
  hint,
  loading,
  onClick,
}: {
  label: string;
  hint: string;
  loading: boolean;
  onClick: () => void;
}) => (
  <button
    onClick={onClick}
    disabled={loading}
    className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-left hover:bg-slate-50 disabled:opacity-40 transition-colors"
  >
    <div className="flex items-center gap-2 mb-0.5">
      {loading ? <RefreshCw className="w-3.5 h-3.5 animate-spin text-slate-400" /> : <Wrench className="w-3.5 h-3.5 text-slate-400" />}
      <span className="text-xs font-medium text-slate-600">{label}</span>
    </div>
    <div className="text-[10px] text-slate-400">{hint}</div>
  </button>
);

const CanvasButton = ({
  icon,
  onClick,
}: {
  icon: React.ReactNode;
  onClick: () => void;
}) => (
  <button
    onClick={onClick}
    className="w-8 h-8 rounded-lg border border-slate-200 bg-white text-slate-500 hover:bg-slate-50 hover:text-slate-700 transition-colors flex items-center justify-center"
  >
    {icon}
  </button>
);

const RankList = ({ title, items }: { title: string; items: Array<{ name: string; value: number }> }) => (
  <div className="mb-4 last:mb-0">
    <div className="text-xs font-medium text-slate-500 mb-2">{title}</div>
    <div className="space-y-1.5">
      {items.slice(0, 5).map((item, index) => (
        <div
          key={`${title}-${item.name}-${index}`}
          className="flex items-center justify-between rounded-lg bg-slate-50 border border-slate-200 px-3 py-2.5"
        >
          <div className="min-w-0">
            <div className="text-[10px] text-slate-400 mb-0.5">#{index + 1}</div>
            <div className="text-xs font-medium text-slate-600 truncate">{item.name}</div>
          </div>
          <div className="text-sm font-semibold text-slate-700">{item.value}</div>
        </div>
      ))}
      {!items.length && <div className="text-xs text-slate-400 px-1 py-2">暂无数据</div>}
    </div>
  </div>
);

const CenteredNotice = ({
  icon,
  title,
  text,
}: {
  icon: React.ReactNode;
  title: string;
  text: string;
}) => (
  <div className="h-full flex items-center justify-center">
    <div className="text-center text-slate-400">
      <div className="w-14 h-14 rounded-2xl bg-slate-100 mx-auto mb-4 flex items-center justify-center">
        {icon}
      </div>
      <div className="text-sm font-medium text-slate-500 mb-1">{title}</div>
      <div className="text-xs text-slate-400">{text}</div>
    </div>
  </div>
);
