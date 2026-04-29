import React, { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate, Routes, Route } from 'react-router-dom';
import * as XLSX from 'xlsx';
import {
  Upload, Plus, Download, Edit2, Trash2, X, Save,
  FileText, Database, PieChart as PieChartIcon,
  Search, LayoutDashboard, Cloud, BarChart3,
  Home, Activity, TrendingUp, List, ChevronDown, Check, Eye,
  CheckCircle, AlertCircle, MessageSquare, Loader2, Network, Settings,
  ChevronLeft, ChevronRight
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts';
import { PatentDataManagement } from './components/PatentDataManagement';
import { GraphVisualization } from './components/GraphVisualization';
import { ModelConfigPage } from './components/ModelConfigPage';
import { ChatContainer } from './chat';
import { patentApi, PatentInfo, PATENT_COLUMNS, WORD_CLOUD_COLUMNS, STATISTICS_COLUMNS, TREND_COLUMNS, ALL_PATENT_COLUMNS } from './api';

// --- 3D Sphere Word Cloud Component ---
const SphereWordCloud = ({ words, radius = 220, onWordClick }: { words: any[], radius?: number, onWordClick?: (word: any) => void }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [points, setPoints] = useState<any[]>([]);
  const mouseRef = useRef({ x: 0.003, y: 0.003 });
  const pointsRef = useRef<any[]>([]);
  const isPausedRef = useRef(false);
  const [hoveredWord, setHoveredWord] = useState<any | null>(null);

  useEffect(() => {
    if (!words.length) return;
    const maxVal = Math.max(...words.map(w => w.value));
    const minVal = Math.min(...words.map(w => w.value));
    const n = words.length;
    
    const initialPoints = words.map((word, i) => {
      const phi = Math.acos(-1 + (2 * i) / n);
      const theta = Math.sqrt(n * Math.PI) * phi;
      return {
        ...word,
        x: radius * Math.cos(theta) * Math.sin(phi),
        y: radius * Math.sin(theta) * Math.sin(phi),
        z: radius * Math.cos(phi),
        fontSize: 12 + ((word.value - minVal) / (maxVal - minVal || 1)) * 24,
        color: `hsl(${(i * 360) / n}, 75%, 45%)`
      };
    });
    pointsRef.current = initialPoints;
    setPoints(initialPoints);
  }, [words, radius]);

  useEffect(() => {
    let animationFrameId: number;
    const update = () => {
      if (!isPausedRef.current) {
        const { x: speedX, y: speedY } = mouseRef.current;
        const sinX = Math.sin(speedX);
        const cosX = Math.cos(speedX);
        const sinY = Math.sin(speedY);
        const cosY = Math.cos(speedY);

        pointsRef.current = pointsRef.current.map(p => {
          const y1 = p.y * cosX - p.z * sinX;
          const z1 = p.z * cosX + p.y * sinX;
          const x2 = p.x * cosY + z1 * sinY;
          const z2 = z1 * cosY - p.x * sinY;
          return { ...p, x: x2, y: y1, z: z2 };
        });
        
        setPoints([...pointsRef.current]);
      }
      animationFrameId = requestAnimationFrame(update);
    };
    update();
    return () => cancelAnimationFrame(animationFrameId);
  }, []);

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!containerRef.current || isPausedRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;
    mouseRef.current = {
      x: (y / rect.height) * 0.02,
      y: (x / rect.width) * 0.02
    };
  };

  const handleMouseLeave = () => {
    if (isPausedRef.current) return;
    mouseRef.current = { x: 0.003, y: 0.003 };
  };

  const focalLength = radius * 2;

  return (
    <div 
      ref={containerRef}
      className="relative w-full h-full flex items-center justify-center overflow-hidden bg-white"
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
    >
      {points.map((p, i) => {
        const scale = focalLength / (focalLength + p.z);
        const opacity = Math.max(0.1, 1 - (p.z + radius) / (2 * radius));
        const zIndex = Math.round(scale * 100);
        
        return (
          <div
            key={i}
            className="absolute font-bold transition-colors duration-200 cursor-pointer hover:!text-blue-500"
            style={{
              transform: `translate(${p.x * scale}px, ${p.y * scale}px) scale(${scale})`,
              opacity: opacity,
              zIndex: zIndex,
              fontSize: `${p.fontSize}px`,
              color: p.color,
              textShadow: '0px 1px 2px rgba(0,0,0,0.1)'
            }}
            onClick={() => onWordClick && onWordClick(p)}
            onMouseEnter={(e) => {
              isPausedRef.current = true;
              const rect = containerRef.current?.getBoundingClientRect();
              if (rect) {
                setHoveredWord({
                  ...p,
                  clientX: e.clientX - rect.left,
                  clientY: e.clientY - rect.top
                });
              }
            }}
            onMouseLeave={() => {
              isPausedRef.current = false;
              setHoveredWord(null);
            }}
          >
            {p.text}
          </div>
        );
      })}

      {/* Custom Tooltip */}
      {hoveredWord && (
        <div 
          className="absolute z-[999] bg-white/95 backdrop-blur-sm border border-slate-200 shadow-xl rounded-xl p-5 pointer-events-none transform -translate-x-1/2 -translate-y-full mt-[-10px] w-96"
          style={{
            left: hoveredWord.clientX,
            top: hoveredWord.clientY
          }}
        >
          <div className="flex items-center gap-2 mb-3 border-b border-slate-100 pb-3">
            <div className="w-3 h-3 rounded-full shadow-sm shrink-0" style={{ backgroundColor: hoveredWord.color }}></div>
            <span className="font-bold text-slate-800 text-base leading-tight">{hoveredWord.fullText}</span>
          </div>
          
          {hoveredWord.record && (
            <div className="space-y-2.5">
              <div className="grid grid-cols-3 gap-2">
                <span className="text-xs text-slate-500">申请号:</span>
                <span className="text-xs text-slate-800 col-span-2 truncate">{hoveredWord.record['申请号'] || '-'}</span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                <span className="text-xs text-slate-500">申请日:</span>
                <span className="text-xs text-slate-800 col-span-2 truncate">{hoveredWord.record['申请日'] || '-'}</span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                <span className="text-xs text-slate-500">发明人:</span>
                <span className="text-xs text-slate-800 col-span-2 truncate">{hoveredWord.record['发明人'] || '-'}</span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                <span className="text-xs text-slate-500">法律状态:</span>
                <span className="text-xs text-slate-800 col-span-2 truncate">
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-blue-50 text-blue-700 border border-blue-100">
                    {hoveredWord.record['法律状态/事件'] || '-'}
                  </span>
                </span>
              </div>
            </div>
          )}
          
          <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
            <span className="text-xs text-slate-500">出现频次: <span className="font-semibold text-blue-600">{hoveredWord.value}</span></span>
            <span className="text-xs text-blue-600 font-medium flex items-center gap-1">点击查看完整详情 <Eye size={12} /></span>
          </div>
        </div>
      )}
    </div>
  );
};

// --- Custom Select Component ---
const CustomSelect = ({ value, onChange, options, labels, placeholder = "请选择" }: { value: string, onChange: (val: string) => void, options: string[], labels?: Record<string, string>, placeholder?: string }) => {
  const [isOpen, setIsOpen] = useState(false);
  const selectRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (selectRef.current && !selectRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const displayValue = labels?.[value] || value;

  return (
    <div className="relative min-w-[200px]" ref={selectRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`w-full flex items-center justify-between px-4 py-2.5 bg-white border ${isOpen ? 'border-blue-500 ring-4 ring-blue-500/10' : 'border-slate-200 hover:border-slate-300'} rounded-xl text-sm text-slate-700 shadow-sm transition-all focus:outline-none`}
      >
        <span className="truncate font-medium">{displayValue || placeholder}</span>
        <ChevronDown size={16} className={`text-slate-400 transition-transform duration-200 ${isOpen ? 'rotate-180 text-blue-500' : ''}`} />
      </button>

      {isOpen && (
        <div className="absolute z-50 w-full mt-2 bg-white border border-slate-100 rounded-xl shadow-[0_10px_40px_-10px_rgba(0,0,0,0.1)] overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="max-h-64 overflow-y-auto dropdown-scrollbar p-1.5">
            {options.map((option) => (
              <button
                key={option}
                onClick={() => {
                  onChange(option);
                  setIsOpen(false);
                }}
                className={`w-full flex items-center justify-between px-3 py-2.5 text-sm rounded-lg transition-colors ${
                  value === option
                    ? 'bg-blue-50 text-blue-700 font-semibold'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <span className="truncate">{labels?.[option] || option}</span>
                {value === option && <Check size={16} className="text-blue-600 shrink-0" />}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

const INITIAL_COLUMNS = [
  '序号', '标题', '申请日', '发明人', '学院', '法律状态/事件', '专利类型', '申请号', '授权日', '技术领域',
  '[标]技术问题短语', '[标]技术功效短语', '[标]当前申请(专利权)人', '[标]原始申请(专利权)人', '发明人数量',
  '[标]代理机构', '当前申请(专利权)人州/省', '原始申请(专利权)人州/省', '[标]原始申请(专利权)人类型',
  '[标]当前申请(专利权)人类型', '申请年', '公开(公告)日', '授权年', 'IPC分类号', 'CPC分类号', 'IPC主分类号',
  'IPC主分类号(部)释义', '战略新兴产业分类', '应用领域分类', '技术主题分类', '失效日', '简单同族被引用专利总数',
  '被引用专利数量', '5年内被引用数量', '权利要求数', '专利价值', '技术价值', '市场价值', '权利转移生效日',
  '许可类型', '许可次数', '许可生效日', '转让人', '受让人'
];

const INITIAL_DATA = [
  {
    '序号': '1', '标题': '一种基于深度学习的图像识别方法及系统', '申请日': '2021-05-12', '发明人': '张三, 李四',
    '学院': '计算机学院', '法律状态/事件': '实质审查', '专利类型': '发明专利', '申请号': 'CN202110512345.X',
    '授权日': '', '技术领域': '人工智能', '[标]技术问题短语': '图像识别准确率低', '[标]技术功效短语': '提高识别准确率',
    '[标]当前申请(专利权)人': '腾讯科技（深圳）有限公司', '[标]原始申请(专利权)人': '腾讯科技（深圳）有限公司',
    '发明人数量': '2', '[标]代理机构': '深圳市专利代理有限公司', '当前申请(专利权)人州/省': '广东',
    '原始申请(专利权)人州/省': '广东', '[标]原始申请(专利权)人类型': '企业', '[标]当前申请(专利权)人类型': '企业',
    '申请年': '2021', '公开(公告)日': '2021-08-15', '授权年': '', 'IPC分类号': 'G06K9/00', 'CPC分类号': '',
    'IPC主分类号': 'G06', 'IPC主分类号(部)释义': '物理', '战略新兴产业分类': '新一代信息技术产业',
    '应用领域分类': '智慧城市', '技术主题分类': '计算机视觉', '失效日': '', '简单同族被引用专利总数': '15',
    '被引用专利数量': '10', '5年内被引用数量': '8', '权利要求数': '12', '专利价值': '高', '技术价值': '高',
    '市场价值': '中', '权利转移生效日': '', '许可类型': '', '许可次数': '0', '许可生效日': '', '转让人': '', '受让人': ''
  },
  {
    '序号': '2', '标题': '区块链共识机制的优化方法', '申请日': '2022-08-20', '发明人': '王五',
    '学院': '软件学院', '法律状态/事件': '授权', '专利类型': '发明专利', '申请号': 'CN202210820345.1',
    '授权日': '2023-05-10', '技术领域': '区块链', '[标]技术问题短语': '通信开销大', '[标]技术功效短语': '降低通信开销',
    '[标]当前申请(专利权)人': '阿里巴巴集团控股有限公司', '[标]原始申请(专利权)人': '阿里巴巴集团控股有限公司',
    '发明人数量': '1', '[标]代理机构': '杭州市专利代理事务所', '当前申请(专利权)人州/省': '浙江',
    '原始申请(专利权)人州/省': '浙江', '[标]原始申请(专利权)人类型': '企业', '[标]当前申请(专利权)人类型': '企业',
    '申请年': '2022', '公开(公告)日': '2022-12-01', '授权年': '2023', 'IPC分类号': 'H04L9/32', 'CPC分类号': '',
    'IPC主分类号': 'H04', 'IPC主分类号(部)释义': '电学', '战略新兴产业分类': '新一代信息技术产业',
    '应用领域分类': '金融科技', '技术主题分类': '分布式账本', '失效日': '', '简单同族被引用专利总数': '5',
    '被引用专利数量': '3', '5年内被引用数量': '3', '权利要求数': '8', '专利价值': '中', '技术价值': '中',
    '市场价值': '高', '权利转移生效日': '', '许可类型': '', '许可次数': '0', '许可生效日': '', '转让人': '', '受让人': ''
  },
  {
    '序号': '3', '标题': '自动驾驶车辆的轨迹预测方法', '申请日': '2019-11-05', '发明人': '赵六, 孙七',
    '学院': '汽车工程学院', '法律状态/事件': '授权', '专利类型': '发明专利', '申请号': 'CN201911105678.2',
    '授权日': '2021-02-15', '技术领域': '自动驾驶', '[标]技术问题短语': '轨迹预测不准', '[标]技术功效短语': '提高预测准确性',
    '[标]当前申请(专利权)人': '百度网讯科技有限公司', '[标]原始申请(专利权)人': '百度网讯科技有限公司',
    '发明人数量': '2', '[标]代理机构': '北京市专利代理机构', '当前申请(专利权)人州/省': '北京',
    '原始申请(专利权)人州/省': '北京', '[标]原始申请(专利权)人类型': '企业', '[标]当前申请(专利权)人类型': '企业',
    '申请年': '2019', '公开(公告)日': '2020-05-20', '授权年': '2021', 'IPC分类号': 'B60W30/00', 'CPC分类号': '',
    'IPC主分类号': 'B60', 'IPC主分类号(部)释义': '作业；运输', '战略新兴产业分类': '新能源汽车产业',
    '应用领域分类': '智能交通', '技术主题分类': '轨迹规划', '失效日': '', '简单同族被引用专利总数': '25',
    '被引用专利数量': '18', '5年内被引用数量': '15', '权利要求数': '15', '专利价值': '高', '技术价值': '高',
    '市场价值': '高', '权利转移生效日': '', '许可类型': '', '许可次数': '0', '许可生效日': '', '转让人': '', '受让人': ''
  },
  {
    '序号': '4', '标题': '基于5G通信的物联网设备接入方法', '申请日': '2023-02-15', '发明人': '周八',
    '学院': '通信工程学院', '法律状态/事件': '公开', '专利类型': '发明专利', '申请号': 'CN202310215890.3',
    '授权日': '', '技术领域': '5G通信', '[标]技术问题短语': '网络拥塞', '[标]技术功效短语': '降低网络拥塞',
    '[标]当前申请(专利权)人': '华为技术有限公司', '[标]原始申请(专利权)人': '华为技术有限公司',
    '发明人数量': '1', '[标]代理机构': '深圳市专利代理有限公司', '当前申请(专利权)人州/省': '广东',
    '原始申请(专利权)人州/省': '广东', '[标]原始申请(专利权)人类型': '企业', '[标]当前申请(专利权)人类型': '企业',
    '申请年': '2023', '公开(公告)日': '2023-08-10', '授权年': '', 'IPC分类号': 'H04W4/70', 'CPC分类号': '',
    'IPC主分类号': 'H04', 'IPC主分类号(部)释义': '电学', '战略新兴产业分类': '新一代信息技术产业',
    '应用领域分类': '物联网', '技术主题分类': '设备接入', '失效日': '', '简单同族被引用专利总数': '2',
    '被引用专利数量': '1', '5年内被引用数量': '1', '权利要求数': '10', '专利价值': '中', '技术价值': '高',
    '市场价值': '中', '权利转移生效日': '', '许可类型': '', '许可次数': '0', '许可生效日': '', '转让人': '', '受让人': ''
  }
];

const LEGACY_SEARCH_DIMENSIONS = [
  { value: '', label: '全部字段' },
  { value: 'college', label: '学院' },
  { value: 'ipcMainClassInterpretation', label: 'IPC分类释义' },
  { value: 'inventors', label: '发明人' },
  { value: 'applicationFieldClassification', label: '应用领域' },
  { value: 'technicalSubjectClassification', label: '技术主题' }
];

const SEARCH_DIMENSIONS = [
  { value: '', label: '全部字段' },
  { value: 'title', label: '标题' },
  { value: 'applicationNumber', label: '申请号' },
  { value: 'college', label: '学院' },
  { value: 'inventors', label: '发明人' },
  { value: 'technicalFields', label: '技术领域' },
  { value: 'technicalProblem', label: '技术问题短语' },
  { value: 'technicalEffect', label: '技术功效短语' },
  { value: 'ipcClassifications', label: 'IPC分类号' },
  { value: 'cpcClassifications', label: 'CPC分类号' },
  { value: 'ipcMainClassInterpretation', label: 'IPC主分类号(部)释义' },
  { value: 'applicationFieldClassification', label: '应用领域分类' },
  { value: 'technicalSubjectClassification', label: '技术主题分类' },
  { value: 'strategicIndustryClassification', label: '战略新兴产业分类' },
];

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (location.pathname === '/') {
      navigate('/manage', { replace: true });
    }
  }, [location.pathname, navigate]);

  // Data State
  const [data, setData] = useState<any[]>(INITIAL_DATA);
  const [columns, setColumns] = useState<string[]>(INITIAL_COLUMNS);
  const [fileName, setFileName] = useState<string>('示例专利数据集.xlsx');
  const [searchTerm, setSearchTerm] = useState('');
  const [searchField, setSearchField] = useState('');

  // Backend API State
  const [remoteData, setRemoteData] = useState<PatentInfo[]>([]);
  const [remoteTotal, setRemoteTotal] = useState(0);
  const [remoteLoading, setRemoteLoading] = useState(false);
  const [importLoading, setImportLoading] = useState(false);
  const [useBackend, setUseBackend] = useState(true);

  // WordCloud & Stats Backend State
  const [remoteWordCloudData, setRemoteWordCloudData] = useState<{ name: string; value: number }[]>([]);
  const [remoteStatsData, setRemoteStatsData] = useState<{ name: string; value: number }[]>([]);
  const [remoteTrendData, setRemoteTrendData] = useState<{ year: string; name: string; count: number }[]>([]);
  
  // CRUD State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [formData, setFormData] = useState<any>({});
  
  // Pagination State
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(20);

  // View Details State
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [viewingRecord, setViewingRecord] = useState<any | null>(null);
  
  // Stats State
  const [statsColumn, setStatsColumn] = useState<string>('inventors');
  const [wordCloudColumn, setWordCloudColumn] = useState<string>('inventors');
  const [trendColumn, setTrendColumn] = useState<string>('ipcMainClassInterpretation');

  const fileInputRef = useRef<HTMLInputElement>(null);
  const searchDropdownRef = useRef<HTMLDivElement>(null);
  const [isSearchDropdownOpen, setIsSearchDropdownOpen] = useState(false);

  // Toast Notification State
  const [toast, setToast] = useState<{ show: boolean; message: string; type: 'success' | 'error' }>({ show: false, message: '', type: 'success' });
  const [deleteTarget, setDeleteTarget] = useState<any | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  useEffect(() => {
    if (toast.show) {
      const timer = setTimeout(() => {
        setToast({ ...toast, show: false });
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchDropdownRef.current && !searchDropdownRef.current.contains(event.target as Node)) {
        setIsSearchDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // --- Backend API ---
  const fetchPatents = useCallback(async () => {
    setRemoteLoading(true);
    try {
      const params: any = {
        page: currentPage,
        size: itemsPerPage,
      };

      if (searchTerm) {
        if (searchField && searchField !== 'all') {
          params[searchField] = searchTerm;
        } else {
          params.keyword = searchTerm;
        }
      }

      const response = await patentApi.getList(params);
      if (response.code === 200 && response.data) {
        setRemoteData(response.data.list || []);
        setRemoteTotal(response.data.total || 0);
      }
    } catch (error) {
      console.error('获取专利列表失败:', error);
    } finally {
      setRemoteLoading(false);
    }
  }, [currentPage, itemsPerPage, searchTerm, searchField]);

  useEffect(() => {
    if (useBackend && location.pathname === '/manage') {
      fetchPatents();
    }
  }, [fetchPatents, useBackend, location.pathname]);

  // --- Fetch WordCloud Data from Backend ---
  const fetchWordCloudData = useCallback(async (dimension: string) => {
    try {
      const response = await patentApi.getWordCloud({ dimension: dimension as any, limit: 80 });
      if (response.code === 200 && response.data) {
        setRemoteWordCloudData(response.data.map(item => ({ name: item.word, value: item.count })));
      }
    } catch (error) {
      console.error('获取词云数据失败:', error);
    }
  }, []);

  // --- Fetch Stats Data from Backend ---
  const fetchStatsData = useCallback(async (field: string) => {
    try {
      const response = await patentApi.statistics({ field, limit: 10 });
      if (response.code === 200 && response.data) {
        setRemoteStatsData(response.data);
      }
    } catch (error) {
      console.error('获取统计数据失败:', error);
    }
  }, []);

  // --- Fetch Trend Data from Backend ---
  const fetchTrendData = useCallback(async (dimension: string) => {
    try {
      const response = await patentApi.getTrend({ dimension: dimension as any, limit: 10 });
      if (response.code === 200 && response.data) {
        setRemoteTrendData(response.data || []);
      }
    } catch (error) {
      console.error('获取趋势数据失败:', error);
    }
  }, []);

  // --- Handlers ---
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (useBackend) {
      setImportLoading(true);
      try {
        const result = await patentApi.import(file);
        if (result.code === 200 || result.code === 0) {
          setToast({ show: true, message: `导入成功！共导入 ${result.data?.success || 0} 条数据`, type: 'success' });
          fetchPatents();
        } else {
          setToast({ show: true, message: result.message || '导入失败', type: 'error' });
        }
      } catch (error) {
        setToast({ show: true, message: '导入失败，请重试', type: 'error' });
      } finally {
        setImportLoading(false);
      }
    } else {
      setFileName(file.name);
      const reader = new FileReader();
      reader.onload = (evt) => {
        const bstr = evt.target?.result;
        const wb = XLSX.read(bstr, { type: 'binary' });
        const wsname = wb.SheetNames[0];
        const ws = wb.Sheets[wsname];
        const parsedData = XLSX.utils.sheet_to_json(ws, { defval: '' });
        
        if (parsedData.length > 0) {
          const rawArr = XLSX.utils.sheet_to_json(ws, { header: 1 });
          if (rawArr.length > 0) {
            const cols = (rawArr[0] as string[]).filter(Boolean);
            setColumns(cols);
            setStatsColumn(cols[0] || '');
            setWordCloudColumn(cols[0] || '');
          }
          setData(parsedData);
          navigate('/manage');
        } else {
          setToast({ show: true, message: '上传的 Excel 文件为空', type: 'error' });
        }
      };
      reader.readAsBinaryString(file);
    }
    
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleExport = async () => {
    if (useBackend) {
      try {
        await patentApi.export({
          keyword: searchTerm,
          page: currentPage,
          size: remoteTotal,
        });
        setToast({ show: true, message: '导出成功！', type: 'success' });
      } catch (error) {
        setToast({ show: true, message: '导出失败，请重试', type: 'error' });
      }
    } else {
      const ws = XLSX.utils.json_to_sheet(data, { header: columns });
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "专利数据");
      XLSX.writeFile(wb, fileName || "专利导出数据.xlsx");
      setToast({ show: true, message: '导出成功！', type: 'success' });
    }
  };

  const openAddModal = () => {
    const newForm: any = {};
    if (useBackend) {
      ALL_PATENT_COLUMNS.forEach(col => { newForm[col.key] = ''; });
    } else {
      columns.forEach(col => { newForm[col] = ''; });
    }
    setFormData(newForm);
    setEditingIndex(null);
    setIsModalOpen(true);
  };

  const openEditModal = (item: any) => {
    setFormData({ ...item });
    setEditingIndex(useBackend ? item.id : data.findIndex(d => d === item));
    setIsModalOpen(true);
  };

  const handleDelete = async (item: any) => {
    setDeleteTarget(item);
    return;
    if (window.confirm("确定要删除这条专利记录吗？")) {
      if (useBackend && item.id) {
        try {
          const res = await patentApi.delete(String(item.id));
          if (res.code === 200 || res.code === 0) {
            setRemoteData(prev => prev.filter(r => r.id !== item.id));
            setToast({ show: true, message: '删除成功！', type: 'success' });
          } else {
            setToast({ show: true, message: res.message || '删除失败', type: 'error' });
          }
        } catch (error) {
          setToast({ show: true, message: '删除失败，请重试', type: 'error' });
        }
      } else {
        const originalIndex = data.findIndex(d => d === item);
        if (originalIndex !== -1) {
          const newData = [...data];
          newData.splice(originalIndex, 1);
          setData(newData);
          setToast({ show: true, message: '删除成功！', type: 'success' });
        }
      }
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    const item = deleteTarget;
    setDeleteLoading(true);
    try {
      if (useBackend && item.id) {
        const res = await patentApi.delete(String(item.id));
        if (res.code === 200 || res.code === 0) {
          setRemoteData(prev => prev.filter(r => r.id !== item.id));
          setToast({ show: true, message: '删除成功！', type: 'success' });
          setDeleteTarget(null);
        } else {
          setToast({ show: true, message: res.message || '删除失败', type: 'error' });
        }
      } else {
        const originalIndex = data.findIndex(d => d === item);
        if (originalIndex !== -1) {
          const newData = [...data];
          newData.splice(originalIndex, 1);
          setData(newData);
          setToast({ show: true, message: '删除成功！', type: 'success' });
          setDeleteTarget(null);
        }
      }
    } catch (error) {
      setToast({ show: true, message: '删除失败，请重试', type: 'error' });
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleSave = async () => {
    if (editingIndex !== null) {
      if (useBackend) {
        try {
          const res = await patentApi.update(formData);
          if (res.code === 200) {
            setToast({ show: true, message: '编辑成功！', type: 'success' });
            fetchPatents();
            setIsModalOpen(false);
          } else {
            setToast({ show: true, message: res.message || '编辑失败', type: 'error' });
          }
        } catch (error) {
          setToast({ show: true, message: '编辑失败，请重试', type: 'error' });
        }
      } else {
        const newData = [...data];
        newData[editingIndex] = formData;
        setData(newData);
        setIsModalOpen(false);
        setToast({ show: true, message: '编辑成功！', type: 'success' });
      }
    } else {
      if (useBackend) {
        try {
          const res = await patentApi.add(formData);
          if (res.code === 200) {
            setToast({ show: true, message: '添加成功！', type: 'success' });
            fetchPatents();
            setIsModalOpen(false);
          } else {
            setToast({ show: true, message: res.message || '添加失败', type: 'error' });
          }
        } catch (error) {
          setToast({ show: true, message: '添加失败，请重试', type: 'error' });
        }
      } else {
        setData([formData, ...data]);
        setIsModalOpen(false);
        setToast({ show: true, message: '添加成功！', type: 'success' });
      }
    }
  };

  // --- Computed Data ---
  const filteredData = useMemo(() => {
    const sourceData = useBackend ? remoteData : data;
    if (!searchTerm) return sourceData;
    const lowerSearch = searchTerm.toLowerCase();
    return sourceData.filter((row: any) => {
      if (searchField === '') {
        const keys = Object.keys(row);
        return keys.some(key => String(row[key] || '').toLowerCase().includes(lowerSearch));
      } else {
        return String(row[searchField] || '').toLowerCase().includes(lowerSearch);
      }
    });
  }, [useBackend, data, remoteData, searchTerm, searchField]);

  // Reset pagination when search term changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, searchField, data.length, itemsPerPage]);

  const paginatedData = useBackend ? filteredData : filteredData.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  const totalPages = useBackend ? Math.ceil(remoteTotal / itemsPerPage) || 1 : Math.ceil(filteredData.length / itemsPerPage) || 1;

  const statsData = useMemo(() => {
    if (useBackend) {
      return remoteStatsData;
    }

    if (!statsColumn || data.length === 0) return [];
    const counts: Record<string, number> = {};
    data.forEach(row => {
      const val = String(row[statsColumn] || '未知');
      counts[val] = (counts[val] || 0) + 1;
    });
    return Object.entries(counts)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 10);
  }, [useBackend, remoteStatsData, data, statsColumn]);

  const trendData = useMemo(() => {
    if (useBackend) {
      const yearMap = new Map<string, { name: string; value: number }>();
      remoteTrendData.forEach(item => {
        const key = `${item.year}-${item.name}`;
        if (!yearMap.has(key)) {
          yearMap.set(key, { name: `${item.year} ${item.name}`, value: item.count });
        }
      });
      return Array.from(yearMap.values())
        .sort((a, b) => a.name.localeCompare(b.name))
        .slice(-30);
    }

    if (!trendColumn || data.length === 0) return [];
    const counts: Record<string, number> = {};
    data.forEach(row => {
      const val = String(row[trendColumn] || '未知').trim();
      const match = val.match(/\d{4}/);
      const key = match ? match[0] : val;
      if (key) counts[key] = (counts[key] || 0) + 1;
    });
    return Object.entries(counts)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => a.name.localeCompare(b.name))
      .slice(-30);
  }, [useBackend, remoteTrendData, data, trendColumn]);

  useEffect(() => {
    if (useBackend && location.pathname === '/analyze') {
      fetchStatsData(statsColumn);
      fetchTrendData(trendColumn);
    }
  }, [useBackend, statsColumn, trendColumn, location.pathname, fetchStatsData, fetchTrendData]);

  useEffect(() => {
    if (useBackend && location.pathname === '/wordcloud') {
      fetchWordCloudData(wordCloudColumn);
    }
  }, [useBackend, wordCloudColumn, location.pathname, fetchWordCloudData]);

  const wordCloudData = useMemo(() => {
    if (useBackend) {
      return remoteWordCloudData.map(item => ({
        text: item.name.length > 15 ? item.name.substring(0, 15) + '...' : item.name,
        fullText: item.name,
        value: item.value,
        name: item.name
      }));
    }

    if (!wordCloudColumn || data.length === 0) return [];

    const counts: Record<string, { count: number, records: any[] }> = {};

    data.forEach(row => {
      const val = String(row[wordCloudColumn] || '').trim();
      if (!val) return;

      if (!counts[val]) {
        counts[val] = { count: 0, records: [] };
      }
      counts[val].count += 1;
      counts[val].records.push(row);
    });

    return Object.entries(counts)
      .map(([text, data]) => ({
        text: text.length > 15 ? text.substring(0, 15) + '...' : text,
        fullText: text,
        value: data.count,
        record: data.records[0]
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 80);
  }, [useBackend, remoteWordCloudData, data, wordCloudColumn]);

  const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#f43f5e', '#0ea5e9', '#84cc16', '#d946ef'];

  // --- Render Helpers ---
  const NavItem = ({ path, icon, label }: { path: string, icon: React.ReactNode, label: string }) => (
    <button
      onClick={() => navigate(path)}
      title={sidebarCollapsed ? label : undefined}
      className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
        location.pathname === path
          ? 'bg-blue-50 text-blue-600 font-medium'
          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
      } ${sidebarCollapsed ? 'justify-center' : ''}`}
    >
      {icon}
      {!sidebarCollapsed && <span>{label}</span>}
    </button>
  );

  return (
    <div className="h-screen bg-[#f8fafc] text-slate-900 font-sans flex overflow-hidden">
      {/* Toast Notification */}
      {toast.show && (
        <div className="fixed top-6 right-6 z-[100] animate-in slide-in-from-right">
          <div className={`px-6 py-4 rounded-xl shadow-lg flex items-center gap-3 ${
            toast.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : 'bg-red-50 text-red-800 border border-red-200'
          }`}>
            {toast.type === 'success' ? (
              <CheckCircle size={20} className="text-green-600 shrink-0" />
            ) : (
              <AlertCircle size={20} className="text-red-600 shrink-0" />
            )}
            <span className="font-medium">{toast.message}</span>
            <button
              onClick={() => setToast({ ...toast, show: false })}
              className="ml-2 text-slate-400 hover:text-slate-600 transition-colors"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div className="fixed inset-0 z-[120] flex items-center justify-center px-4">
          <div className="absolute inset-0 bg-slate-900/35 backdrop-blur-sm" onClick={() => !deleteLoading && setDeleteTarget(null)} />
          <div className="relative w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl border border-slate-200 animate-in zoom-in-95 duration-200">
            <div className="p-6">
              <div className="flex items-start gap-4">
                <div className="w-11 h-11 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                  <Trash2 size={22} />
                </div>
                <div className="min-w-0">
                  <h3 className="text-lg font-semibold text-slate-900">删除专利记录</h3>
                  <p className="mt-1 text-sm leading-6 text-slate-500">
                    确定要删除这条专利记录吗？删除后该记录及其拆分字段数据将从列表中移除。
                  </p>
                  <div className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-700 truncate">
                    {deleteTarget.title || deleteTarget.applicationNumber || deleteTarget.serialNumber || '未命名专利'}
                  </div>
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50 px-6 py-4">
              <button
                onClick={() => setDeleteTarget(null)}
                disabled={deleteLoading}
                className="px-4 py-2 text-sm font-medium text-slate-700 hover:bg-white border border-transparent hover:border-slate-200 rounded-lg transition-colors disabled:opacity-50"
              >
                取消
              </button>
              <button
                onClick={confirmDelete}
                disabled={deleteLoading}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors disabled:opacity-60"
              >
                {deleteLoading && <Loader2 size={16} className="animate-spin" />}
                删除
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Sidebar */}
      <aside className={`${sidebarCollapsed ? 'w-20' : 'w-64'} shrink-0 bg-white border-r border-slate-200 h-full z-20 flex flex-col shadow-[4px_0_24px_rgba(0,0,0,0.02)] transition-all duration-300`}>
        <div className="h-20 shrink-0 flex items-center px-4 border-b border-slate-100">
          <div className="bg-blue-600 p-2 rounded-lg text-white shadow-sm shadow-blue-200 shrink-0">
            <LayoutDashboard size={20} />
          </div>
          {!sidebarCollapsed && <h1 className="text-lg font-bold tracking-tight text-slate-800 ml-3 truncate">专利管理分析系统</h1>}
        </div>
        
        <nav className="flex-1 overflow-y-auto p-4 space-y-2">
          <NavItem path="/manage" icon={<Database size={20} />} label="管理专利" />
          <NavItem path="/analyze" icon={<PieChartIcon size={20} />} label="统计分析" />
          <NavItem path="/wordcloud" icon={<Cloud size={20} />} label="词云分析" />
          <NavItem path="/graph" icon={<Network size={20} />} label="知识图谱" />
          <NavItem path="/chat" icon={<MessageSquare size={20} />} label="智能助手" />
        </nav>

        <div className="p-4 border-t border-slate-100">
          <button
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 text-slate-500 hover:text-slate-700 hover:bg-slate-50 rounded-xl transition-colors"
          >
            {sidebarCollapsed ? <ChevronRight size={20} /> : <ChevronLeft size={20} />}
            {!sidebarCollapsed && <span className="text-sm">收起菜单</span>}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 h-full flex flex-col min-w-0">
        {/* Top Header */}
        <header className="h-20 shrink-0 bg-white/80 backdrop-blur-md border-b border-slate-200 z-50 flex items-center justify-between px-8">
          <h2 className="text-xl font-semibold text-slate-800">
            {location.pathname === '/manage' && '专利数据管理'}
            {location.pathname === '/analyze' && '多维统计分析'}
            {location.pathname === '/wordcloud' && '3D 文本词云分析'}
            {location.pathname === '/graph' && '专利知识图谱'}
            {location.pathname === '/chat' && '智能助手'}
            {location.pathname === '/settings' && '模型配置'}
          </h2>

          {location.pathname === '/manage' && (
            <div className="relative w-full max-w-2xl mx-4 hidden md:flex items-center shadow-sm rounded-full">
              <div className="relative shrink-0" ref={searchDropdownRef}>
                <button
                  type="button"
                  onClick={() => setIsSearchDropdownOpen(!isSearchDropdownOpen)}
                  className={`flex items-center gap-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 border border-r-0 border-slate-200 rounded-l-full text-sm font-medium text-slate-700 transition-colors focus:outline-none h-full`}
                >
                  <span className="truncate max-w-[100px]">
                    {SEARCH_DIMENSIONS.find(d => d.value === searchField)?.label || '全部字段'}
                  </span>
                  <ChevronDown size={14} className={`text-slate-500 transition-transform duration-200 ${isSearchDropdownOpen ? 'rotate-180' : ''}`} />
                </button>

                {isSearchDropdownOpen && (
                  <div className="absolute top-full left-0 mt-2 w-40 bg-white border border-slate-100 rounded-xl shadow-[0_10px_40px_-10px_rgba(0,0,0,0.1)] overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                    <div className="py-1.5">
                      {SEARCH_DIMENSIONS.map((dim) => (
                        <button
                          key={dim.value}
                          onClick={() => {
                            setSearchField(dim.value);
                            setIsSearchDropdownOpen(false);
                          }}
                          className={`w-full flex items-center justify-between px-4 py-2.5 text-sm transition-colors ${
                            searchField === dim.value
                              ? 'bg-blue-50 text-blue-700 font-semibold'
                              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                          }`}
                        >
                          <span className="truncate">{dim.label}</span>
                          {searchField === dim.value && <Check size={16} className="text-blue-600 shrink-0" />}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                <input
                  type="text"
                  placeholder={searchField === '' ? "全局搜索专利信息..." : `在 ${SEARCH_DIMENSIONS.find(d => d.value === searchField)?.label} 中搜索...`}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-r-full text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
                />
              </div>
            </div>
          )}
          
          <div className="flex items-center gap-3">
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={importLoading}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-blue-700 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors shadow-sm disabled:opacity-50"
              style={{ display: location.pathname === '/manage' ? 'inline-flex' : 'none' }}
            >
              {importLoading ? <Loader2 size={16} className="animate-spin" /> : <Upload size={16} />}
              {importLoading ? '导入中...' : '导入数据'}
            </button>
            {data.length > 0 && location.pathname === '/manage' && (
              <>
                <button
                  onClick={handleExport}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors shadow-sm"
                >
                  <Download size={16} />
                  导出 Excel
                </button>
                <button
                  onClick={openAddModal}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
                >
                  <Plus size={16} />
                  新增专利
                </button>
              </>
            )}
          </div>
        </header>

        {/* Module Content */}
        <div className="p-8 flex-1 overflow-y-auto min-h-0 relative">
          
          {/* Global Hidden File Input */}
          <input
            type="file"
            accept=".xlsx, .xls, .csv"
            className="hidden"
            ref={fileInputRef}
            onChange={handleFileUpload}
          />

          {/* 1. Manage Module */}
          {location.pathname === '/manage' && (
            <PatentDataManagement
              data={useBackend ? remoteData : data}
              columns={useBackend ? PATENT_COLUMNS.map(c => c.key) : columns}
              allColumns={useBackend ? PATENT_COLUMNS : columns.map(col => ({ key: col, label: col }))}
              defaultVisibleColumns={useBackend ? PATENT_COLUMNS.map(c => c.key) : columns}
              searchTerm={searchTerm}
              setSearchTerm={setSearchTerm}
              searchField={searchField}
              setSearchField={setSearchField}
              searchFieldOptions={SEARCH_DIMENSIONS}
              filteredData={filteredData}
              paginatedData={paginatedData}
              currentPage={currentPage}
              setCurrentPage={setCurrentPage}
              totalPages={totalPages}
              itemsPerPage={itemsPerPage}
              setItemsPerPage={setItemsPerPage}
              setViewingRecord={setViewingRecord}
              setIsViewModalOpen={setIsViewModalOpen}
              openEditModal={openEditModal}
              handleDelete={handleDelete}
              onImportClick={() => fileInputRef.current?.click()}
              loading={useBackend ? remoteLoading : false}
              total={useBackend ? remoteTotal : filteredData.length}
            />
          )}

          {/* 3. Analyze Module */}
          {location.pathname === '/analyze' && (
              <div className="space-y-8 pb-12">
                {/* Stats Section */}
                <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-8">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
                    <div>
                      <h3 className="text-xl font-semibold text-slate-800 flex items-center gap-2">
                        <BarChart3 className="text-blue-600" size={24} />
                        数据分布统计
                      </h3>
                      <p className="text-sm text-slate-500 mt-1">分析特定字段（如：专利类型、申请人、年份等）的频次分布。</p>
                    </div>
                    <div className="flex items-center gap-3 bg-slate-50 p-2 rounded-xl border border-slate-100">
                      <label className="text-sm font-medium text-slate-700 pl-2">分析维度:</label>
                      <CustomSelect
                        value={statsColumn}
                        onChange={setStatsColumn}
                        options={useBackend ? STATISTICS_COLUMNS.map(c => c.key) : columns}
                        labels={useBackend ? STATISTICS_COLUMNS.reduce((acc, c) => ({ ...acc, [c.key]: c.label }), {} as Record<string, string>) : undefined}
                      />
                    </div>
                  </div>

                  {statsData.length === 0 ? (
                    <div className="text-center py-16 text-slate-500 bg-slate-50 rounded-xl border border-slate-100">
                      暂无数据
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
                      <div className="h-[350px] w-full">
                        <h4 className="text-sm font-semibold text-slate-600 mb-6 text-center bg-slate-50 py-2 rounded-lg">柱状图 (TOP 10)</h4>
                        <ResponsiveContainer width="100%" height="100%">
                          <BarChart data={statsData} margin={{ top: 5, right: 30, left: 20, bottom: 25 }}>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                            <XAxis 
                              dataKey="name" 
                              tick={{fontSize: 12, fill: '#64748b'}} 
                              tickLine={false} 
                              axisLine={{stroke: '#cbd5e1'}}
                              angle={-45}
                              textAnchor="end"
                              height={60}
                            />
                            <YAxis tick={{fontSize: 12, fill: '#64748b'}} tickLine={false} axisLine={{stroke: '#cbd5e1'}} />
                            <Tooltip 
                              cursor={{fill: '#f8fafc'}}
                              contentStyle={{borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)'}}
                            />
                            <Bar dataKey="value" fill="#3b82f6" radius={[6, 6, 0, 0]} onClick={(data) => {
                              setSearchField(statsColumn);
                              setSearchTerm(String(data.name));
                              navigate('/manage');
                            }}>
                              {statsData.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                              ))}
                            </Bar>
                          </BarChart>
                        </ResponsiveContainer>
                      </div>
                      <div className="h-[350px] w-full">
                        <h4 className="text-sm font-semibold text-slate-600 mb-6 text-center bg-slate-50 py-2 rounded-lg">饼图占比 (TOP 10)</h4>
                        <ResponsiveContainer width="100%" height="100%">
                          <PieChart>
                            <Pie
                              data={statsData}
                              cx="50%"
                              cy="50%"
                              innerRadius={80}
                              outerRadius={120}
                              paddingAngle={3}
                              dataKey="value"
                              onClick={(data) => {
                                setSearchField(statsColumn);
                                setSearchTerm(String(data.name));
                                navigate('/manage');
                              }}
                            >
                              {statsData.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                              ))}
                            </Pie>
                            <Tooltip 
                              contentStyle={{borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)'}}
                            />
                            <Legend wrapperStyle={{fontSize: '12px', paddingTop: '20px'}} />
                          </PieChart>
                        </ResponsiveContainer>
                      </div>
                    </div>
                  )}
                </div>

                {/* Trend Analysis Section */}
                <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-8">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
                    <div>
                      <h3 className="text-xl font-semibold text-slate-800 flex items-center gap-2">
                        <TrendingUp className="text-blue-600" size={24} />
                        趋势与周期分析
                      </h3>
                      <p className="text-sm text-slate-500 mt-1">分析时间、年份或连续型字段的趋势变化。</p>
                    </div>
                    <div className="flex items-center gap-3 bg-slate-50 p-2 rounded-xl border border-slate-100">
                      <label className="text-sm font-medium text-slate-700 pl-2">趋势字段:</label>
                      <CustomSelect
                        value={trendColumn}
                        onChange={setTrendColumn}
                        options={useBackend ? TREND_COLUMNS.map(c => c.key!) : columns}
                        labels={useBackend ? TREND_COLUMNS.reduce((acc, c) => ({ ...acc, [c.key!]: c.label }), {} as Record<string, string>) : undefined}
                      />
                    </div>
                  </div>

                  <div className="w-full h-[350px]">
                    {trendData.length === 0 ? (
                      <div className="h-full flex items-center justify-center text-slate-500 bg-slate-50 rounded-xl border border-slate-100">
                        暂无数据
                      </div>
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={trendData} margin={{ top: 5, right: 30, left: 20, bottom: 25 }}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                          <XAxis 
                            dataKey="name" 
                            tick={{fontSize: 10, fill: '#64748b'}} 
                            tickLine={false} 
                            axisLine={{stroke: '#cbd5e1'}}
                            angle={-45}
                            textAnchor="end"
                            height={80}
                            interval={0}
                          />
                          <YAxis tick={{fontSize: 12, fill: '#64748b'}} tickLine={false} axisLine={{stroke: '#cbd5e1'}} />
                          <Tooltip 
                            cursor={{stroke: '#cbd5e1', strokeWidth: 1, strokeDasharray: '3 3'}}
                            contentStyle={{borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)'}}
                          />
                          <Legend wrapperStyle={{fontSize: '12px', paddingTop: '10px'}} />
                          <Line type="monotone" dataKey="value" stroke="#3b82f6" strokeWidth={2} dot={{r: 3, fill: '#3b82f6', strokeWidth: 2, stroke: '#fff'}} activeDot={{r: 5}} />
                        </LineChart>
                      </ResponsiveContainer>
                    )}
                  </div>
                </div>
              </div>
          )}

          {/* 4. Word Cloud Module */}
          {location.pathname === '/wordcloud' && (
              <div className="space-y-8 pb-12 h-full flex flex-col">
                <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-8 flex-1 flex flex-col">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4 shrink-0">
                    <div>
                      <h3 className="text-xl font-semibold text-slate-800 flex items-center gap-2">
                        <Cloud className="text-blue-600" size={24} />
                        3D 文本词云分析
                      </h3>
                      <p className="text-sm text-slate-500 mt-1">提取长文本字段（如：摘要、权利要求、标题等）的高频词汇，生成可交互的 3D 球体。</p>
                    </div>
                    <div className="flex items-center gap-3 bg-slate-50 p-2 rounded-xl border border-slate-100">
                      <label className="text-sm font-medium text-slate-700 pl-2">分析维度:</label>
                      <CustomSelect
                        value={wordCloudColumn}
                        onChange={setWordCloudColumn}
                        options={useBackend ? WORD_CLOUD_COLUMNS.map(c => c.key!) : columns}
                        labels={useBackend ? WORD_CLOUD_COLUMNS.reduce((acc, c) => ({ ...acc, [c.key!]: c.label }), {} as Record<string, string>) : undefined}
                      />
                    </div>
                  </div>

                  <div className="w-full flex-1 min-h-[500px] border border-slate-100 rounded-xl bg-white flex items-center justify-center overflow-hidden relative shadow-sm">
                    {wordCloudData.length === 0 ? (
                      <div className="text-slate-400">暂无数据</div>
                    ) : (
                      <SphereWordCloud
                        words={wordCloudData}
                        radius={250}
                        onWordClick={(word) => {
                          if (useBackend) {
                            const searchValue = word.fullText || word.name;
                            setSearchField(wordCloudColumn);
                            setSearchTerm(searchValue);
                            navigate('/manage');
                          } else if (word.record) {
                            setViewingRecord(word.record);
                            setIsViewModalOpen(true);
                          }
                        }}
                      />
                    )}
                  </div>
                </div>
              </div>
          )}

          {location.pathname === '/chat' && (
            <div className="h-full">
              <ChatContainer
                title="专利智能助手"
                subtitle="我可以帮你解答专利相关问题、分析专利数据、提供申请建议。请输入你的问题开始对话。"
                welcomeMessage="你好！我是专利智能助手"
                onToast={(type, message) => setToast({ show: true, message, type: type === 'error' ? 'error' : 'success' })}
              />
            </div>
          )}

          {location.pathname === '/settings' && (
            <div className="h-full -m-8">
              <ModelConfigPage />
            </div>
          )}

          {location.pathname === '/graph' && (
            <div className="absolute inset-0">
              <GraphVisualization />
            </div>
          )}
        </div>
      </main>

      {/* Modal for CRUD */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
          <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
          <div className="relative w-full max-w-4xl bg-white rounded-2xl shadow-2xl flex flex-col max-h-[90vh] transform transition-all">
            <div className="flex items-center justify-between px-8 py-5 border-b border-slate-100">
              <h3 className="text-xl font-semibold text-slate-800">
                {editingIndex !== null ? '编辑专利记录' : '新增专利记录'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 transition-colors p-2 rounded-lg hover:bg-slate-100"
              >
                <X size={20} />
              </button>
            </div>
            
            <div className="p-8 overflow-y-auto flex-1 custom-scrollbar">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-5">
                {(useBackend ? ALL_PATENT_COLUMNS : columns.map(col => ({ key: col, label: col }))).map((col: any, i: number) => (
                  <div key={i} className="space-y-1.5">
                    <label className="block text-sm font-medium text-slate-700 truncate" title={col.label}>
                      {col.label}
                    </label>
                    <input
                      type={col.inputType || 'text'}
                      value={formData[col.key || col] || ''}
                      onChange={(e) => setFormData({ ...formData, [col.key || col]: e.target.value })}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 placeholder-slate-400
                               focus:outline-none focus:bg-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all"
                      placeholder={`输入 ${col.label}`}
                    />
                  </div>
                ))}
              </div>
            </div>
            
            <div className="px-8 py-5 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-3">
              <button
                onClick={() => setIsModalOpen(false)}
                className="px-6 py-2.5 text-sm font-medium text-slate-600 bg-white border border-slate-300 rounded-xl hover:bg-slate-50 transition-colors shadow-sm"
              >
                取消
              </button>
              <button
                onClick={handleSave}
                className="inline-flex items-center gap-2 px-6 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-xl hover:bg-blue-700 transition-colors shadow-sm"
              >
                <Save size={18} />
                保存记录
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal for View Details */}
      {isViewModalOpen && viewingRecord && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
          <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={() => setIsViewModalOpen(false)} />
          <div className="relative w-full max-w-5xl bg-white rounded-2xl shadow-2xl flex flex-col max-h-[90vh] transform transition-all">
            <div className="flex items-center justify-between px-8 py-5 border-b border-slate-100">
              <h3 className="text-xl font-semibold text-slate-800">
                专利详情
              </h3>
              <button
                onClick={() => setIsViewModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 transition-colors p-2 rounded-lg hover:bg-slate-100"
              >
                <X size={20} />
              </button>
            </div>

            <div className="p-8 overflow-y-auto flex-1 custom-scrollbar">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-8 gap-y-6">
                {(useBackend ? ALL_PATENT_COLUMNS : columns.map(col => ({ key: col, label: col }))).map((col: any, i: number) => {
                  const val = viewingRecord[col.key || col];
                  const displayVal = val !== undefined && val !== null && String(val).trim() !== '' ? String(val) : '-';
                  return (
                    <div key={i} className="space-y-1.5 border-b border-slate-100 pb-3">
                      <label className="block text-sm font-medium text-slate-500">
                        {col.label}
                      </label>
                      <div className="text-sm text-slate-800 break-words">
                        {displayVal}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="px-8 py-5 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-3">
              <button
                onClick={() => setIsViewModalOpen(false)}
                className="px-6 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-xl hover:bg-blue-700 transition-colors shadow-sm"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
