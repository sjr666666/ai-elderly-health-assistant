/**
 * 百度 ASR 语音识别工具（老人语音输入）
 *
 * 链路：getUserMedia 采集 → AudioContext 下采样到 16kHz 单声道 → 编码 WAV
 *      → POST /api/ai/asr → 百度短语音识别 → 返回文字
 *
 * 为什么不用 MediaRecorder：它默认产出 webm/opus，百度短语音识别只支持
 * wav/pcm 原始 PCM，所以用 Web Audio API 自行采集 PCM 并封装 WAV。
 * 浏览器不支持 / 无麦克风权限 / 识别失败时，由调用方降级到 Web Speech API。
 */

const TARGET_RATE = 16000; // 百度短语音识别最佳采样率
const MAX_RECORD_SECONDS = 15; // 自动停止，防老人一直按着

let cachedBaiduEnabled = null;

/**
 * 查询后端百度 ASR 是否已配置（结果缓存，避免每次录音都请求）
 * @returns {Promise<boolean>}
 */
export async function isBaiduAsrEnabled() {
  if (cachedBaiduEnabled !== null) return cachedBaiduEnabled;
  try {
    const res = await fetch('/api/ai/asr/config', {
      headers: { Accept: 'application/json' },
    });
    const data = await res.json();
    cachedBaiduEnabled = data.code === 200 && !!data.data?.baiduAsrEnabled;
  } catch (e) {
    cachedBaiduEnabled = false;
  }
  return cachedBaiduEnabled;
}

/**
 * 创建百度 ASR 录音控制器（start/stop 两段式，避免阻塞 UI）
 *
 * @param {string} token JWT（老人端 elderToken）
 * @returns {{start: () => Promise<void>, stop: () => Promise<string>, cancel: () => void}}
 */
export function createBaiduAsrRecorder(token) {
  let stream = null;
  let audioCtx = null;
  let source = null;
  let processor = null;
  let chunks = [];
  let active = false;

  /** 开始录音（异步拉起麦克风） */
  async function start() {
    if (active) return;
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('当前浏览器不支持录音');
    }
    chunks = [];
    stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    audioCtx = new AudioCtx();
    source = audioCtx.createMediaStreamSource(stream);
    processor = audioCtx.createScriptProcessor(4096, 1, 1);
    processor.onaudioprocess = (e) => {
      chunks.push(new Float32Array(e.inputBuffer.getChannelData(0)));
    };
    processor.connect(audioCtx.destination); // ScriptProcessor 需连接才回调
    source.connect(processor);
    active = true;
  }

  /** 停止录音 → 降采样编码 WAV → 上传百度识别 → 返回文字（失败抛错） */
  async function stop() {
    if (!active) throw new Error('未在录音');
    cleanup();
    if (chunks.length === 0) throw new Error('没有录到声音');

    const totalLen = chunks.reduce((s, c) => s + c.length, 0);
    const raw = new Float32Array(totalLen);
    let offset = 0;
    chunks.forEach((c) => { raw.set(c, offset); offset += c.length; });

    const sampleRate = audioCtx ? audioCtx.sampleRate || 48000 : 48000;
    const downsampled = downsample(raw, sampleRate, TARGET_RATE);
    const wav = encodeWav(downsampled, TARGET_RATE);

    const form = new FormData();
    form.append('file', wav, 'speech.wav');
    const res = await fetch('/api/ai/asr', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    });
    const data = await res.json();
    if (data.code !== 200 || !data.data?.text) {
      throw new Error(data.message || '语音识别失败');
    }
    return data.data.text;
  }

  /** 放弃录音（不识别） */
  function cancel() {
    cleanup();
  }

  function cleanup() {
    active = false;
    try { if (source) source.disconnect(); } catch (e) { /* noop */ }
    try { if (processor) processor.disconnect(); } catch (e) { /* noop */ }
    try { if (audioCtx && audioCtx.state !== 'closed') audioCtx.close(); } catch (e) { /* noop */ }
    if (stream) stream.getTracks().forEach((t) => t.stop());
    stream = null; audioCtx = null; source = null; processor = null;
  }

  return { start, stop, cancel };
}

/**
 * 线性平均降采样：fromRate → toRate
 */
function downsample(input, fromRate, toRate) {
  if (!input.length || fromRate <= 0) return input;
  if (fromRate === toRate) return input;
  const ratio = fromRate / toRate;
  const outLen = Math.floor(input.length / ratio);
  const out = new Float32Array(outLen);
  for (let i = 0; i < outLen; i++) {
    const start = Math.floor(i * ratio);
    const end = Math.min(Math.floor((i + 1) * ratio), input.length);
    let sum = 0;
    for (let j = start; j < end; j++) sum += input[j];
    out[i] = sum / (end - start);
  }
  return out;
}

/**
 * Float32 PCM → 16bit 单声道 WAV Blob
 */
function encodeWav(samples, sampleRate) {
  const buffer = new ArrayBuffer(44 + samples.length * 2);
  const view = new DataView(buffer);
  const writeStr = (o, s) => { for (let i = 0; i < s.length; i++) view.setUint8(o + i, s.charCodeAt(i)); };

  writeStr(0, 'RIFF');
  view.setUint32(4, 36 + samples.length * 2, true);
  writeStr(8, 'WAVE');
  writeStr(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);          // PCM
  view.setUint16(22, 1, true);          // 单声道
  view.setUint32(24, sampleRate, true); // 采样率
  view.setUint32(28, sampleRate * 2, true); // 字节率
  view.setUint16(32, 2, true);          // 块对齐
  view.setUint16(34, 16, true);         // 位深
  writeStr(36, 'data');
  view.setUint32(40, samples.length * 2, true);

  let o = 44;
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(o, s < 0 ? s * 0x8000 : s * 0x7fff, true);
    o += 2;
  }
  return new Blob([buffer], { type: 'audio/wav' });
}

export { MAX_RECORD_SECONDS };
