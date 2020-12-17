import Foundation

public class HKDataUtils {
    // Inspired from https://github.com/SwifterSwift/SwifterSwift/blob/master/Sources/SwifterSwift/Foundation/DateExtensions.swift
    static func beginningOfHour(date: Date?) -> Date? {
        let calendar = Calendar.current
        let component: Set<Calendar.Component> = [.year, .month, .day, .hour]

        return calendar.date(from: calendar.dateComponents(component, from: date!))
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
        
        var newDate = Calendar.current.date(byAdding: .day, value: 1, to: date)!
            //adding(.day, value: 1)
        
        //  calendar.date(byAdding: component, value: value, to: self)!
        newDate = Calendar.current.startOfDay(for: newDate)
        
        return Calendar.current.date(byAdding: .second, value: -1, to: newDate)!
    }
}
