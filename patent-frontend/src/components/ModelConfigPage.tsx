import React, { useState, useEffect } from 'react';
import { Settings, Key, Cpu, Save, Eye, EyeOff, CheckCircle, AlertCircle, RefreshCw, Wifi, WifiOff } from 'lucide-react';

interface ModelConfig {
  apiKey: string;
  model: string;
  baseUrl: string;
}

const API_BASE = '/api/config';

const DEFAULT_CONFIG: ModelConfig = {
  apiKey: '',
  model: 'gpt-4o',
  baseUrl: 'https://api.openai.com/v1'
};

export const ModelConfigPage: React.FC = () => {
  const [config, setConfig] = useState<ModelConfig>(DEFAULT_CONFIG);
  const [showApiKey, setShowApiKey] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'success' | 'error'>('idle');
  const [testStatus, setTestStatus] = useState<'idle' | 'testing' | 'success' | 'error'>('idle');
  const [testMessage, setTestMessage] = useState('');

  useEffect(() => {
    fetch(`${API_BASE}/model`)
      .then(res => res.json())
      .then(data => {
        if (data && data.baseUrl) {
          setConfig({
            apiKey: data.apiKey || '',
            model: data.model || DEFAULT_CONFIG.model,
            baseUrl: data.baseUrl || DEFAULT_CONFIG.baseUrl,
          });
        }
      })
      .catch(() => {});
  }, []);

  const handleSave = async () => {
    setSaveStatus('saving');
    try {
      const res = await fetch(`${API_BASE}/model`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config),
      });
      const result = await res.json();
      setSaveStatus(result.success ? 'success' : 'error');
      setTimeout(() => setSaveStatus('idle'), 2000);
    } catch (e) {
      setSaveStatus('error');
      setTimeout(() => setSaveStatus('idle'), 2000);
    }
  };

  const handleReset = () => {
    setConfig(DEFAULT_CONFIG);
  };

  const handleTestConnection = async () => {
    if (!config.apiKey || !config.baseUrl || !config.model) {
      setTestStatus('error');
      setTestMessage('请填写完整的配置信息');
      setTimeout(() => setTestStatus('idle'), 3000);
      return;
    }

    setTestStatus('testing');
    setTestMessage('');

    try {
      const response = await fetch(`${API_BASE}/test`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
      });

      const result = await response.json();
      
      if (result.success) {
        setTestStatus('success');
        setTestMessage(result.message || '连接成功！');
      } else {
        setTestStatus('error');
        setTestMessage(result.message || '连接失败');
      }
    } catch (e: any) {
      setTestStatus('error');
      setTestMessage(`连接失败: ${e.message || '网络错误'}`);
    }

    setTimeout(() => {
      setTestStatus('idle');
      setTestMessage('');
    }, 5000);
  };

  return (
    <div className="h-full bg-gradient-to-br from-slate-50 via-white to-blue-50 p-8 overflow-auto">
      <div className="max-w-2xl mx-auto">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2.5 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl shadow-lg shadow-blue-500/20">
              <Settings className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-800">模型配置</h1>
              <p className="text-sm text-slate-500">配置 AI 模型的 API 密钥和参数</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-6 border-b border-slate-100">
            <h2 className="text-lg font-semibold text-slate-800 flex items-center gap-2">
              <Key className="w-5 h-5 text-blue-500" />
              API 配置
            </h2>
            <p className="text-sm text-slate-500 mt-1">配置模型的 API 密钥和基础 URL</p>
          </div>

          <div className="p-6 space-y-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                API Key
              </label>
              <div className="relative">
                <input
                  type={showApiKey ? 'text' : 'password'}
                  value={config.apiKey}
                  onChange={(e) => setConfig(prev => ({ ...prev, apiKey: e.target.value }))}
                  placeholder="请输入 API Key"
                  className="w-full px-4 py-3 pr-12 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <button
                  type="button"
                  onClick={() => setShowApiKey(!showApiKey)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showApiKey ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
              <p className="text-xs text-slate-400 mt-2">
                API Key 将保存在本地浏览器中，不会上传到服务器
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Base URL
              </label>
              <input
                type="text"
                value={config.baseUrl}
                onChange={(e) => setConfig(prev => ({ ...prev, baseUrl: e.target.value }))}
                placeholder="https://api.openai.com/v1"
                className="w-full px-4 py-3 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-slate-400 mt-2">
                默认为 OpenAI API 地址，使用其他服务请修改此地址
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden mt-6">
          <div className="p-6 border-b border-slate-100">
            <h2 className="text-lg font-semibold text-slate-800 flex items-center gap-2">
              <Cpu className="w-5 h-5 text-blue-500" />
              模型配置
            </h2>
            <p className="text-sm text-slate-500 mt-1">配置使用的模型名称</p>
          </div>

          <div className="p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                模型名称
              </label>
              <input
                type="text"
                value={config.model}
                onChange={(e) => setConfig(prev => ({ ...prev, model: e.target.value }))}
                placeholder="例如: gpt-4o, deepseek-chat, qwen-max"
                className="w-full px-4 py-3 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-slate-400 mt-2">
                输入模型名称，如 gpt-4o、deepseek-chat、qwen-max 等
              </p>
            </div>

            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <p className="text-sm text-slate-600">
                当前模型: <span className="font-medium text-slate-800">{config.model || '未设置'}</span>
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between mt-6">
          <button
            onClick={handleReset}
            className="px-4 py-2.5 text-sm text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors"
          >
            重置为默认
          </button>

          <div className="flex items-center gap-3">
            {saveStatus === 'success' && (
              <div className="flex items-center gap-2 text-emerald-600">
                <CheckCircle className="w-5 h-5" />
                <span className="text-sm font-medium">保存成功</span>
              </div>
            )}
            {saveStatus === 'error' && (
              <div className="flex items-center gap-2 text-red-500">
                <AlertCircle className="w-5 h-5" />
                <span className="text-sm font-medium">保存失败</span>
              </div>
            )}
            <button
              onClick={handleSave}
              disabled={saveStatus === 'saving'}
              className="flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-blue-600 to-blue-500 text-white rounded-xl hover:from-blue-700 hover:to-blue-600 disabled:opacity-50 transition-all shadow-lg shadow-blue-500/20"
            >
              {saveStatus === 'saving' ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  保存中...
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  保存配置
                </>
              )}
            </button>
          </div>
        </div>

        <div className="mt-6 bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-slate-800 flex items-center gap-2">
                  {testStatus === 'success' ? (
                    <Wifi className="w-5 h-5 text-emerald-500" />
                  ) : testStatus === 'error' ? (
                    <WifiOff className="w-5 h-5 text-red-500" />
                  ) : (
                    <Wifi className="w-5 h-5 text-blue-500" />
                  )}
                  连通性测试
                </h3>
                <p className="text-sm text-slate-500 mt-1">测试 API 配置是否正确</p>
              </div>
              <button
                onClick={handleTestConnection}
                disabled={testStatus === 'testing'}
                className="flex items-center gap-2 px-5 py-2.5 bg-slate-800 text-white rounded-xl hover:bg-slate-700 disabled:opacity-50 transition-colors"
              >
                {testStatus === 'testing' ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    测试中...
                  </>
                ) : (
                  <>
                    <Wifi className="w-4 h-4" />
                    测试连接
                  </>
                )}
              </button>
            </div>

            {testMessage && (
              <div className={`mt-4 p-4 rounded-xl ${
                testStatus === 'success' 
                  ? 'bg-emerald-50 border border-emerald-100' 
                  : 'bg-red-50 border border-red-100'
              }`}>
                <div className={`flex items-center gap-2 ${
                  testStatus === 'success' ? 'text-emerald-700' : 'text-red-600'
                }`}>
                  {testStatus === 'success' ? (
                    <CheckCircle className="w-5 h-5" />
                  ) : (
                    <AlertCircle className="w-5 h-5" />
                  )}
                  <span className="text-sm font-medium">{testMessage}</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
