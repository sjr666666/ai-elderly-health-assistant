import { useRef, useState, useEffect } from 'react';

/**
 * useTTS - 语音播报 Hook（百度TTS + 浏览器原生语音备用）
 * 从 App.js 拆分而来，封装全部语音播放逻辑：
 * - 全局语音：speak / stopSpeaking（用于用药指导、提醒播报）
 * - 追问语音：speakFollowUp / stopFollowUpSpeaking（独立于全局，用于AI追问对话）
 *
 * authFetch 通过 setAuthFetch 动态注入（避免与组件内函数定义顺序耦合）
 *
 * @param {object} deps
 * @param {Function} deps.showToast - Toast 提示函数
 */
export function useTTS({ showToast }) {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [speechRate, setSpeechRate] = useState(1);
  const [isFollowUpSpeaking, setIsFollowUpSpeaking] = useState(false);
  const [speakingFollowUpIdx, setSpeakingFollowUpIdx] = useState(null);

  const audioRef = useRef(null);
  const followUpAudioRef = useRef(null);
  const followUpMessagesRef = useRef(null);
  const speakRef = useRef(null);
  const authFetchRef = useRef(null);

  // 动态注入 authFetch（由组件在定义后调用 setAuthFetch 完成）
  const setAuthFetch = (fn) => { authFetchRef.current = fn; };

  // 浏览器原生语音（备用方案）
  const speakWithBrowser = (text, rate) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'zh-CN';
      utterance.rate = rate;
      utterance.volume = 1;

      // 尝试选择最好的中文语音
      const voices = window.speechSynthesis.getVoices();
      const chineseVoice = voices.find(v => v.lang.includes('zh') && v.name.includes('Female'));
      if (chineseVoice) {
        utterance.voice = chineseVoice;
      }

      utterance.onend = () => setIsSpeaking(false);
      utterance.onerror = () => {
        console.error('浏览器语音播放失败');
        setIsSpeaking(false);
      };

      window.speechSynthesis.speak(utterance);
    } else {
      showToast('您的浏览器不支持语音播报功能', 'error');
      setIsSpeaking(false);
    }
  };

  // 全局语音播报（优先百度TTS，失败降级浏览器原生）
  const speak = async (text, rate = speechRate) => {
    // 剥离HTML标签，避免TTS朗读标签内容
    const cleanText = (text || '').replace(/<[^>]*>/g, '');
    if (!cleanText || cleanText.trim() === '') {
      return;
    }

    // 先停止当前播放，避免中断错误
    stopSpeaking();

    // 优先尝试调用百度TTS API
    try {
      setIsSpeaking(true);

      // 将前端语速(0.6-1)映射到百度TTS语速(3-5)
      const baiduRate = rate === 0.6 ? 3 : 5;
      const { response, data: result } = await authFetchRef.current(`/api/ai/tts?text=${encodeURIComponent(cleanText)}&speechRate=${baiduRate}`);

      if (response.ok) {
        if (result.code === 200 && result.data) {
          // 播放百度返回的音频
          if (audioRef.current) {
            audioRef.current.src = result.data;
            audioRef.current.play().catch(err => {
              console.error('音频播放失败:', err);
              setIsSpeaking(false);
            });
            return; // 成功则返回
          }
        }
      }

      // 如果百度TTS失败，使用浏览器原生语音（备用方案）
      console.warn('百度TTS不可用，使用浏览器原生语音');
      speakWithBrowser(cleanText, rate);

    } catch (error) {
      console.error('百度TTS调用失败，使用备用方案:', error);
      speakWithBrowser(cleanText, rate);
    }
  };

  // 停止所有播放（全局 + 追问）
  const stopSpeaking = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    if (followUpAudioRef.current) {
      followUpAudioRef.current.pause();
      followUpAudioRef.current.currentTime = 0;
    }
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsSpeaking(false);
    setIsFollowUpSpeaking(false);
    setSpeakingFollowUpIdx(null);
  };

  // 追问消息专用语音播放（独立于全局isSpeaking，避免与用药说明播放按钮联动）
  const speakFollowUp = async (text, rate = speechRate) => {
    const cleanText = (text || '').replace(/<[^>]*>/g, '');
    if (!cleanText || cleanText.trim() === '') { return; }
    // 先停止所有播放（包括全局播放），避免音频重叠
    if (audioRef.current) { audioRef.current.pause(); audioRef.current.currentTime = 0; }
    if (followUpAudioRef.current) { followUpAudioRef.current.pause(); followUpAudioRef.current.currentTime = 0; }
    if ('speechSynthesis' in window) { window.speechSynthesis.cancel(); }
    setIsSpeaking(false);
    try {
      setIsFollowUpSpeaking(true);
      const baiduRate = rate === 0.6 ? 3 : 5;
      const { response, data: result } = await authFetchRef.current(`/api/ai/tts?text=${encodeURIComponent(cleanText)}&speechRate=${baiduRate}`);
      if (response.ok) {
        if (result.code === 200 && result.data) {
          if (followUpAudioRef.current) {
            followUpAudioRef.current.src = result.data;
            followUpAudioRef.current.play().catch(err => {
              setIsFollowUpSpeaking(false); setSpeakingFollowUpIdx(null);
            });
            return;
          }
        }
      }
      speakFollowUpWithBrowser(cleanText, rate);
    } catch (error) {
      speakFollowUpWithBrowser(cleanText, rate);
    }
  };

  // 追问消息专用浏览器语音（备用方案）
  const speakFollowUpWithBrowser = (text, rate) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'zh-CN';
      utterance.rate = rate;
      utterance.volume = 1;
      const voices = window.speechSynthesis.getVoices();
      const chineseVoice = voices.find(v => v.lang.includes('zh') && v.name.includes('Female'));
      if (chineseVoice) { utterance.voice = chineseVoice; }
      utterance.onend = () => { setIsFollowUpSpeaking(false); setSpeakingFollowUpIdx(null); };
      utterance.onerror = () => { setIsFollowUpSpeaking(false); setSpeakingFollowUpIdx(null); };
      window.speechSynthesis.speak(utterance);
    } else {
      showToast('您的浏览器不支持语音播报功能', 'error');
      setIsFollowUpSpeaking(false);
      setSpeakingFollowUpIdx(null);
    }
  };

  // 仅停止追问消息的播放（不影响全局isSpeaking）
  const stopFollowUpSpeaking = () => {
    if (followUpAudioRef.current) {
      followUpAudioRef.current.pause();
      followUpAudioRef.current.currentTime = 0;
    }
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsFollowUpSpeaking(false);
    setSpeakingFollowUpIdx(null);
  };

  // 切换追问消息的语音播放
  const toggleFollowUpSpeech = (idx, text) => {
    if (speakingFollowUpIdx === idx && isFollowUpSpeaking) {
      stopFollowUpSpeaking();
    } else {
      setSpeakingFollowUpIdx(idx);
      speakFollowUp(text);
    }
  };

  // 监听全局音频播放结束
  useEffect(() => {
    const audio = audioRef.current;
    if (audio) {
      const handleEnded = () => setIsSpeaking(false);
      audio.addEventListener('ended', handleEnded);
      return () => audio.removeEventListener('ended', handleEnded);
    }
  }, []);

  // 监听追问音频播放结束（独立重置追问播放状态）
  useEffect(() => {
    const audio = followUpAudioRef.current;
    if (audio) {
      const handleEnded = () => {
        setIsFollowUpSpeaking(false);
        setSpeakingFollowUpIdx(null);
      };
      audio.addEventListener('ended', handleEnded);
      return () => audio.removeEventListener('ended', handleEnded);
    }
  }, []);

  // 暴露 speak 引用（供定时提醒/WebSocket 场景调用）
  speakRef.current = speak;

  return {
    isSpeaking,
    speechRate,
    setSpeechRate,
    isFollowUpSpeaking,
    speakingFollowUpIdx,
    audioRef,
    followUpAudioRef,
    followUpMessagesRef,
    speakRef,
    setAuthFetch,
    speak,
    stopSpeaking,
    speakFollowUp,
    stopFollowUpSpeaking,
    toggleFollowUpSpeech,
  };
}
