package pages

/**
 * 用系统浏览器/外部应用打开链接（视频演示等外链入口）。
 * 平台实现：Android Intent VIEW / iOS UIApplication openURL。
 */
expect fun openExternalUrl(url: String)
