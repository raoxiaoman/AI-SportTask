package pages

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openExternalUrl(url: String) {
    runCatching {
        NSURL.URLWithString(url)?.let { nsUrl ->
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}
