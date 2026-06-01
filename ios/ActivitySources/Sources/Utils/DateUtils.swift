import Foundation

/// Usefull functions to work with Date in Swift
public class DateUtils {

    static func startOfDay(date: Date) -> Date {
        return Calendar.current.startOfDay(for: date)
    }

    static func endOfDay(date: Date) -> Date? {
        return Calendar.current.date(bySettingHour: 23, minute: 59, second: 59, of: date)
    }

    // Inspired from https://github.com/SwifterSwift/SwifterSwift/blob/master/Sources/SwifterSwift/Foundation/DateExtensions.swift
    static func endOfHour(date: Date?) -> Date? {
        guard let date = date else { return nil }

        let calendar = Calendar.current
        let nextHour = calendar.date(byAdding: .hour, value: 1, to: date)

        let after = calendar.date(from: calendar.dateComponents([.year, .month, .day, .hour], from: nextHour!))!
        return calendar.date(byAdding: .second, value: -1, to: after)
    }

    static func endOfDay(date: Date?) -> Date? {
        guard let date = date else { return nil }

        let calendar = Calendar.current

        var newDate = calendar.date(byAdding: .day, value: 1, to: date)!
        newDate = calendar.startOfDay(for: newDate)

        return Calendar.current.date(byAdding: .second, value: -1, to: newDate)
    }

    /// Prepare a set of UTC hour start dates (beginning of each UTC hour).
    /// - Parameters:
    ///   - startDate: start Date
    ///   - endDate: end Date
    /// - Returns: Set of UTC hour start dates
    static func dirtyUTCHours(startDate: Date, endDate: Date) -> Set<Date> {
        var utcCalendar = Calendar(identifier: .gregorian)
        utcCalendar.timeZone = TimeZone(secondsFromGMT: 0)!

        var currentDate = startDate
        var dates = [currentDate]
        var batchStartDates: Set<Date> = []

        while currentDate < endDate {
            if let newDate = utcCalendar.date(byAdding: .hour, value: 1, to: currentDate) {
                currentDate = newDate

                if newDate < endDate {
                    dates.append(currentDate)
                }
            } else {
                break
            }
        }
        dates.append(endDate)

        dates.forEach { batchDate in
            let components = utcCalendar.dateComponents([.year, .month, .day, .hour], from: batchDate)
            if let date = utcCalendar.date(from: components) {
                batchStartDates.insert(date)
            }
        }

        return batchStartDates
    }
}
