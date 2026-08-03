import { useState, useRef, useCallback, useEffect } from 'react';
import { createPortal } from 'react-dom';

const FloatingMicButton = ({ onTranscript }) => {
  const [isListening, setIsListening] = useState(false);
  const [isSupported] = useState(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    return !!SpeechRecognition;
  });
  const recognitionRef = useRef(null);

  const startListening = useCallback(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    const recognition = new SpeechRecognition();
    recognition.lang = 'zh-CN';
    recognition.interimResults = true;
    recognition.continuous = false;

    recognition.onstart = () => {
      setIsListening(true);
    };

    recognition.onresult = (event) => {
      let transcript = '';
      for (let i = 0; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
      }
      if (event.results[0].isFinal && onTranscript) {
        onTranscript(transcript);
      }
    };

    recognition.onerror = () => {
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognitionRef.current = recognition;
    recognition.start();
  }, [onTranscript]);

  const stopListening = useCallback(() => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
      recognitionRef.current = null;
    }
    setIsListening(false);
  }, []);

  const toggleListening = useCallback(() => {
    if (isListening) {
      stopListening();
    } else {
      startListening();
    }
  }, [isListening, startListening, stopListening]);

  useEffect(() => {
    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }
    };
  }, []);

  if (!isSupported) return null;

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
        {isListening ? '正在聆听' : '语音输入'}
      </span>
    </div>,
    document.body
  );
};

export default FloatingMicButton;
