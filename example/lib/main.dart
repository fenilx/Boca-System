import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_boca_systems/flutter_boca_systems.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Boca Systems Printer',
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Boca Systems Printer'),
        ),
        body: const MyHomePage(),
      ),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _status = 'Not connected';
  String _printerStatus = '';
  String _deviceAddress = '';
  String _ipAddress = '192.168.1.100'; // Default IP address
  bool _connected = false;

  @override
  void initState() {
    super.initState();
    // Initialize the plugin with a status callback
    FlutterBocaSystems.initialize(
      statusCallback: (status) {
        setState(() {
          _printerStatus += '\n$status';
        });
      },
    );
  }

  Future<void> _connectBluetooth() async {
    if (_deviceAddress.isEmpty) {
      _showError('Please enter a device address');
      return;
    }

    try {
      setState(() {
        _status = 'Connecting to Bluetooth...';
      });

      final success = await FlutterBocaSystems.openSessionBT(_deviceAddress);
      
      setState(() {
        _connected = success;
        _status = success ? 'Connected via Bluetooth' : 'Failed to connect';
      });
    } on PlatformException {
      setState(() {
        _status = 'Error: Platform exception occurred';
      });
    }
  }

  Future<void> _connectUSB() async {
    try {
      setState(() {
        _status = 'Connecting to USB...';
      });

      final success = await FlutterBocaSystems.openSessionUSB();
      
      setState(() {
        _connected = success;
        _status = success ? 'Connected via USB' : 'Failed to connect';
      });
    } on PlatformException {
      setState(() {
        _status = 'Error: Platform exception occurred';
      });
    }
  }

  Future<void> _connectWiFi() async {
    try {
      setState(() {
        _status = 'Connecting to WiFi...';
      });

      final success = await FlutterBocaSystems.openSessionWIFI(_ipAddress);
      
      setState(() {
        _connected = success;
        _status = success ? 'Connected via WiFi' : 'Failed to connect';
      });
    } on PlatformException {
      setState(() {
        _status = 'Error: Platform exception occurred';
      });
    }
  }

  Future<void> _disconnect() async {
    if (_connected) {
      // Try to close all possible connections
      try {
        await FlutterBocaSystems.closeSessionBT();
      } on PlatformException {
        // Ignore errors
      }
      
      try {
        await FlutterBocaSystems.closeSessionUSB();
      } on PlatformException {
        // Ignore errors
      }
      
      try {
        await FlutterBocaSystems.closeSessionWIFI();
      } on PlatformException {
        // Ignore errors
      }
      
      setState(() {
        _connected = false;
        _status = 'Disconnected';
      });
    }
  }

  Future<void> _sendTestCommand() async {
    if (_connected) {
      try {
        await FlutterBocaSystems.sendString('<RC100,100><F11>This is a test<p>');
        _showInfo('Test command sent');
      } on PlatformException {
        _showError('Error sending command: Platform exception occurred');
      }
    } else {
      _showError('Not connected to printer');
    }
  }

  Future<void> _printTicket() async {
    if (_connected) {
      try {
        await FlutterBocaSystems.sendString('<F12><RC50,50>Boca Systems Test Ticket<p>');
        await FlutterBocaSystems.printCut();
        _showInfo('Ticket printed');
      } on PlatformException {
        _showError('Error printing ticket: Platform exception occurred');
      }
    } else {
      _showError('Not connected to printer');
    }
  }

  Future<void> _clearMemory() async {
    if (_connected) {
      try {
        await FlutterBocaSystems.clearMemory();
        _showInfo('Printer memory cleared');
      } on PlatformException {
        _showError('Error clearing memory: Platform exception occurred');
      }
    } else {
      _showError('Not connected to printer');
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  void _showInfo(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Connection status
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Connection Status',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(_status),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          decoration: const InputDecoration(
                            labelText: 'Bluetooth Address',
                            hintText: 'XX:XX:XX:XX:XX:XX',
                          ),
                          onChanged: (value) => _deviceAddress = value,
                        ),
                      ),
                      const SizedBox(width: 8),
                      ElevatedButton(
                        onPressed: _connectBluetooth,
                        child: const Text('BT Connect'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          decoration: const InputDecoration(
                            labelText: 'WiFi IP Address',
                            hintText: '192.168.1.100',
                          ),
                          onChanged: (value) => _ipAddress = value,
                        ),
                      ),
                      const SizedBox(width: 8),
                      ElevatedButton(
                        onPressed: _connectWiFi,
                        child: const Text('WiFi Connect'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: ElevatedButton(
                          onPressed: _connectUSB,
                          child: const Text('USB Connect'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: _disconnect,
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
                          child: const Text('Disconnect'),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          
          const SizedBox(height: 16),
          
          // Printer actions
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Printer Actions',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      ElevatedButton(
                        onPressed: _connected ? _sendTestCommand : null,
                        child: const Text('Send Test'),
                      ),
                      ElevatedButton(
                        onPressed: _connected ? _printTicket : null,
                        child: const Text('Print Ticket'),
                      ),
                      ElevatedButton(
                        onPressed: _connected ? _clearMemory : null,
                        child: const Text('Clear Memory'),
                      ),
                      ElevatedButton(
                        onPressed: _connected ? FlutterBocaSystems.printCut : null,
                        child: const Text('Cut'),
                      ),
                      ElevatedButton(
                        onPressed: _connected ? FlutterBocaSystems.printNoCut : null,
                        child: const Text('No Cut'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          
          const SizedBox(height: 16),
          
          // Printer status
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Printer Status',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    height: 200,
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    padding: const EdgeInsets.all(8),
                    child: SingleChildScrollView(
                      child: Text(_printerStatus.isEmpty ? 'No status updates yet' : _printerStatus),
                    ),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton(
                    onPressed: () {
                      setState(() {
                        _printerStatus = '';
                      });
                    },
                    child: const Text('Clear Status'),
                  ),
                ],
              ),
            ),
          ),
          
          const SizedBox(height: 16),
          
          // Configuration
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Configuration',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton(
                    onPressed: _connected
                        ? () async {
                            await FlutterBocaSystems.changeConfiguration(
                              resolution: 300,
                              dithered: true,
                              path: '<P1>',
                              orientation: '<LM>',
                            );
                            _showInfo('Configuration updated');
                          }
                        : null,
                    child: const Text('Set Config'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
