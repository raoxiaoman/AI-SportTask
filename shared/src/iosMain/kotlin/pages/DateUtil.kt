package pages

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents

actual fun todayDateString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(NSDate())
}

actual fun daysAgoDateString(days: Int): String {
    val calendar = NSCalendar.currentCalendar
    val components = NSDateComponents()
    components.setValue(-days, forComponent = NSCalendarUnitDay)
    val date = calendar.dateByAddingComponents(components, toDate = NSDate(), options = 0u)
        ?: NSDate()
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(date)
}
