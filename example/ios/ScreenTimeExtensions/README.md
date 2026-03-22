# Screen Time extensions for the example app

This folder is intentionally not wired into the Xcode project as build targets.

To make the example fully runnable for Screen Time development on a real device:

1. Open `example/ios/Runner.xcworkspace` in Xcode.
2. Add a `Shield Configuration Extension` target.
3. Add a `Device Activity Monitor Extension` target if you need scheduled or threshold-based behavior.
4. Reuse the sample files in this folder for those targets.
5. Add an `App Group` shared by the app target and the extension targets.

The example app target already includes the `Family Controls` entitlement so the authorization and picker flow is easier to understand.
