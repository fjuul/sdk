import Foundation

/// Usefull functions to work with Date in Swift
public class DateUtils {
    // Inspired from https://github.com/SwifterSwift/SwifterSwift/blob/master/Sources/SwifterSwift/Foundation/DateExtensions.swift
    static func beginningOfHour(date: Date?) -> Date? {
        guard let date = date else { return nil }

        let calendar = Calendar.current
        let component: Set<Calendar.Component> = [.year, .month, .day, .hour]

        return calendar.date(from: calendar.dateComponents(component, from: date))
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

    /// Prepare Set with hours starts from beginning of hour.
    /// - Parameters:
    ///   - startDate: start Date
    ///   - endDate: end Date
    /// - Returns: Set of dates
    static func dirtyHours(startDate: Date, endDate: Date) -> Set<Date> {
        let calendar = Calendar.current
        var currentDate = startDate
        var dates = [currentDate]
        var batchStartDates: Set<Date> = []

        while currentDate < endDate {
            // iterate by 1 hour
            if let newDate = calendar.date(byAdding: .hour, value: 1, to: currentDate) {
                currentDate = newDate

                if newDate < endDate {
                    dates.append(currentDate)
                }
            } else {
                break
            }
        }
        dates.append(endDate)

        // Coverts dates to Set with date starts from beginning of hour
        dates.forEach { batchDate in
            if let date = DateUtils.beginningOfHour(date: batchDate) {
                batchStartDates.insert(date)
            }
        }

        return batchStartDates
    }
}
