import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_boca_systems/flutter_boca_systems.dart';
import 'package:flutter_boca_systems/flutter_boca_systems_platform_interface.dart';
import 'package:flutter_boca_systems/flutter_boca_systems_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterBocaSystemsPlatform
    with MockPlatformInterfaceMixin
    implements FlutterBocaSystemsPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterBocaSystemsPlatform initialPlatform = FlutterBocaSystemsPlatform.instance;

  test('$MethodChannelFlutterBocaSystems is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterBocaSystems>());
  });

  test('getPlatformVersion', () async {
    MockFlutterBocaSystemsPlatform fakePlatform = MockFlutterBocaSystemsPlatform();
    FlutterBocaSystemsPlatform.instance = fakePlatform;

    expect(await FlutterBocaSystems.getPlatformVersion(), '42');
  });
}
