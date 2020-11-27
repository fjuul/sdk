import Foundation
import HealthKit

extension HKWorkoutEventType {
    var typeName: String {
        switch self {
        case .pause:                return "Pause"
        case .resume:               return "Resume"
        case .lap:                  return "Lap"
        case .marker:               return "Marker"
        case .motionPaused:         return "MotionPaused"
        case .motionResumed:        return "MotionResumed"
        case .segment:              return "Segment"
        case .pauseOrResumeRequest: return "PauseOrResumeRequest"

        default:                    return "Other"
        }
    }
}
