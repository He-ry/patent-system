import React from 'react';
import { Search, List, Edit2, Trash2, Database, Loader2 } from 'lucide-react';
import { PatentInfo, PatentQueryRequest, ALL_PATENT_COLUMNS, DEFAULT_VISIBLE_COLUMNS } from './api';
import { CustomSelect } from './CustomSelect';

interface PatentDataManagementProps {
  data: PatentInfo[];
  total: number;
  loading: boolean;
  page: number;
  setPage: (page: number) => void;
  size: number;
  setSize: (size: number) => void;
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  searchField: keyof PatentQueryRequest | '';
  setSearchField: (field: keyof PatentQueryRequest | '') => void;
  visibleColumns: (keyof PatentInfo)[];
  setVisibleColumns: (columns: (keyof PatentInfo)[]) => void;
  openEditModal: (record: PatentInfo) => void;
  handleDelete: (id: string | undefined) => void;
}

const SEARCH_FIELD_LABELS: Record<string, string> = {
  '': '全部字段',
  'college': '学院',
  'ipcMainClassInterpretation': 'IPC分类释义',
  'inventor': '发明人',
  'applicationFieldClassification': '应用领域',
  'technicalSubjectClassification': '技术主题',
};

export const PatentDataManagement: React.FC<PatentDataManagementProps> = ({
  data,
  total,
  loading,
  page,
  setPage,
  size,
  setSize,
  searchTerm,
  setSearchTerm,
  searchField,
  setSearchField,
  visibleColumns,
  setVisibleColumns,
  openEditModal,
  handleDelete,
}) => {
  const totalPages = Math.ceil(total / size);
  const [isColumnSelectorOpen, setIsColumnSelectorOpen] = React.useState(false);

  const renderCellValue = (col: keyof PatentInfo, value: any) => {
    if (col === 'patentType' || col === 'legalStatus') {
      return (
        <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
          value === '授权' ? 'bg-green-50 text-green-700' :
          value === '实质审查' ? 'bg-yellow-50 text-yellow-700' :
          value === '发明专利' ? 'bg-blue-50 text-blue-700' :
          value === '实用新型' ? 'bg-purple-50 text-purple-700' :
          value === '外观设计' ? 'bg-orange-50 text-orange-700' :
          'bg-slate-100 text-slate-600'
        }`}>{value || '-'}</span>
      );
    }
    
    if (col === 'applicationDate' || col === 'grantDate' || col === 'publicationDate' || col === 'expiryDate' || col === 'transferEffectiveDate' || col === 'licenseEffectiveDate') {
      return value ? String(value).substring(0, 10) : '-';
    }
    
    return value || '-';
  };

  return (
    <div className="bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-col h-full overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center bg-slate-50/50 shrink-0">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3 bg-slate-50 p-2 rounded-xl border border-slate-100">
            <label className="text-sm font-medium text-slate-700 pl-2">搜索字段:</label>
            <CustomSelect
              value={searchField}
              onChange={(val) => {
                setSearchField(val as keyof PatentQueryRequest | '');
                setPage(1);
              }}
              options={['', 'college', 'ipcMainClassInterpretation', 'inventor', 'applicationFieldClassification', 'technicalSubjectClassification']}
              labels={SEARCH_FIELD_LABELS}
              placeholder="全部字段"
            />
          </div>
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              type="text" 
              placeholder="搜索专利信息..." 
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(1);
              }}
              className="w-full pl-9 pr-4 py-2 bg-white border border-slate-300 rounded-lg text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
            />
          </div>
          <div className="relative">
            <button
              onClick={() => setIsColumnSelectorOpen(!isColumnSelectorOpen)}
              className="inline-flex items-center gap-2 px-3 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              <List size={16} />
              选择列 ({visibleColumns.length})
            </button>
            {isColumnSelectorOpen && (
              <>
                <div className="fixed inset-0 z-10" onClick={() => setIsColumnSelectorOpen(false)} />
                <div className="absolute left-0 top-full mt-2 w-80 bg-white border border-slate-200 rounded-xl shadow-lg z-20 max-h-[60vh] overflow-y-auto custom-scrollbar">
                  <div className="p-3 border-b border-slate-100 sticky top-0 bg-white">
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium text-slate-700">选择显示列</span>
                      <button
                        onClick={() => setVisibleColumns(DEFAULT_VISIBLE_COLUMNS)}
                        className="text-xs text-blue-600 hover:text-blue-700"
                      >
                        重置
                      </button>
                    </div>
                  </div>
                  <div className="p-2">
                    {ALL_PATENT_COLUMNS.map((col) => (
                      <label
                        key={col.key}
                        className="flex items-center gap-2 px-2 py-1.5 hover:bg-slate-50 rounded-lg cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={visibleColumns.includes(col.key)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setVisibleColumns([...visibleColumns, col.key]);
                            } else {
                              if (visibleColumns.length > 1) {
                                setVisibleColumns(visibleColumns.filter(k => k !== col.key));
                              }
                            }
                          }}
                          className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500"
                        />
                        <span className="text-sm text-slate-700">{col.label}</span>
                      </label>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-500">共 <span className="font-semibold text-slate-700">{total}</span> 条</span>
        </div>
      </div>
      
      <div className="flex-1 min-h-0">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 size={32} className="animate-spin text-blue-600" />
          </div>
        ) : (
          <div className="h-full overflow-auto border-t border-slate-100">
            <table className="border-collapse">
              <thead className="bg-slate-50 sticky top-0 z-10">
                <tr>
                  {visibleColumns.map((col) => (
                    <th key={col} className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap border-b border-slate-200 bg-slate-50">
                      {ALL_PATENT_COLUMNS.find(c => c.key === col)?.label || col}
                    </th>
                  ))}
                  <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap border-b border-slate-200 bg-slate-50">操作</th>
                </tr>
              </thead>
              <tbody className="bg-white">
                {data.length === 0 ? (
                  <tr>
                    <td colSpan={visibleColumns.length + 1} className="px-4 py-16 text-center text-slate-500">
                      <Database size={40} className="mx-auto mb-3 text-slate-300" />
                      <p className="text-base font-medium text-slate-600 mb-1">暂无专利数据</p>
                      <p className="text-sm text-slate-400">点击右上角「导入数据」或「新增专利」添加数据</p>
                    </td>
                  </tr>
                ) : (
                  data.map((row, index) => (
                    <tr key={row.id || index} className="hover:bg-slate-50/50 transition-colors border-b border-slate-100">
                      {visibleColumns.map((col) => {
                        const value = (row as any)[col];
                        return (
                          <td key={col} className="px-4 py-3 whitespace-nowrap text-sm text-slate-600" title={value}>
                            {renderCellValue(col, value)}
                          </td>
                        );
                      })}
                      <td className="px-4 py-3 whitespace-nowrap text-center">
                        <div className="flex items-center justify-center gap-2">
                          <button
                            onClick={() => openEditModal(row)}
                            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="编辑"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => handleDelete(row.id)}
                            className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="删除"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <div className="px-6 py-4 border-t border-slate-200 bg-white shrink-0">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <span className="text-sm text-slate-500">每页显示</span>
              <div className="flex items-center bg-slate-50 rounded-lg p-1 border border-slate-200">
                {[10, 20, 50, 100].map((s) => (
                  <button
                    key={s}
                    onClick={() => { setSize(s); setPage(1); }}
                    className={`px-3 py-1.5 text-sm font-medium rounded-md transition-all ${
                      size === s
                        ? 'bg-white text-blue-600 shadow-sm'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
              <span className="text-sm text-slate-500">条</span>
            </div>

            <div className="flex items-center gap-1">
              <button
                onClick={() => setPage(1)}
                disabled={page === 1}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="首页"
              >
                «
              </button>
              <button
                onClick={() => setPage(Math.max(1, page - 1))}
                disabled={page === 1}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="上一页"
              >
                ‹
              </button>

              <div className="flex items-center gap-1 mx-2">
                {(() => {
                  const pages: (number | string)[] = [];
                  const showPages = 5;
                  
                  if (totalPages <= showPages + 2) {
                    for (let i = 1; i <= totalPages; i++) pages.push(i);
                  } else {
                    pages.push(1);
                    if (page > 3) pages.push('...');
                    const start = Math.max(2, page - 1);
                    const end = Math.min(totalPages - 1, page + 1);
                    for (let i = start; i <= end; i++) pages.push(i);
                    if (page < totalPages - 2) pages.push('...');
                    pages.push(totalPages);
                  }
                  
                  return pages.map((p, i) => (
                    p === '...' ? (
                      <span key={`ellipsis-${i}`} className="px-2 text-slate-400">...</span>
                    ) : (
                      <button
                        key={p}
                        onClick={() => setPage(p as number)}
                        className={`w-9 h-9 flex items-center justify-center text-sm font-medium rounded-lg transition-all ${
                          page === p
                            ? 'bg-blue-600 text-white shadow-sm'
                            : 'text-slate-600 hover:bg-slate-100'
                        }`}
                      >
                        {p}
                      </button>
                    )
                  ));
                })()}
              </div>

              <button
                onClick={() => setPage(Math.min(totalPages, page + 1))}
                disabled={page === totalPages}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="下一页"
              >
                ›
              </button>
              <button
                onClick={() => setPage(totalPages)}
                disabled={page === totalPages}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="末页"
              >
                »
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
