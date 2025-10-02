// You have generated a new plugin project without specifying the `--platforms`
// flag. A plugin project with no platform support was generated. To add a
// platform, run `flutter create -t plugin --platforms <platforms> .` under the
// same directory. You can also find a detailed instruction on how to add
// platforms in the `pubspec.yaml` at
// https://flutter.dev/to/pubspec-plugin-platforms.

import 'package:flutter/services.dart';

/// Callback function type for receiving status updates from the printer
typedef StatusCallback = void Function(String status);

/// A Flutter plugin for Boca Systems thermal printers
class FlutterBocaSystems {
  static const MethodChannel _channel = MethodChannel('flutter_boca_systems');

  /// Callback for receiving status updates from the printer
  static StatusCallback? _statusCallback;

  /// Initializes the plugin with a status callback
  static void initialize({StatusCallback? statusCallback}) {
    _statusCallback = statusCallback;
    
    // Set up method call handler for receiving status updates
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == 'onStatusUpdate' && _statusCallback != null) {
        final status = call.arguments['status'] as String?;
        if (status != null) {
          _statusCallback!(status);
        }
      }
    });
  }

  /// Get the platform version
  static Future<String?> getPlatformVersion() async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Open Bluetooth session with the printer
  static Future<bool> openSessionBT(String device) async {
    final bool result = await _channel.invokeMethod(
      'openSessionBT',
      {'device': device},
    );
    return result;
  }

  /// Open USB session with the printer
  static Future<bool> openSessionUSB() async {
    final bool result = await _channel.invokeMethod('openSessionUSB');
    return result;
  }

  /// Open WiFi session with the printer
  static Future<bool> openSessionWIFI(String ipAddress) async {
    final bool result = await _channel.invokeMethod(
      'openSessionWIFI',
      {'ipAddress': ipAddress},
    );
    return result;
  }

  /// Close Bluetooth session
  static Future<void> closeSessionBT() async {
    await _channel.invokeMethod('closeSessionBT');
  }

  /// Close USB session
  static Future<void> closeSessionUSB() async {
    await _channel.invokeMethod('closeSessionUSB');
  }

  /// Close WiFi session
  static Future<void> closeSessionWIFI() async {
    await _channel.invokeMethod('closeSessionWIFI');
  }

  /// Verify Bluetooth connection status
  static Future<bool> verifyConnectionBT() async {
    final bool result = await _channel.invokeMethod('verifyConnectionBT');
    return result;
  }

  /// Verify USB connection status
  static Future<bool> verifyConnectionUSB() async {
    final bool result = await _channel.invokeMethod('verifyConnectionUSB');
    return result;
  }

  /// Verify WiFi connection status
  static Future<bool> verifyConnectionWIFI() async {
    final bool result = await _channel.invokeMethod('verifyConnectionWIFI');
    return result;
  }

  /// Send string command to the printer
  static Future<void> sendString(String string) async {
    await _channel.invokeMethod('sendString', {'string': string});
  }

  /// Send file to the printer (supports TXT, PDF, PNG, JPG, BMP)
  static Future<bool> sendFile(String filename, {int row = 0, int column = 0}) async {
    final bool result = await _channel.invokeMethod('sendFile', {
      'filename': filename,
      'row': row,
      'column': column,
    });
    return result;
  }

  /// Download a file as a logo to the printer memory
  static Future<bool> downloadLogo(String filename, int idnum) async {
    final bool result = await _channel.invokeMethod('downloadLogo', {
      'filename': filename,
      'idnum': idnum,
    });
    return result;
  }

  /// Print a previously downloaded logo
  static Future<bool> printLogo(int idnum, {int row = 0, int column = 0}) async {
    final bool result = await _channel.invokeMethod('printLogo', {
      'idnum': idnum,
      'row': row,
      'column': column,
    });
    return result;
  }

  /// Change printer configuration
  static Future<void> changeConfiguration({
    String path = '<P1>', // Printer path (P1-P4)
    int resolution = 300, // Printer resolution (200, 300, 600 DPI)
    bool scaled = false, // Scale image to ticket size
    bool dithered = true, // Dither image
    int stocksizeindex = 0, // Stock size index (0-8)
    String orientation = '<LM>', // Orientation (LM=landscape, PM=portrait)
  }) async {
    await _channel.invokeMethod('changeConfiguration', {
      'path': path,
      'resolution': resolution,
      'scaled': scaled,
      'dithered': dithered,
      'stocksizeindex': stocksizeindex,
      'orientation': orientation,
    });
  }

  /// Clear printer memory (remove downloaded logos)
  static Future<void> clearMemory() async {
    await _channel.invokeMethod('clearMemory');
  }

  /// Print with cut
  static Future<void> printCut() async {
    await _channel.invokeMethod('printCut');
  }

  /// Print without cut
  static Future<void> printNoCut() async {
    await _channel.invokeMethod('printNoCut');
  }

  /// Get printer status
  static Future<String> getStatus() async {
    final String status = await _channel.invokeMethod('getStatus') ?? '';
    return status;
  }
}
