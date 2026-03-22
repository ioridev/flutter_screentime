import 'package:flutter_screentime/flutter_screentime.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('ScreenTimeBlockScreenConfig serializes consistently', () {
    const config = ScreenTimeBlockScreenConfig(
      title: 'Focus',
      message: 'Come back later.',
      backgroundColorHex: '#000000',
      textColorHex: '#FFFFFF',
      primaryButtonLabel: 'Open app',
      secondaryButtonLabel: 'Settings',
    );

    final decoded = ScreenTimeBlockScreenConfig.fromMap(config.toMap());

    expect(decoded.title, config.title);
    expect(decoded.message, config.message);
    expect(decoded.backgroundColorHex, config.backgroundColorHex);
    expect(decoded.textColorHex, config.textColorHex);
    expect(decoded.primaryButtonLabel, config.primaryButtonLabel);
    expect(decoded.secondaryButtonLabel, config.secondaryButtonLabel);
  });

  test('authorization status falls back to notDetermined', () {
    expect(
      ScreenTimeAuthorizationStatus.fromPlatformValue('unknown'),
      ScreenTimeAuthorizationStatus.notDetermined,
    );
  });
}
