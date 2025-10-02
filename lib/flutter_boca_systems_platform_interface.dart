import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_boca_systems_method_channel.dart';

abstract class FlutterBocaSystemsPlatform extends PlatformInterface {
  /// Constructs a FlutterBocaSystemsPlatform.
  FlutterBocaSystemsPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterBocaSystemsPlatform _instance = MethodChannelFlutterBocaSystems();

  /// The default instance of [FlutterBocaSystemsPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterBocaSystems].
  static FlutterBocaSystemsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterBocaSystemsPlatform] when
  /// they register themselves.
  static set instance(FlutterBocaSystemsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
