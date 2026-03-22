# flutter_screentime

[English README](README.md)

Screen Time 風のブロック機能を提供する Flutter plugin です。

このパッケージは責務を次のように分けています。

- plugin 側は Flutter API、Android のフォアグラウンドオーバーレイ service、iOS の native bridge を持ちます。
- iOS ホストアプリ側は Screen Time extension target、entitlement、signing、最終的な shield 画面表示を持ちます。

## デモ

### iOS

![iOS demo](https://raw.githubusercontent.com/ioridev/flutter_screentime/main/doc/ios-demo.gif)

### Android

![Android demo](https://raw.githubusercontent.com/ioridev/flutter_screentime/main/doc/android-demo.gif)

## plugin が提供するもの

- iOS で認可を要求し、Android では必要な設定画面を開く
- iOS で `FamilyActivityPicker` を表示してブロック対象アプリを選択する
- iOS で `ManagedSettingsStore` を使って選択済みのアプリやカテゴリに shield を適用する
- Android で設定注入可能なブロックオーバーレイを表示する
- iOS extension がカスタム画面を描画できるように、ブロック画面設定を共有ストレージへ保存する

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

## iOS extension の扱い

カスタムのブロック画面は引き続きホストアプリ側の責務です。この plugin は設定保存までを担当し、ホストアプリ側では以下を追加する必要があります。

- `Shield Configuration Extension`
- 必要に応じて `Device Activity Monitor Extension`
- 共有 `App Group`
- `Family Controls` capability

セットアップ詳細: [doc/ios_extensions.md](doc/ios_extensions.md)

テンプレート:

- [ShieldConfigurationExtension.swift.sample](templates/ios/ShieldConfigurationExtension/ShieldConfigurationExtension.swift.sample)
- [DeviceActivityMonitorExtension.swift.sample](templates/ios/DeviceActivityMonitorExtension/DeviceActivityMonitorExtension.swift.sample)

## Android について

Android には Screen Time 相当の公式 API がないため、この plugin は以下を使います。

- `SYSTEM_ALERT_WINDOW`
- `PACKAGE_USAGE_STATS`
- フォアグラウンド service によるオーバーレイ

ブロック対象 package を明示的に設定しない場合、Android 実装はホストアプリ自身を除く起動可能な非システムアプリをすべてブロック対象として扱います。
