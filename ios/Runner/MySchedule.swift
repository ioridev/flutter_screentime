import Foundation
import DeviceActivity

// The Device Activity name is how I can reference the activity from within my extension
extension DeviceActivityName {
    // Set the name of the activity to "daily"
    static let daily = Self("daily")
}

// I want to remove the application shield restriction when the child accumulates enough usage for a set of guardian-selected encouraged apps
extension DeviceActivityEvent.Name {
    // Set the name of the event to "encouraged"
    static let encouraged = Self("encouraged")
}

// The Device Activity schedule represents the time bounds in which my extension will monitor for activity
let schedule = DeviceActivitySchedule(
    // I've set my schedule to start and end at midnight
    intervalStart: DateComponents(hour: 15, minute: 08),
    intervalEnd: DateComponents(hour: 16, minute: 08),
    // I've also set the schedule to repeat
    repeats: false,
    warningTime: nil
)

class MySchedule {
    static public func unsetSchedule() {
        let center = DeviceActivityCenter()
        print("center.avtivities:\(center.activities)")
        if center.activities.isEmpty {
            return
        }
        center.stopMonitoring(center.activities)
        print("center.avtivities:\(center.activities)")
    }
    
    static public func setSchedule() {
        let applications = MyModel.shared.selectionToEncourage
        if applications.applicationTokens.isEmpty {
            print("empty applicationTokens")
        }
        if applications.categoryTokens.isEmpty {
            print("empty categoryTokens")
        }
        
        let events: [DeviceActivityEvent.Name: DeviceActivityEvent] = [
            .encouraged: DeviceActivityEvent(
                applications: applications.applicationTokens,
                categories: applications.categoryTokens,
                threshold: DateComponents(minute: 1)
            )
        ]
        
        // Create a Device Activity center
        let center = DeviceActivityCenter()
        do {
            // Call startMonitoring with the activity name, schedule, and events
            print("center.avtivities:\(center.activities)")
            print("Try to start monitoring...")
            try center.startMonitoring(.daily, during: schedule, events: events)
            print("monitoring...")
        } catch {
            print("Error monitoring schedule: ", error)
        }
    }
}

// Another ingredient to shielding apps is figuring out what the guardian wants to discourage
// The Family Controls framework has a SwiftUI element for this: the family activity picker
