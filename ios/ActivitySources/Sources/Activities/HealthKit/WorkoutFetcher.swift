import Foundation
import HealthKit
import Logging

private let logger = Logger(label: "FjuulSDK")

class WorkoutFetcher {
    static func fetch(anchor: HKQueryAnchor, predicate: NSCompoundPredicate, completion: @escaping (_ data: [WorkoutDataPoint], _ newAnchor: HKQueryAnchor?) -> Void) {
        let cycleDispatchGroup = DispatchGroup()
        var workouts: [WorkoutDataPoint] = []
        let query = HKAnchoredObjectQuery(type: HKObjectType.workoutType(), predicate: predicate, anchor: anchor, limit: HKObjectQueryNoLimit) { (_, samples, _, newAnchor, error) in
            if error != nil {
                completion([], nil)
                return
            }

            guard let samples = samples as? [HKWorkout] else {
                completion(workouts, nil)
                return
            }

            for sampleItem in samples {
                cycleDispatchGroup.enter()
                let events = sampleItem.workoutEvents?.compactMap { event in
                    return WorkoutEventData(
                        startDate: event.date,
                        type: event.type.typeName,
                        metadata: event.metadata?.compactMapValues { String(describing: $0 )}
                    )
                }

                var workout = WorkoutDataPoint(
                    uuid: sampleItem.uuid.uuidString,
                    sourceBundleIdentifier: sampleItem.sourceRevision.source.bundleIdentifier,
                    startDate: sampleItem.startDate,
                    endDate: sampleItem.endDate,
                    totalDistance: sampleItem.totalDistance?.doubleValue(for: .meter()) ?? 0,
                    totalEnergyBurned: sampleItem.totalEnergyBurned?.doubleValue(for: .kilocalorie()) ?? 0,
                    workoutActivityType: sampleItem.workoutActivityType.name,
                    totalSwimmingStrokeCount: sampleItem.totalSwimmingStrokeCount?.doubleValue(for: .count()) ?? 0,
                    workoutEvents: events,
                    metadata: sampleItem.metadata?.compactMapValues { String(describing: $0 )}
                )

                let workoutDispatchGroup = DispatchGroup()
                if let distanceWalkingRunningType = HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning) {
                    workoutDispatchGroup.enter()
                    self.fetchWorkoutSmaples(sampleType: distanceWalkingRunningType, workout: sampleItem) { result in
                        switch result {
                        case .success(let samples):
                            workout.walkingRunningDistances = self.convertWorkoutQuantitySamples(samples: samples, unit: .meter())
                        case .failure(let err):
                            logger.error("WorkoutFetcher error: \(err)")
                        }

                        workoutDispatchGroup.leave()
                    }
                }

                if let heartRateType = HKObjectType.quantityType(forIdentifier: .heartRate) {
                    let hrUnit = HKUnit.count().unitDivided(by: HKUnit.minute())
                    workoutDispatchGroup.enter()
                    self.fetchWorkoutSmaples(sampleType: heartRateType, workout: sampleItem) { result in
                        switch result {
                        case .success(let samples):
                            workout.heartRates = self.convertWorkoutQuantitySamples(samples: samples, unit: hrUnit)
                        case .failure(let err):
                            logger.error("WorkoutFetcher error: \(err)")
                        }

                        workoutDispatchGroup.leave()
                    }
                }

                if let activeEnergyBurnedType = HKObjectType.quantityType(forIdentifier: .activeEnergyBurned) {
                    workoutDispatchGroup.enter()
                    self.fetchWorkoutSmaples(sampleType: activeEnergyBurnedType, workout: sampleItem) { result in
                        switch result {
                        case .success(let samples):
                            workout.activeEnergyBurned = self.convertWorkoutQuantitySamples(samples: samples, unit: .kilocalorie())
                        case .failure(let err):
                            logger.error("WorkoutFetcher error: \(err)")
                        }

                        workoutDispatchGroup.leave()
                    }
                }

                workoutDispatchGroup.notify(queue: .global(qos: .userInitiated)) {
                    workouts.append(workout)
                    cycleDispatchGroup.leave()
                }
            }

            cycleDispatchGroup.notify(queue: .global(qos: .userInitiated)) {
                completion(workouts, newAnchor)
            }
        }
        HealthKitManager.healthStore.execute(query)
    }

    private static func fetchWorkoutSmaples(sampleType: HKQuantityType, workout: HKWorkout, completion: @escaping (Result<[HKQuantitySample], Error>) -> Void) {
        let workoutPredicate = HKQuery.predicateForObjects(from: workout)
        let startDateSort = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)
        let query = HKSampleQuery(sampleType: sampleType,
                                  predicate: workoutPredicate,
                                  limit: HKObjectQueryNoLimit,
                                  sortDescriptors: [startDateSort]) { (_, results, error) -> Void in
                                    if let error = error {
                                        completion(.failure(error))
                                        return
                                    }

                                    guard let samples = results as? [HKQuantitySample] else {
                                        completion(.success([]))
                                        return
                                    }

                                    completion(.success(samples))
        }
        HealthKitManager.healthStore.execute(query)
    }

    private static func convertWorkoutQuantitySamples(samples: [HKQuantitySample], unit: HKUnit) -> [WorkoutSampleData] {
        var workoutSamples: [WorkoutSampleData] = []
        for sample in samples {
            let workoutSample = WorkoutSampleData(value: sample.quantity.doubleValue(for: unit), startDate: sample.startDate, endDate: sample.endDate)
            workoutSamples.append(workoutSample)
        }
        return workoutSamples
    }
}
