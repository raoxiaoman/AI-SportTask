#!/usr/bin/env bash
# 生成训练音频资源，一次产出三端副本：
#   Android → androidApp/src/androidMain/res/raw（mp3）
#   iOS     → iosApp/iosApp/Audio（mp3，Xcode 以文件夹引用方式整体拷进 App bundle）
#   Desktop → desktopApp/src/main/resources/audio（wav，JVM Clip 只原生支持 PCM）
#
# 音效来源：sfxmint.com，CC0 1.0（可商用、无需署名）
#   - 木质节拍器滴答 https://sfxmint.com/dl/metronome-ticking-01.mp3
#   - 柔和铃音       https://sfxmint.com/dl/ui-chime-46.mp3
# 中文播报：macOS 内置 Tingting 语音**离线预生成**，只在本脚本构建时运行；
#   App 运行时直接播放 mp3，不调用系统 TTS，避免引擎初始化卡顿与机型差异。
#
# 依赖：macOS say、ffmpeg（含 libmp3lame）、curl
# 用法：./tools/generate_training_audio.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/androidApp/src/androidMain/res/raw"
IOS_OUT="$ROOT/iosApp/iosApp/Audio"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT" "$IOS_OUT"

echo "下载 CC0 音效…"
curl -fLsS --max-time 30 -o "$TMP/metronome.mp3" \
  https://sfxmint.com/dl/metronome-ticking-01.mp3
curl -fLsS --max-time 30 -o "$TMP/chime.mp3" \
  https://sfxmint.com/dl/ui-chime-46.mp3

# 从 4.5 秒连续滴答中裁出第 1 个 tick（约 55ms 木质敲击），提亮后加短淡出。
echo "生成 tick.mp3 / chime.mp3…"
ffmpeg -y -loglevel error -i "$TMP/metronome.mp3" \
  -af "atrim=start=0:end=0.09,volume=12dB,alimiter=limit=0.8,afade=t=out:st=0.055:d=0.035,apad=pad_dur=0.02" \
  -ac 1 -ar 44100 -c:a libmp3lame -q:a 5 "$OUT/tick.mp3"
ffmpeg -y -loglevel error -i "$TMP/chime.mp3" \
  -af "volume=2dB,alimiter=limit=0.9,atrim=duration=1.0" \
  -ac 1 -ar 44100 -c:a libmp3lame -q:a 5 "$OUT/chime.mp3"

# 离线生成中文播报。语速偏快、文案精简，保证单段 < 1.2s，
# 不会被下一秒的秒针声盖住；前后静音裁掉，尾部留 50ms 呼吸。
voice() {
  local name="$1" rate="$2" text="$3"
  say -v Tingting -r "$rate" -o "$TMP/$name.aiff" "$text"
  ffmpeg -y -loglevel error -i "$TMP/$name.aiff" \
    -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-45dB:stop_periods=1:stop_duration=0.20:stop_threshold=-45dB:stop_silence=0.08,volume=1.5,alimiter=limit=0.95,apad=pad_dur=0.05" \
    -ac 1 -ar 44100 -c:a libmp3lame -q:a 6 "$OUT/$name.mp3"
}

echo "生成中文倒数与阶段播报…"
voice v5         220 "五"
voice v4         220 "四"
voice v3         220 "三"
voice v2         220 "二"
voice v1         220 "一"
voice v_start    235 "开始训练"
voice v_resume   235 "继续训练"
voice v_rest     235 "休息一下"
voice v_rest_end 235 "休息结束"
voice v_next     235 "下一个动作"
voice v_half     235 "时间过半"
voice v_complete 220 "训练完成"
voice v_skip     235 "跳过"

# 两端必须完全一致：iOS 侧直接复用 Android 的产物，避免两处资源各自漂移。
echo "同步到 iOS App bundle…"
cp "$OUT"/*.mp3 "$IOS_OUT"/

# 桌面端与手机端用同一批离线语音，只是容器换成 JVM Clip 能直接读的 PCM wav。
# App 运行时三端都只播放打包进来的音频，不调用系统 TTS。
echo "同步到桌面 App resources（mp3 → wav）…"
DESKTOP_OUT="$ROOT/desktopApp/src/main/resources/audio"
mkdir -p "$DESKTOP_OUT"
for src in "$OUT"/*.mp3; do
  name="$(basename "$src" .mp3)"
  ffmpeg -y -loglevel error -i "$src" \
    -ac 1 -ar 22050 -c:a pcm_s16le "$DESKTOP_OUT/$name.wav"
done

echo "产物："
ls -lh "$OUT"
echo "→ $IOS_OUT"
ls -lh "$IOS_OUT" | tail -n +2 | wc -l | xargs echo "iOS 片段数："
echo "→ $DESKTOP_OUT"
ls -lh "$DESKTOP_OUT"/*.wav | tail -n +2 | wc -l | xargs echo "桌面片段数："
