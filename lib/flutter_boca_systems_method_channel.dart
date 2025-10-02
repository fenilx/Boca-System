import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_boca_systems_platform_interface.dart';

/// An implementation of [FlutterBocaSystemsPlatform] that uses method channels.
class MethodChannelFlutterBocaSystems extends FlutterBocaSystemsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_boca_systems');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
