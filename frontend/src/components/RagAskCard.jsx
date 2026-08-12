import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import DOMPurify from 'dompurify';
import { elderFetch, getToken } from '../utils/elderApi';
import { createBaiduAsrRecorder, isBaiduAsrEnabled } from '../utils/asr';

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
  const [feedbackSent, setFeedbackSent] = useState(null); // null=未反馈, 1=👍, -1=👎
  const [feedbackState, setFeedbackState] = useState(''); // ''|'sending'|'ok'|'fail'
  const answerRef = useRef(null);
  const streamAbortRef = useRef(null);

  // ===== 语音问药（百度 ASR 优先，Web Speech 降级） =====
  const [voiceListening, setVoiceListening] = useState(false);
  const [voiceTip, setVoiceTip] = useState('');
  const voiceRecorderRef = useRef(null);
  const voiceRecognitionRef = useRef(null);

  useEffect(() => {
    isBaiduAsrEnabled().then((enabled) => {
      if (!enabled) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) setVoiceTip('当前环境不支持语音输入');
      }
    });
  }, []);

  const voiceCleanup = useCallback(() => {
    if (voiceRecorderRef.current) {
      try { voiceRecorderRef.current.cancel(); } catch (e) { /* noop */ }
      voiceRecorderRef.current = null;
    }
    if (voiceRecognitionRef.current) {
      try { voiceRecognitionRef.current.abort(); } catch (e) { /* noop */ }
      voiceRecognitionRef.current = null;
    }
  }, []);

  useEffect(() => () => voiceCleanup(), [voiceCleanup]);

  // 提交回答反馈（这个回答有用吗）
  const submitFeedback = async (rating) => {
    if (feedbackSent || !answer) return;
    setFeedbackSent(rating);
    setFeedbackState('sending');
    try {
      const data = await elderFetch('/api/rag/feedback', {
        method: 'POST',
        body: JSON.stringify({
          question: answer.question || question,
          answer: answer.answer,
          rating,
          mode: answer.mode,
        }),
      });
      setFeedbackState(data.code === 200 ? 'ok' : 'fail');
    } catch (e) {
      setFeedbackState('fail');
      console.warn('提交反馈失败:', e.message);
    }
  };

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
  const askStream = useCallback(async (text) => {
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
          question: text,
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
  }, []);

  const ask = useCallback(async (q) => {
    const text = (q ?? question).trim();
    if (!text || loading) return;
    setLoading(true);
    setError('');
    setAnswer(null);
    setFeedbackSent(null);
    setFeedbackState('');
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
  }, [question, loading, askStream]);

  // 语音问药：识别结果直接填入输入框并提问（百度 ASR 优先，Web Speech 降级）
  const toggleVoice = useCallback(async () => {
    if (voiceListening) {
      // 停止：百度录音直接识别，Web Speech 停止后 onresult 自动回传
      if (voiceRecorderRef.current) {
        const recorder = voiceRecorderRef.current;
        voiceRecorderRef.current = null;
        try {
          const text = await recorder.stop();
          if (text) {
            setQuestion(text);
            ask(text);
          }
        } catch (e) {
          setVoiceTip(e.message || '识别失败');
        }
      } else if (voiceRecognitionRef.current) {
        voiceRecognitionRef.current.stop();
        voiceRecognitionRef.current = null;
      }
      setVoiceListening(false);
      setVoiceTip('');
      return;
    }

    setVoiceTip('');
    const baiduEnabled = await isBaiduAsrEnabled();
    if (baiduEnabled) {
      try {
        const recorder = createBaiduAsrRecorder(getToken());
        voiceRecorderRef.current = recorder;
        await recorder.start();
        setVoiceListening(true);
        return;
      } catch (e) {
        console.warn('百度 ASR 启动失败，降级 Web Speech:', e.message);
      }
    }
    // 降级：Web Speech API
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setVoiceTip('当前环境不支持语音输入');
      return;
    }
    const recognition = new SpeechRecognition();
    recognition.lang = 'zh-CN';
    recognition.interimResults = true;
    recognition.continuous = false;
    recognition.onstart = () => setVoiceListening(true);
    recognition.onresult = (event) => {
      let transcript = '';
      for (let i = 0; i < event.results.length; i++) transcript += event.results[i][0].transcript;
      if (event.results[0].isFinal && transcript) {
        setQuestion(transcript);
        ask(transcript);
      }
    };
    recognition.onerror = () => { setVoiceListening(false); };
    recognition.onend = () => { setVoiceListening(false); };
    voiceRecognitionRef.current = recognition;
    recognition.start();
  }, [voiceListening, ask]);

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
  // 安全：先 escapeHtml 转义 AI 原始输出，再插入受控标签，最后 DOMPurify 清洗（纵深防御）
  const renderAnswer = (text) => {
    const withCite = (html) =>
      html.replace(/\[(\d+)\]/g, '<sup class="rag-cite" data-ref="$1" title="点击查看参考资料$1">[$1]</sup>');
    const sanitize = (raw) =>
      DOMPurify.sanitize(raw, { ADD_ATTR: ['data-ref', 'title'] });
    return text.split('\n').map((line, i) => {
      const t = line.trim();
      if (!t) return <div key={i} className="rag-para-gap" />;
      const html = sanitize(withCite(escapeHtml(t).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')));
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
        <button
          className={`rag-voice-btn${voiceListening ? ' rag-voice-btn-on' : ''}`}
          onClick={toggleVoice}
          title={voiceListening ? '点击结束语音' : '语音提问，免打字'}
          aria-label={voiceListening ? '结束语音提问' : '语音提问'}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <line x1="12" y1="19" x2="12" y2="22" />
          </svg>
        </button>
        <button className="btn btn-primary rag-ask-btn" onClick={() => ask()} disabled={loading || !question.trim()}>
          {loading ? '…' : '提问'}
        </button>
      </div>
      {voiceTip && <div className="rag-voice-tip">{voiceTip}</div>}

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
          {/* 回答质量反馈闭环：这个回答有用吗 */}
          <div className="rag-feedback">
            {!feedbackSent ? (
              <>
                <span className="rag-feedback-label">这个回答有用吗？</span>
                <button className="rag-feedback-btn" onClick={() => submitFeedback(1)} aria-label="有用">👍 有用</button>
                <button className="rag-feedback-btn rag-feedback-btn-no" onClick={() => submitFeedback(-1)} aria-label="没用">👎 没用</button>
              </>
            ) : (
              <span className="rag-feedback-done">
                {feedbackSent === 1 ? '👍 谢谢反馈，我们会继续保持！' : '👎 谢谢反馈，我们会改进回答质量'}
                {feedbackState === 'fail' && '（提交失败，请重试）'}
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default RagAskCard;
