import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screentime/flutter_screentime.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _screenTime = const FlutterScreentime();
  ScreenTimeAuthorizationStatus _status =
      ScreenTimeAuthorizationStatus.notDetermined;
  String _message = 'Idle';

  @override
  void initState() {
    super.initState();
    _refreshAuthorization();
  }

  Future<void> _refreshAuthorization() async {
    try {
      final status = await _screenTime.checkAuthorization();
      if (!mounted) {
        return;
      }
      setState(() {
        _status = status;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _message = _errorMessage(error);
      });
    }
  }

  Future<void> _requestAuthorization() async {
    try {
      final status = await _screenTime.requestAuthorization();
      if (!mounted) {
        return;
      }
      setState(() {
        _status = status;
        _message = 'Authorization updated';
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _message = _errorMessage(error);
      });
    }
  }

  Future<void> _selectBlockedApps() async {
    try {
      final summary = await _screenTime.selectBlockedApps();
      if (!mounted) {
        return;
      }
      setState(() {
        _message =
            'Selected ${summary.applicationCount} apps and ${summary.categoryCount} categories';
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _message = _errorMessage(error);
      });
    }
  }

  Future<void> _configureBlockScreen() async {
    await _screenTime.configureBlockScreen(
      const ScreenTimeBlockScreenConfig(
        title: 'Focus mode',
        message: 'This app is blocked right now.',
        backgroundColorHex: '#0F172A',
        textColorHex: '#F8FAFC',
        primaryButtonLabel: 'Open app',
        secondaryButtonLabel: 'Settings',
      ),
    );
    if (!mounted) {
      return;
    }
    setState(() {
      _message = 'Block screen updated';
    });
  }

  Future<void> _startBlocking() async {
    try {
      if (Platform.isAndroid) {
        await _screenTime.setBlockedPackages(const <String>[]);
      }
      await _screenTime.startBlocking();
      if (!mounted) {
        return;
      }
      setState(() {
        _message = 'Blocking started';
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _message = _errorMessage(error);
      });
    }
  }

  Future<void> _stopBlocking() async {
    await _screenTime.stopBlocking();
    if (!mounted) {
      return;
    }
    setState(() {
      _message = 'Blocking stopped';
    });
  }

  String _errorMessage(Object error) {
    if (error is PlatformException) {
      return error.message ?? error.code;
    }
    return error.toString();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('flutter_screentime example')),
        body: ListView(
          padding: const EdgeInsets.all(24),
          children: <Widget>[
            Text('Authorization: ${_status.platformValue}'),
            const SizedBox(height: 8),
            Text(_message),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _requestAuthorization,
              child: const Text('Request authorization'),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _configureBlockScreen,
              child: const Text('Configure block screen'),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _selectBlockedApps,
              child: const Text('Select blocked apps (iOS)'),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _startBlocking,
              child: const Text('Start blocking'),
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: _stopBlocking,
              child: const Text('Stop blocking'),
            ),
          ],
        ),
      ),
    );
  }
}
