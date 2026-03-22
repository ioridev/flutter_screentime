# flutter_screentime example

This example app exercises the plugin APIs directly:

- request authorization
- configure the block screen
- open the iOS blocked-app picker
- start and stop blocking

## iOS notes

The example app target now includes the `Family Controls` entitlement so the authorization flow is less confusing on a real device.

If you want the full Screen Time setup, open `example/ios/Runner.xcworkspace` in Xcode and add extension targets using:

- `example/ios/ScreenTimeExtensions/ShieldConfigurationExtension/ShieldConfigurationExtension.swift.sample`
- `example/ios/ScreenTimeExtensions/DeviceActivityMonitorExtension/DeviceActivityMonitorExtension.swift.sample`
