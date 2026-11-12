package pages

// iOS 端尚未接入 AVAudioPlayer（本工程目前未在 Xcode 编译验证）。
// 音频资源已按文件名统一命名，接入时按 TrainingAudioEvent 展开并用
// NSBundle.mainBundle 读取同名 mp3 即可，commonMain 无需改动。
actual fun prepareTrainingAudio() {}

actual fun playTrainingAudio(event: TrainingAudioEvent, voiceAllowed: Boolean) {}

actual fun stopTrainingAudio() {}

actual fun releaseTrainingAudio() {}
