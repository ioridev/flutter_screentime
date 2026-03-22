# flutter_screentime

[日本語版 README](README.ja.md)

Flutter plugin for Screen Time style blocking.

The package now splits concerns this way:

- The plugin owns the Flutter API, Android foreground overlay service, and iOS native bridge.
- The host iOS app owns Screen Time extension targets, entitlements, signing, and final shield-screen presentation.

## Demo

- [iOS demo video (.mov)](https://user-images.githubusercontent.com/41247249/235582095-66d23f3e-91d1-4a42-88d0-4c3cae886c39.mov)
- [Android demo video](https://github.com/ioridev/flutter_screentime/assets/41247249/144c7f65-6ea0-4ab7-82fb-4c5444f18cf3)

## What the plugin does

- Requests authorization on iOS and opens the required settings screens on Android.
- Presents `FamilyActivityPicker` on iOS to capture blocked app selections.
- Applies `ManagedSettingsStore` shielding on iOS for the selected apps and categories.
- Runs a configurable Android overlay for blocked apps.
- Persists block-screen configuration into shared storage so host iOS extensions can render custom screens.

## Dart API

```dart
const screenTime = FlutterScreentime();

await screenTime.setSharedContainerId('group.your.app');
await screenTime.configureBlockScreen(
  const ScreenTimeBlockScreenConfig(
    title: 'Focus mode',
    message: 'This app is blocked right now.',
    backgroundColorHex: '#0F172A',
    textColorHex: '#F8FAFC',
    primaryButtonLabel: 'Open app',
    secondaryButtonLabel: 'Settings',
  ),
);

await screenTime.requestAuthorization();
await screenTime.selectBlockedApps();
await screenTime.startBlocking();
```

## iOS extension model

Custom blocked screens are still a host-app concern. This plugin stores the configuration, but the host app must add:

- `Shield Configuration Extension`
- `Device Activity Monitor Extension`, if needed
- a shared `App Group`
- the `Family Controls` capability

Setup details: [doc/ios_extensions.md](doc/ios_extensions.md)

Templates:

- [ShieldConfigurationExtension.swift.sample](templates/ios/ShieldConfigurationExtension/ShieldConfigurationExtension.swift.sample)
- [DeviceActivityMonitorExtension.swift.sample](templates/ios/DeviceActivityMonitorExtension/DeviceActivityMonitorExtension.swift.sample)

## Android notes

Android does not have an equivalent Screen Time API, so the plugin uses:

- `SYSTEM_ALERT_WINDOW`
- `PACKAGE_USAGE_STATS`
- a foreground service overlay

If you do not set blocked packages explicitly, the Android implementation blocks all launchable non-system apps except the host app itself.
