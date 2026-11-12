package data.plan

/**
 * iOS 端暂不接入本地通知文案，保留空实现以便后续接入 UNUserNotificationCenter
 */
actual fun publishReminderPreview(
    text: String,
    startDate: String,
    totalDays: Int,
    generatedDate: String
) {
}
