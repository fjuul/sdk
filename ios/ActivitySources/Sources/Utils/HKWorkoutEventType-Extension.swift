import Foundation
import HealthKit

/// Simple mapping of available workout event types to a human readable name before send on back-end
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
