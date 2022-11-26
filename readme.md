# Bluetooth Low Energy Client with BLESSED-ANDROID library (part 4)

This is the part 4 BLE Client example using the library in https://github.com/weliem/blessed-android.

**The code is changed to find and connect to nearby BLE devices.**

Recent parts of the client and the server apps:

**Client part 1 (Setup a Android Bluetooth Low Energy client part 1)**: https://github.com/AndroidCrypto/BleClientBlessedOriginal

**Client part 2 (Enhance a Android Bluetooth Low Energy client part 2)**: https://github.com/AndroidCrypto/BleClientBlessedPart2

**Client part 3 (Add a Battery Service listener to  a Android Bluetooth Low Energy client)**: https://github.com/AndroidCrypto/BleClientBlessedPart3

**Server part 1 (Setup your own Android Bluetooth Low Energy Server part 1)**: https://github.com/AndroidCrypto/BleServerBlessedOriginal

**Server part 2 (Enhance your own Android Bluetooth Low Energy Server part 2)**: https://github.com/AndroidCrypto/BleServerBlessedPart2

**Server part 3 (Add a Battery Service to your own Android Bluetooth Low Energy Server)**: https://github.com/AndroidCrypto/BleServerBlessedPart3

The library in use (BLESSED-ANDROID) is available here: https://github.com/weliem/blessed-android 
provided by **Martijn van Welie**.

For a general overview on Bluetooth Low Energy (BLE) see this perfect article "The Ultimate Guide to Android Bluetooth Low Energy", 
available as PDF in the docs folder as well: https://punchthrough.com/android-ble-guide/.

Screenshot of the running app after connection to the server.

![client_view_after_connect](docs/client00.png?raw=true)

To check that the server is up and running I recommend to additionally install another app on the second device that 
allows to connect to the "Server", I'm using **nRF Connect for Mobile** and it is available on the 
Google's PlayStore:  https://play.google.com/store/apps/details?id=no.nordicsemi.android.mcp&hl=de&gl=US. I 
provide a simple manual on how to work with the nRF Connect-app here: 
[nRFConnect_manual](nrfconnect_manual.md) or see my article on Medium: 
https://medium.com/@androidcrypto/connect-the-android-nrf-connect-mobile-app-with-a-bluetooth-low-energy-device-8ba900d70286

To get the Client app to build you need 2 additional dependencies, add them in build.gradle(app):
```plaintext
    implementation 'com.jakewharton.timber:timber:5.0.1'
    implementation 'com.github.weliem:blessed-android:2.3.4'
```

Additionally a new line is necessary in the settings.gradle file (project settings):
```plaintext
add the line maven { url 'https://jitpack.io' }:

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

You may notice that the AndroidManifest.xml does not contain any Bluetooth related permissions - they are all 
set within the library but you are asked to grant some runtime permissions during startup (depending on the Android  
SDK version running on your Smartphone the server is running on).

For reconnecting:
```plaintext
https://github.com/weliem/blessed-android/issues/61
In the meantime, I've been using this horrible hack in order to reconnect to the same peripheral:
public void startScanningPeripherals()
{
    if (connectedPeripheral != null)
    {
        connectedPeripheral.clearServicesCache();
        try
        {
            Thread.sleep(250);
        }
        catch (InterruptedException e)
        {}
        connectedPeripheral = null;
        central = new BluetoothCentral(context, bluetoothCentralCallback, new Handler(Looper.getMainLooper()));
    }
    central.scanForPeripherals();
}
```

Notice regarding a bug in the blessed-library's log system: in BluetoothServer.java you will notice an 
"error" on line 185 ("Cannot resolve method 'plant(timber.log.Timber.DebugTree)'"):
```plaintext
Timber.plant(new Timber.DebugTree());
```
This error is filed as a Timber issue and may get corrected in a newer version. The good news is - you 
can build your app regardless of this error and you still see the logged messages in your LogCat.

**Important notice when working with an emulated device running on a Smartphone (e.g. the BleServerBlessedOriginal):  
For security reasons the address the server can get connected is changing very often so when using a client app 
like the nRF Connect-app it is often necessary to (re)run a scan AND choose the newest entry (mostly the most 
bottom down one).**

This example app is providing just a minimal UI but it is worth to see the LogCat output where you can see 
e.g. that the example Server app is providing the (server's) time every second. In an enhanced version this 
data will get shown on the UI...

sample LogFile:
```plaintext
...
BluetoothLeScanner       D  Start Scan with callback
BluetoothLeScanner       D  onScannerRegistered() - status=0 scannerId=9 mScannerId=0
BluetoothCentralManager  I  scan started
BluetoothHandler         I  Found peripheral 'SM-A515F'
BluetoothLeScanner       D  Stop Scan with callback
BluetoothPeripheral      I  connect to 'SM-A515F' (68:D0:C8:D5:4A:AA) using transport LE
BluetoothGatt            D  connect() - device: 68:D0:C8:D5:4A:AA, auto: false
BluetoothGatt            D  registerApp() - UUID=d2d69178-2364-4f70-a731-2cc400eb3d07
BluetoothPeripheral      I  peripheral '68:D0:C8:D5:4A:AA' is connecting
BluetoothPeripheral      D  discovering services of 'SM-A515F' with delay of 0 ms
BluetoothPeripheral      I  discovered 5 services for 'SM-A515F'
BluetoothPeripheral      D  reading characteristic <00002a29-0000-1000-8000-00805f9b34fb>
BluetoothHandler         I  Received manufacturer: samsung
BluetoothPeripheral      D  reading characteristic <00002a24-0000-1000-8000-00805f9b34fb>
BluetoothHandler         I  Received modelnumber: SM-A515F
BluetoothGatt            D  setCharacteristicNotification() - uuid: 00002a2b-0000-1000-8000-00805f9b34fb enable: true
BluetoothHandler         I  SUCCESS: Notify set to 'true' for 00002a2b-0000-1000-8000-00805f9b34fb
BluetoothPeripheral      D  writing <e6070a170d2623073a01> to characteristic <00002a2b-0000-1000-8000-00805f9b34fb>
BluetoothHandler         I  Received device time: Sun Oct 23 13:38:37 GMT+02:00 2022
BluetoothHandler         I  SUCCESS: Writing <e6070a170d2623073a01> to <00002a2b-0000-1000-8000-00805f9b34fb>
BluetoothGatt            D  setCharacteristicNotification() - uuid: 00002a37-0000-1000-8000-00805f9b34fb enable: true
BluetoothHandler         I  SUCCESS: Notify set to 'true' for 00002a37-0000-1000-8000-00805f9b34fb
BluetoothHandler         D  79
BluetoothHandler         I  Received device time: Sun Oct 23 13:38:35 GMT+02:00 2022
BluetoothHandler         D  77
...
```

Some technical details on this app:
```plaintext
minimum SDK is 21
compiled/target SDK is 33
Gradle version is 7.4
```

The library blessed-android is MIT-licensed.
