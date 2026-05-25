package pages

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate

actual fun todayDateString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(NSDate())
}
