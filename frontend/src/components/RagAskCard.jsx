import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { elderFetch, getToken } from '../utils/elderApi';

/**
 * 用药问问 - RAG 问答卡片（老人端）
 *
 * 定位：吃药前，问一问（用药知识问答，与"紧急助手-突发急救"区分）
 * 交互：提问 → 知识库检索 → AI 流式生成（打字机效果，老人等待时有实时反馈）
 * 老人友好设计：大字号、条目化排版、语音播报、快捷问题（少打字）
 */

// 快捷问题：老人少打字，点一下直接问
const QUICK_QUESTIONS = [
  '阿司匹林能和降压药一起吃吗',
  '忘记吃药了怎么办',
  '缓释片能掰开吃吗',
  '药过期了还能吃吗',
];

// 轻量转义，防止 AI 回答注入 HTML
const escapeHtml = (s) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

function RagAskCard() {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState(null); // {answer, mode, sources, userDrugs}
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [speaking, setSpeaking] = useState(false);
  const answerRef = useRef(null);
  const streamAbortRef = useRef(null);

  useEffect(() => {
    return () => {
      if ('speechSynthesis' in window) window.speechSynthesis.cancel();
      if (streamAbortRef.current) streamAbortRef.current.abort();
    };
  }, []);

  /**
   * 流式问答：先收 meta（来源/药箱）→ 打字机式接收 delta
   * 失败（无流支持/网络异常）时抛错，由 ask() 回退非流式
   */
  const askStream = async (text) => {
    const token = getToken();
    const controller = new AbortController();
    streamAbortRef.current = controller;

    const res = await fetch('/api/rag/ask/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ question: text }),
      signal: controller.signal,
    });
    if (!res.ok || !res.body) {
      throw new Error('stream not available');
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let content = '';
    let finished = false;

    const handleChunk = (chunk) => {
      const dataLine = chunk.split('\n').find((l) => l.startsWith('data:'));
      const eventLine = chunk.split('\n').find((l) => l.startsWith('event:'));
      if (eventLine && eventLine.slice(6).trim() === 'done') {
        finished = true;
        return;
      }
      if (!dataLine) return;
      let payload;
      try {
        payload = JSON.parse(dataLine.slice(5).trim());
      } catch {
        return;
      }
      if (payload.type === 'meta') {
        setAnswer({
          answer: '',
          mode: payload.mode,
          sources: payload.sources || [],
          userDrugs: payload.userDrugs || [],
        });
      } else if (payload.type === 'delta') {
        content += payload.content || '';
        setAnswer((prev) => ({ ...prev, answer: content }));
      }
    };

    while (!finished) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let idx;
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const chunk = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 2);
        handleChunk(chunk);
        if (finished) break;
      }
    }
    // 处理尾部残余
    if (buffer.trim()) handleChunk(buffer);
  };

  const ask = async (q) => {
    const text = (q ?? question).trim();
    if (!text || loading) return;
    setLoading(true);
    setError('');
    setAnswer(null);
    try {
      await askStream(text);
    } catch (e) {
      // 流式不可用 → 回退非流式（elderFetch 自动带 token + 401 刷新）
      console.warn('流式问答不可用，回退普通问答:', e.message);
      try {
        const data = await elderFetch('/api/rag/ask', {
          method: 'POST',
          body: JSON.stringify({ question: text }),
        });
        if (data.code === 200) {
          setAnswer(data.data);
        } else {
          setError(data.message || '提问失败');
        }
      } catch (e2) {
        setError(e2.message || '提问失败，请稍后重试');
      }
    } finally {
      setLoading(false);
      streamAbortRef.current = null;
      if (answerRef.current) {
        answerRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    }
  };

  // 语音播报（去掉 markdown 符号后朗读）
  const toggleSpeak = useCallback(() => {
    if (!('speechSynthesis' in window) || !answer?.answer) return;
    const synth = window.speechSynthesis;
    if (speaking) {
      synth.cancel();
      setSpeaking(false);
      return;
    }
    const text = answer.answer
      .replace(/\*\*/g, '')
      .replace(/\[\d+\]/g, '')
      .replace(/[#*`>]/g, '');
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'zh-CN';
    utterance.rate = 0.95;
    utterance.onend = () => setSpeaking(false);
    utterance.onerror = () => setSpeaking(false);
    setSpeaking(true);
    synth.speak(utterance);
  }, [answer, speaking]);

  // 解析回答里的 [1][2] 引用编号 → 对应下方参考资料（引用感知展示）
  const citedRefs = useMemo(() => {
    if (!answer?.answer) return new Set();
    const set = new Set();
    const re = /\[(\d+)\]/g;
    let m;
    while ((m = re.exec(answer.answer))) set.add(parseInt(m[1], 10));
    return set;
  }, [answer]);
  const showCiteBadge = citedRefs.size > 0;

  // 点击回答里的 [1] 引用 → 滚动到对应资料并闪烁高亮
  const handleCiteClick = (e) => {
    const el = e.target.closest('.rag-cite');
    if (!el) return;
    const n = parseInt(el.dataset.ref, 10);
    const target = document.querySelector(`.rag-source-item[data-ref="${n}"]`);
    if (!target) return;
    target.scrollIntoView({ behavior: 'smooth', block: 'center' });
    target.classList.add('rag-source-flash');
    setTimeout(() => target.classList.remove('rag-source-flash'), 1600);
  };

  // 渲染回答：识别编号条目 / 加粗 / 引用上标 / 分段
  const renderAnswer = (text) => {
    const withCite = (html) =>
      html.replace(/\[(\d+)\]/g, '<sup class="rag-cite" data-ref="$1" title="点击查看参考资料$1">[$1]</sup>');
    return text.split('\n').map((line, i) => {
      const t = line.trim();
      if (!t) return <div key={i} className="rag-para-gap" />;
      const html = withCite(escapeHtml(t).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>'));
      const numMatch = t.match(/^(\d+)[.、．]\s*/);
      if (numMatch) {
        return (
          <div key={i} className="rag-answer-item">
            <span className="rag-item-num">{numMatch[1]}</span>
            <span className="rag-item-text" dangerouslySetInnerHTML={{ __html: html.replace(/^\d+[.、．]\s*/, '') }} />
          </div>
        );
      }
      if (t.startsWith('- ')) {
        return (
          <div key={i} className="rag-answer-item">
            <span className="rag-item-num">•</span>
            <span className="rag-item-text" dangerouslySetInnerHTML={{ __html: html.replace(/^- /, '') }} />
          </div>
        );
      }
      return <p key={i} className="rag-answer-line" dangerouslySetInnerHTML={{ __html: html }} />;
    });
  };

  return (
    <div className="daily-lesson-card rag-ask-card">
      <div className="daily-lesson-header">
        <span className="daily-lesson-badge">💬</span>
        <h3 className="daily-lesson-title">用药问问</h3>
        <span className="rag-subtitle">吃药前，问一问</span>
      </div>

      <div className="rag-ask-input-row">
        <input
          className="rag-ask-input"
          placeholder="比如：阿司匹林能和降压药一起吃吗？"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && ask()}
        />
        <button className="btn btn-primary rag-ask-btn" onClick={() => ask()} disabled={loading || !question.trim()}>
          {loading ? '…' : '提问'}
        </button>
      </div>

      {/* 快捷问题：老人少打字 */}
      <div className="rag-quick-list">
        {QUICK_QUESTIONS.map((q, i) => (
          <button key={i} className="rag-quick-btn" onClick={() => { setQuestion(q); ask(q); }} disabled={loading}>
            {q}
          </button>
        ))}
      </div>

      {error && <div className="daily-lesson-error">{error}</div>}

      {/* 正在回答：动态反馈，老人知道没失败 */}
      {loading && !answer && (
        <div className="rag-typing">
          <span className="rag-typing-dot" /> 正在回答，请稍候…
        </div>
      )}

      {answer && (
        <div className="rag-answer" ref={answerRef}>
          <div className="rag-answer-toolbar">
            <span className="rag-user-drugs">
              {answer.userDrugs && answer.userDrugs.length > 0
                ? `💊 根据您的药箱：${answer.userDrugs.join('、')}`
                : '💊 药箱暂未关联药品'}
            </span>
            <button className="rag-speak-btn" onClick={toggleSpeak}>
              {speaking ? '🔊 停止朗读' : '🔊 听一听'}
            </button>
          </div>
          <div className="rag-answer-body" onClick={handleCiteClick}>{renderAnswer(answer.answer)}</div>
          {answer.sources && answer.sources.length > 0 && (
            <div className="rag-sources">
              <div className="rag-sources-title">📚 回答参考了这些资料</div>
              {answer.sources.map((s, i) => {
                const cited = citedRefs.has(i + 1);
                return (
                  <div
                    className={`rag-source-item${cited ? ' rag-source-cited' : ' rag-source-unused'}`}
                    data-ref={i + 1}
                    key={i}
                  >
                    <span className="rag-source-idx">{i + 1}</span>
                    <span className="rag-source-title">{s.title}</span>
                    {s.sourceRef && <span className="rag-source-ref">{s.sourceRef}</span>}
                    {showCiteBadge && (
                      <span className={`rag-source-badge${cited ? '' : ' rag-source-badge-unused'}`}>
                        {cited ? '✓ 已引用' : '未采用'}
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default RagAskCard;
