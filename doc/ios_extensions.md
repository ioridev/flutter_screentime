# iOS extension setup

`flutter_screentime` does not create Screen Time extensions for the host app. The host app owns those targets, capabilities, and signing settings.

## Required host setup

1. Add the `Family Controls` capability to the app target.
2. Add an `App Group` that is shared by the app target and every Screen Time extension target.
3. Call `setSharedContainerId('group.your.app')` before configuring the block screen.
4. Add the extension targets you need:
   - `Shield Configuration Extension` for custom blocked-screen content.
   - `Device Activity Monitor Extension` if you want scheduled or threshold-based monitoring.

If you see `Connection error from Optional("com.apple.FamilyControlsAgent"): Couldn’t communicate with a helper application.`, check the app target first. Apple Developer Forums reports this error being resolved by adding the `Family Controls` capability to the app target, which adds the entitlement required for authorization and picker access.

## Shared keys

The plugin writes these values into both `UserDefaults.standard` and the configured app-group defaults:

- `flutter_screentime.blockScreenConfig`
- `flutter_screentime.blockedPackages`
- `flutter_screentime.blockedSelection`
- `flutter_screentime.blockingEnabled`

## Templates

Sample extension implementations live here:

- `templates/ios/ShieldConfigurationExtension/ShieldConfigurationExtension.swift.sample`
- `templates/ios/DeviceActivityMonitorExtension/DeviceActivityMonitorExtension.swift.sample`
