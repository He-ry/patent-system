import React, { useRef, useEffect, useCallback, useLayoutEffect } from 'react';
import * as echarts from 'echarts';

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
  HAS_IPC_INTERPRETATION: 'IPC 释义',
};

interface Props {
  nodes: GraphNode[];
  edges: GraphEdge[];
  summary?: Record<string, unknown>;
  centerNodeId?: string | null;
  hoveredNodeId?: string | null;
  selectedNodeId?: string | null;
  loading?: boolean;
  empty?: boolean;
  onNodeHover?: (id: string | null) => void;
  onNodeClick?: (node: GraphNode | null) => void;
  onEdgeClick?: (edge: GraphEdge | null) => void;
  quickActions?: Array<{ name: string; value: number }>;
  onQuickAction?: (name: string) => void;
}

export const GraphCanvasECharts: React.FC<Props> = ({
  nodes, edges, centerNodeId, loading, empty,
  onNodeHover, onNodeClick, onEdgeClick,
  quickActions, onQuickAction,
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);
  const lastNodesRef = useRef<GraphNode[]>([]);

  const buildOption = useCallback(() => {
    if (!nodes.length) return {};

    // ── Node type size mapping ──
    const NODE_SIZES: Record<string, number> = {
      Patent: 32,
      Inventor: 26,
      TechTopic: 22,
      StrategicIndustry: 20,
      ApplicationField: 18,
      IPC: 16,
      CPC: 14,
      AssigneeOrg: 20,
      College: 20,
      Problem: 14,
      Effect: 14,
    };

    // Count connections per node for sizing
    const degree = new Map<string, number>();
    edges.forEach((e) => {
      degree.set(e.source, (degree.get(e.source) || 0) + 1);
      degree.set(e.target, (degree.get(e.target) || 0) + 1);
    });
    const maxDeg = Math.max(...degree.values(), 1);

    const nodeData = nodes.map((n) => {
      const baseSize = NODE_SIZES[n.type] || 16;
      const d = degree.get(n.id) || 1;
      const scale = 0.7 + (d / maxDeg) * 0.6;
      const size = n.id === centerNodeId ? baseSize * 1.5 : baseSize * scale;

      return {
        id: n.id,
        name: n.label,
        value: NODE_TYPE_LABELS[n.type] || n.type,
        itemStyle: {
          color: NODE_COLORS[n.type] || NODE_COLORS.Unknown,
          shadowBlur: n.id === centerNodeId ? 10 : 0,
          shadowColor: (NODE_COLORS[n.type] || NODE_COLORS.Unknown) + '80',
        },
        symbolSize: Math.max(10, size),
        category: n.type,
        label: {
          show: true,
          position: 'bottom',
          fontSize: d > 3 ? 11 : 10,
          color: '#334155',
          fontWeight: n.id === centerNodeId ? 700 : 400,
          formatter: (params: any) => {
            const name: string = params.name || '';
            return name.length > 12 ? name.slice(0, 12) + '...' : name;
          },
        },
        _raw: n,
      };
    });

    const categories = Object.keys(NODE_TYPE_LABELS)
      .filter((key) => nodes.some((n) => n.type === key))
      .map((key) => ({
        name: key,
        itemStyle: { color: NODE_COLORS[key] || NODE_COLORS.Unknown },
        label: NODE_TYPE_LABELS[key],
      }));

    const linkData = edges.map((e) => ({
      source: e.source,
      target: e.target,
      value: `${RELATION_LABELS[e.type] || e.type}${e.weight ? ` (${e.weight})` : ''}`,
      lineStyle: {
        width: Math.max(0.5, Math.min((e.weight || 1) * 0.2, 2.5)),
        curveness: 0.15,
        opacity: 0.25,
      },
      _raw: e,
    }));

    return {
      backgroundColor: '#ffffff',
      legend: {
        show: categories.length > 1,
        data: categories.map((c) => c.name),
        top: 8,
        left: 12,
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { fontSize: 11, color: '#64748b' },
      },
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            const raw = params.data._raw as GraphNode;
            if (!raw) return '';
            const typeLabel = NODE_TYPE_LABELS[raw.type] || raw.type;
            const d = degree.get(raw.id) || 0;
            let html = `<div style="font-weight:600;font-size:13px;margin-bottom:2px">${raw.label}</div>`;
            html += `<div style="font-size:11px;color:#64748b">${typeLabel} · ${d} 条连接</div>`;
            return html;
          }
          if (params.dataType === 'edge') {
            const raw = params.data._raw as GraphEdge;
            if (!raw) return '';
            const label2 = RELATION_LABELS[raw.type] || raw.type;
            const props = raw.properties || {};
            const sharedCount = props.patentCount || props.count || props.weight || '';
            const patentIds = props.patentIds;
            const idCount = Array.isArray(patentIds) ? patentIds.length : 0;
            let html = `<div style="font-size:12px;font-weight:500;color:#334155">${label2}</div>`;
            if (sharedCount) html += `<div style="font-size:11px;color:#64748b;margin-top:2px">关联 ${sharedCount} 件专利</div>`;
            if (idCount > 0) html += `<div style="font-size:10px;color:#94a3b8;margin-top:1px">${idCount} 个专利 ID</div>`;
            return html;
          }
          return params.name || '';
        },
      },
      series: [{
        type: 'graph',
        layout: 'force',
        force: {
          repulsion: 400,
          edgeLength: [100, 250],
          gravity: 0.06,
          friction: 0.1,
          layoutAnimation: true,
        },
        roam: true,
        draggable: true,
        data: nodeData,
        links: linkData,
        categories,
        edgeSymbol: ['none', 'none'],
        edgeLabel: {
          show: false,
          fontSize: 9,
          color: '#94a3b8',
          formatter: (p: any) => p.data?.value || '',
        },
        label: {
          show: true,
          position: 'bottom',
          fontSize: 10,
          color: '#94a3b8',
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 2, opacity: 0.7 },
        },
        blur: {
          opacity: 0.12,
          lineStyle: { opacity: 0.05 },
        },
        lineStyle: {
          color: 'source',
          curveness: 0.15,
          opacity: 0.2,
        },
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 1.5,
        },
        animation: true,
        animationDuration: 600,
        animationEasing: 'cubicOut',
      }],
    };
  }, [nodes, edges, centerNodeId]);

  // Init chart (useLayoutEffect ensures DOM ref is ready)
  useLayoutEffect(() => {
    if (!chartRef.current) return;
    console.log('[GraphCanvas] init ECharts, container size:', chartRef.current.clientWidth, 'x', chartRef.current.clientHeight);
    const instance = echarts.init(chartRef.current, undefined, { renderer: 'canvas' });
    instanceRef.current = instance;

    // Show any data immediately after init
    const option = buildOption();
    if (Object.keys(option).length > 0) {
      instance.setOption(option, true);
    }

    const handleResize = () => instance.resize();
    const resizeObserver = new ResizeObserver(() => instance.resize());
    if (chartRef.current) resizeObserver.observe(chartRef.current);
    window.addEventListener('resize', handleResize);

    // Force resize after mount (flex layout height resolution)
    const resizeTimer = setTimeout(() => instance.resize(), 100);

    return () => {
      clearTimeout(resizeTimer);
      window.removeEventListener('resize', handleResize);
      resizeObserver.disconnect();
      instance.dispose();
      instanceRef.current = null;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update chart (useLayoutEffect to sync after data arrives)
  useLayoutEffect(() => {
    const instance = instanceRef.current;
    if (!instance) return;
    const option = buildOption();
    try {
      instance.setOption(option, true);
      console.log('[GraphCanvas] setOption OK, nodes=' + nodes.length);
    } catch (err) {
      console.error('[GraphCanvas] setOption ERROR:', err);
    }
    lastNodesRef.current = nodes;
  }, [buildOption, nodes, edges]);

  // Hover events
  useEffect(() => {
    const instance = instanceRef.current;
    if (!instance) return;

    const handleClick = (params: any) => {
      if (params.dataType === 'node' && params.data._raw) {
        onNodeClick?.(params.data._raw);
        onEdgeClick?.(null);
      } else if (params.dataType === 'edge' && params.data._raw) {
        onEdgeClick?.(params.data._raw);
        onNodeClick?.(null);
      } else {
        onNodeClick?.(null);
        onEdgeClick?.(null);
      }
    };

    const handleMouseover = (params: any) => {
      if (params.dataType === 'node' && params.data._raw) {
        onNodeHover?.(params.data._raw.id);
      }
    };

    const handleMouseout = () => {
      onNodeHover?.(null);
    };

    instance.on('click', handleClick);
    instance.on('mouseover', handleMouseover);
    instance.on('mouseout', handleMouseout);

    return () => {
      instance.off('click', handleClick);
      instance.off('mouseover', handleMouseover);
      instance.off('mouseout', handleMouseout);
    };
  }, [onNodeClick, onNodeHover, onEdgeClick]);

  // Resize when loading changes
  useEffect(() => {
    instanceRef.current?.resize();
  }, [loading]);

  return (
    <div className="w-full h-full flex flex-col relative">
      {/* Always rendered chart container */}
      <div ref={chartRef} className="flex-1 min-h-0" />

      {/* Loading overlay */}
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-white z-10">
          <div className="text-center text-slate-400">
            <div className="w-14 h-14 rounded-2xl bg-slate-100 mx-auto mb-4 flex items-center justify-center">
              <div className="w-5 h-5 border-2 border-slate-300 border-t-transparent rounded-full animate-spin" />
            </div>
            <div className="text-sm font-medium text-slate-500 mb-1">加载中</div>
            <div className="text-xs text-slate-400">正在计算布局...</div>
          </div>
        </div>
      )}

      {/* Empty overlay */}
      {!loading && (empty || !nodes.length) && (
        <div className="absolute inset-0 flex items-center justify-center bg-white z-10">
          <div className="text-center text-slate-400">
            <div className="w-14 h-14 rounded-2xl bg-slate-100 mx-auto mb-4 flex items-center justify-center">
              <span className="text-slate-400 text-xl">○</span>
            </div>
            <div className="text-sm font-medium text-slate-500 mb-1">
              {empty ? '等待输入' : '暂无数据'}
            </div>
            <div className="text-xs text-slate-400">
              {empty ? '请指定分析对象' : '调整条件后刷新'}
            </div>
          </div>
        </div>
      )}

      {/* Debug indicator */}
      {!loading && nodes.length > 0 && (
        <div className="absolute top-2 left-2 z-10 bg-yellow-100 border border-yellow-300 rounded px-2 py-1 text-xs text-yellow-800 shadow">
          {nodes.length} 节点 / {edges.length} 边
          {centerNodeId ? ' · 有中心' : ''}
        </div>
      )}

      {/* Quick actions */}
      {quickActions && quickActions.length > 0 && (
        <div className="absolute left-5 bottom-5 flex items-center gap-2 flex-wrap pointer-events-none z-10">
          <div className="flex items-center gap-2 flex-wrap pointer-events-auto">
            {quickActions.slice(0, 5).map((item) => (
              <button
                key={`${item.name}-${item.value}`}
                onClick={() => onQuickAction?.(item.name)}
                className="rounded-full border border-slate-200 bg-white/90 px-3 py-1.5 text-[11px] text-slate-500 hover:border-slate-300 transition-colors shadow-sm"
              >
                {item.name}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
