import { useState, useRef, useCallback, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { getToken } from '../utils/elderApi';
import { createBaiduAsrRecorder, isBaiduAsrEnabled, MAX_RECORD_SECONDS } from '../utils/asr';

/**
 * 浮动麦克风按钮 - 语音输入
 *
 * 识别链路（双通道）：
 *   1. 百度 ASR（后端 /api/ai/asr）：Web Audio 采集 PCM → WAV 16k → 上传识别，跨浏览器一致
 *   2. 浏览器 Web Speech API（降级）：百度未配 Key / 识别失败 / 浏览器无录音能力时自动使用
 * 识别结果统一通过 onTranscript(text) 回传（语音指令解析 / 语音问药共用）
 */
const FloatingMicButton = ({ onTranscript }) => {
  const [isListening, setIsListening] = useState(false);
  const [useBaidu, setUseBaidu] = useState(false);
  const [tip, setTip] = useState('');
  const recorderRef = useRef(null);
  const recognitionRef = useRef(null);
  const autoStopTimerRef = useRef(null);
  const onTranscriptRef = useRef(onTranscript);
  onTranscriptRef.current = onTranscript;

  // 探测识别通道：百度优先，Web Speech 兜底
  const [webSpeechSupported] = useState(() => {
    if (typeof window === 'undefined') return false;
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition);
  });

  useEffect(() => {
    isBaiduAsrEnabled().then((enabled) => setUseBaidu(enabled));
  }, []);

  const cleanup = useCallback(() => {
    if (autoStopTimerRef.current) {
      clearTimeout(autoStopTimerRef.current);
      autoStopTimerRef.current = null;
    }
    if (recorderRef.current) {
      try { recorderRef.current.cancel(); } catch (e) { /* noop */ }
      recorderRef.current = null;
    }
    if (recognitionRef.current) {
      try { recognitionRef.current.abort(); } catch (e) { /* noop */ }
      recognitionRef.current = null;
    }
  }, []);

  // 百度 ASR：录音 → 上传识别
  const startBaidu = useCallback(async () => {
    const recorder = createBaiduAsrRecorder(getToken());
    recorderRef.current = recorder;
    await recorder.start();
    setIsListening(true);
    setTip('百度识别中…');
    // 自动停止：防止老人一直按着不松手
    autoStopTimerRef.current = setTimeout(async () => {
      try {
        const text = await recorder.stop();
        setIsListening(false);
        if (text) onTranscriptRef.current(text);
      } catch (e) {
        setIsListening(false);
        setTip(e.message || '识别失败');
      }
      recorderRef.current = null;
    }, MAX_RECORD_SECONDS * 1000);
  }, []);

  const stopBaidu = useCallback(async () => {
    const recorder = recorderRef.current;
    if (!recorder) return;
    recorderRef.current = null;
    if (autoStopTimerRef.current) {
      clearTimeout(autoStopTimerRef.current);
      autoStopTimerRef.current = null;
    }
    try {
      const text = await recorder.stop();
      if (text) onTranscriptRef.current(text);
      setTip('');
    } catch (e) {
      setTip(e.message || '识别失败');
    }
    setIsListening(false);
  }, []);

  // Web Speech API：浏览器原生语音识别（降级）
  const startWebSpeech = useCallback(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.lang = 'zh-CN';
    recognition.interimResults = true;
    recognition.continuous = false;
    recognition.onstart = () => { setIsListening(true); setTip('浏览器识别中…'); };
    recognition.onresult = (event) => {
      let transcript = '';
      for (let i = 0; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
      }
      if (event.results[0].isFinal) {
        if (transcript) onTranscriptRef.current(transcript);
      }
    };
    recognition.onerror = () => { setIsListening(false); setTip(''); };
    recognition.onend = () => { setIsListening(false); setTip(''); };
    recognitionRef.current = recognition;
    recognition.start();
  }, []);

  const stopWebSpeech = useCallback(() => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
      recognitionRef.current = null;
    }
    setIsListening(false);
    setTip('');
  }, []);

  const toggleListening = useCallback(async () => {
    if (isListening) {
      if (useBaidu && recorderRef.current) {
        await stopBaidu();
      } else {
        stopWebSpeech();
      }
      return;
    }
    setTip('');
    try {
      if (useBaidu) {
        await startBaidu();
      } else if (webSpeechSupported) {
        startWebSpeech();
      } else {
        setTip('当前环境不支持语音输入');
      }
    } catch (e) {
      // 百度识别失败/无麦克风 → 降级 Web Speech
      console.warn('百度 ASR 启动失败，降级 Web Speech:', e.message);
      if (webSpeechSupported) {
        startWebSpeech();
      } else {
        setTip(e.message || '无法使用语音输入');
      }
    }
  }, [isListening, useBaidu, webSpeechSupported, startBaidu, stopBaidu, startWebSpeech, stopWebSpeech]);

  useEffect(() => {
    return () => cleanup();
  }, [cleanup]);

  // 完全无语音能力 → 隐藏按钮
  if (!webSpeechSupported && !useBaidu) return null;

  return createPortal(
    <div className={`floating-mic-wrapper ${isListening ? 'listening' : ''}`}>
      <div className="floating-mic-core">
        <span className="floating-mic-ripple" />
        <span className="floating-mic-ripple floating-mic-ripple--second" />
        <button
          onClick={toggleListening}
          className="floating-mic-btn"
          title={isListening ? '点击停止语音输入' : '点击开始语音输入'}
          aria-label={isListening ? '停止语音输入' : '开始语音输入'}
        >
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#fff"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <line x1="12" y1="19" x2="12" y2="22" />
          </svg>
        </button>
      </div>
      <span className="floating-mic-label">
        {isListening ? '正在聆听' : tip || '语音输入'}
      </span>
    </div>,
    document.body
  );
};

export default FloatingMicButton;
