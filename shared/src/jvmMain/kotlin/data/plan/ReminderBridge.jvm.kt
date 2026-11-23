package data.plan

/**
 * 桌面端暂不接入系统通知，保留空实现，与 iOS 一致。
 * 后续可接 java.awt.TrayIcon 系统托盘通知。
 */
actual fun publishReminderPreview(
    text: String,
    startDate: String,
    totalDays: Int,
    generatedDate: String
) {
}
