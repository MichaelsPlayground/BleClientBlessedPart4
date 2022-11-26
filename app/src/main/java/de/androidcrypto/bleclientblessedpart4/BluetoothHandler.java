package de.androidcrypto.bleclientblessedpart4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.BondState;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;

import com.welie.blessed.ScanFailure;
import com.welie.blessed.WriteType;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import timber.log.Timber;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_SINT16;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT16;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT8;
import static com.welie.blessed.BluetoothBytesParser.bytes2String;

import static java.lang.Math.abs;

class BluetoothHandler {

    // Intent constants
    // new in part 2
    public static final String BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS = "androidcrypto.bluetoothhandler.peripheralmacaddress";
    public static final String BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS_EXTRA = "androidcrypto.bluetoothhandler.peripheralmacaddress.extra";
    public static final String BLUETOOTHHANDLER_CURRENT_TIME = "androidcrypto.bluetoothhandler.currenttime";
    public static final String BLUETOOTHHANDLER_CURRENT_TIME_EXTRA = "androidcrypto.bluetoothhandler.currenttime.extra";
    // new in part 3
    public static final String BLUETOOTHHANDLER_BATTERY_LEVEL = "androidcrypto.bluetoothhandler.batterylevel";
    public static final String BLUETOOTHHANDLER_BATTERY_LEVEL_EXTRA = "androidcrypto.bluetoothhandler.batterylevel.extra";

    public static final String MEASUREMENT_BLOODPRESSURE = "androidcrypto.measurement.bloodpressure";
    public static final String MEASUREMENT_BLOODPRESSURE_EXTRA = "androidcrypto.measurement.bloodpressure.extra";
    public static final String MEASUREMENT_TEMPERATURE = "androidcrypto.measurement.temperature";
    public static final String MEASUREMENT_TEMPERATURE_EXTRA = "androidcrypto.measurement.temperature.extra";
    public static final String MEASUREMENT_HEARTRATE = "androidcrypto.measurement.heartrate";
    public static final String MEASUREMENT_HEARTRATE_EXTRA = "androidcrypto.measurement.heartrate.extra";
    public static final String MEASUREMENT_GLUCOSE = "androidcrypto.measurement.glucose";
    public static final String MEASUREMENT_GLUCOSE_EXTRA = "androidcrypto.measurement.glucose.extra";
    public static final String MEASUREMENT_PULSE_OX = "androidcrypto.measurement.pulseox";
    public static final String MEASUREMENT_PULSE_OX_EXTRA_CONTINUOUS = "androidcrypto.measurement.pulseox.extra.continuous";
    public static final String MEASUREMENT_PULSE_OX_EXTRA_SPOT = "androidcrypto.measurement.pulseox.extra.spot";
    public static final String MEASUREMENT_WEIGHT = "androidcrypto.measurement.weight";
    public static final String MEASUREMENT_WEIGHT_EXTRA = "androidcrypto.measurement.weight.extra";
    public static final String MEASUREMENT_EXTRA_PERIPHERAL = "androidcrypto.measurement.peripheral";

    // UUIDs for the Blood Pressure service (BLP)
    private static final UUID BLOOD_PRESSURE_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Health Thermometer service (HTS)
    private static final UUID HEALTH_THERMOMETER_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    private static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
    private static final UUID PNP_ID_CHARACTERISTIC_UUID = UUID.fromString("00002A50-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Heart Rate service (HRS)
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Device Information service (DIS)
    private static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    private static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Current Time service (CTS)
    private static final UUID CURRENT_TIME_SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Battery Service (BAS)
    private static final UUID BATTERY_LEVEL_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Pulse Oximeter Service (PLX)
    public static final UUID PULSE_OXIMETER_SERVICE_UUID = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OXIMETER_SPOT_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OXIMETER_CONTINUOUS_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Weight Scale Service (WSS)
    public static final UUID WEIGHT_SCALE_SERVICE_UUID = UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb");
    private static final UUID WEIGHT_SCALE_MEASUREMENT_CHAR_UUID = UUID.fromString("00002A9D-0000-1000-8000-00805f9b34fb");

    public static final UUID GLUCOSE_SERVICE_UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002A52-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC_UUID = UUID.fromString("00002A34-0000-1000-8000-00805f9b34fb");

    // Contour Glucose Service
    public static final UUID CONTOUR_SERVICE_UUID = UUID.fromString("00000000-0002-11E2-9E96-0800200C9A66");
    private static final UUID CONTOUR_CLOCK = UUID.fromString("00001026-0002-11E2-9E96-0800200C9A66");

    // Local variables
    public BluetoothCentralManager central;
    private static BluetoothHandler instance = null;
    private final Context context;
    private final Handler handler = new Handler();
    private int currentTimeCounter = 0;

    // new in part 4
    public void connectToAddress(String peripheralMacAddress) {
        Timber.i("BH connectToAddress: %s", peripheralMacAddress);
        central.stopScan();
        BluetoothPeripheral bluetoothPeripheral = central.getPeripheral(peripheralMacAddress);
        central.connectPeripheral(bluetoothPeripheral, peripheralCallback);
    }

    // new in part 2
    public void connectToHeartRateServiceDevice() {
        startScanHrs();
    }

    // new in part 2
    public void disconnectFromHeartRateServiceDevice(String peripheralMacAddress) {
        BluetoothPeripheral connectedPeripheral = central.getPeripheral(peripheralMacAddress);
        central.cancelConnection(connectedPeripheral);
        try {
            central.close();
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    // new in part 2
    public void enableAllSubscriptions(String peripheralMacAddress, boolean enable) {
        BluetoothPeripheral connectedPeripheral = central.getPeripheral(peripheralMacAddress);
        connectedPeripheral.setNotify(CURRENT_TIME_SERVICE_UUID, CURRENT_TIME_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(BLOOD_PRESSURE_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(HEALTH_THERMOMETER_SERVICE_UUID, TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(HEART_RATE_SERVICE_UUID, HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(PULSE_OXIMETER_SERVICE_UUID, PULSE_OXIMETER_CONTINUOUS_MEASUREMENT_CHAR_UUID, enable);
        connectedPeripheral.setNotify(PULSE_OXIMETER_SERVICE_UUID, PULSE_OXIMETER_SPOT_MEASUREMENT_CHAR_UUID, enable);
        connectedPeripheral.setNotify(WEIGHT_SCALE_SERVICE_UUID, WEIGHT_SCALE_MEASUREMENT_CHAR_UUID, enable);
        connectedPeripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID, enable);
        connectedPeripheral.setNotify(CONTOUR_SERVICE_UUID, CONTOUR_CLOCK, enable);
        // new in part 3
        connectedPeripheral.setNotify(BATTERY_LEVEL_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID, enable);
    }

    // Callback for peripherals
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {


        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            // Request a higher MTU, iOS always asks for 185
            peripheral.requestMtu(185);
            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);
            // ### commented this line out as not all (most older) Smartphones don't run Bluetooth 5 with these capabilities
            // peripheral.setPreferredPhy(PhyType.LE_1M, PhyType.LE_2M, PhyOptions.S2);

            // Read manufacturer and model number from the Device Information Service
            peripheral.readCharacteristic(DEVICE_INFORMATION_SERVICE_UUID, MANUFACTURER_NAME_CHARACTERISTIC_UUID);
            peripheral.readCharacteristic(DEVICE_INFORMATION_SERVICE_UUID, MODEL_NUMBER_CHARACTERISTIC_UUID);
            peripheral.readPhy();
            // Turn on notifications for Current Time Service and write it if possible
            BluetoothGattCharacteristic currentTimeCharacteristic = peripheral.getCharacteristic(CURRENT_TIME_SERVICE_UUID, CURRENT_TIME_CHARACTERISTIC_UUID);
            if (currentTimeCharacteristic != null) {

                // changed in part 2, is done in enableAllSubscriptions
                //peripheral.setNotify(currentTimeCharacteristic, true);

                // If it has the write property we write the current time
                if ((currentTimeCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                    // Write the current time unless it is an Omron device
                    if (!isOmronBPM(peripheral.getName())) {
                        BluetoothBytesParser parser = new BluetoothBytesParser();
                        parser.setCurrentTime(Calendar.getInstance());
                        peripheral.writeCharacteristic(currentTimeCharacteristic, parser.getValue(), WriteType.WITH_RESPONSE);
                    }
                }
            }

            // Try to turn on notifications for other characteristics
            // peripheral.readCharacteristic(BATTERY_LEVEL_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID);
            /* enabling is handled in enableAllSubscriptions
            peripheral.setNotify(BLOOD_PRESSURE_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(HEALTH_THERMOMETER_SERVICE_UUID, TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(HEART_RATE_SERVICE_UUID, HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(PULSE_OXIMETER_SERVICE_UUID, PULSE_OXIMETER_CONTINUOUS_MEASUREMENT_CHAR_UUID, true);
            peripheral.setNotify(PULSE_OXIMETER_SERVICE_UUID, PULSE_OXIMETER_SPOT_MEASUREMENT_CHAR_UUID, true);
            peripheral.setNotify(WEIGHT_SCALE_SERVICE_UUID, WEIGHT_SCALE_MEASUREMENT_CHAR_UUID, true);
            peripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_MEASUREMENT_CONTEXT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(GLUCOSE_SERVICE_UUID, GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID, true);
            peripheral.setNotify(CONTOUR_SERVICE_UUID, CONTOUR_CLOCK, true);
             */
        }

        @Override
        public void onNotificationStateUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                final boolean isNotifying = peripheral.isNotifying(characteristic);
                Timber.i("SUCCESS: Notify set to '%s' for %s", isNotifying, characteristic.getUuid());
                if (characteristic.getUuid().equals(CONTOUR_CLOCK)) {
                    writeContourClock(peripheral);
                } else if (characteristic.getUuid().equals(GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID)) {
                    writeGetAllGlucoseMeasurements(peripheral);
                }
            } else {
                Timber.e("ERROR: Changing notification state failed for %s (%s)", characteristic.getUuid(), status);
            }
        }

        @Override
        public void onCharacteristicWrite(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                Timber.i("SUCCESS: Writing <%s> to <%s>", bytes2String(value), characteristic.getUuid());
            } else {
                Timber.i("ERROR: Failed writing <%s> to <%s> (%s)", bytes2String(value), characteristic.getUuid(), status);
            }
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            if (status != GattStatus.SUCCESS) return;

            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                BloodPressureMeasurement measurement = new BloodPressureMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_BLOODPRESSURE);
                intent.putExtra(MEASUREMENT_BLOODPRESSURE_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                TemperatureMeasurement measurement = new TemperatureMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_TEMPERATURE);
                intent.putExtra(MEASUREMENT_TEMPERATURE_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                HeartRateMeasurement measurement = new HeartRateMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_HEARTRATE);
                intent.putExtra(MEASUREMENT_HEARTRATE_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("HeartRate %s", measurement);
            } else if (characteristicUUID.equals(PULSE_OXIMETER_CONTINUOUS_MEASUREMENT_CHAR_UUID)) {
                PulseOximeterContinuousMeasurement measurement = new PulseOximeterContinuousMeasurement(value);
                if (measurement.getSpO2() <= 100 && measurement.getPulseRate() <= 220) {
                    Intent intent = new Intent(MEASUREMENT_PULSE_OX);
                    intent.putExtra(MEASUREMENT_PULSE_OX_EXTRA_CONTINUOUS, measurement);
                    sendMeasurement(intent, peripheral);
                }
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(PULSE_OXIMETER_SPOT_MEASUREMENT_CHAR_UUID)) {
                PulseOximeterSpotMeasurement measurement = new PulseOximeterSpotMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_PULSE_OX);
                intent.putExtra(MEASUREMENT_PULSE_OX_EXTRA_SPOT, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(WEIGHT_SCALE_MEASUREMENT_CHAR_UUID)) {
                WeightMeasurement measurement = new WeightMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_WEIGHT);
                intent.putExtra(MEASUREMENT_WEIGHT_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals((GLUCOSE_MEASUREMENT_CHARACTERISTIC_UUID))) {
                GlucoseMeasurement measurement = new GlucoseMeasurement(value);
                Intent intent = new Intent(MEASUREMENT_GLUCOSE);
                intent.putExtra(MEASUREMENT_GLUCOSE_EXTRA, measurement);
                sendMeasurement(intent, peripheral);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(CURRENT_TIME_CHARACTERISTIC_UUID)) {
                Date currentTime = parser.getDateTime();
                Timber.i("Received device time: %s", currentTime);
                Intent intent = new Intent(BLUETOOTHHANDLER_CURRENT_TIME);
                intent.putExtra(BLUETOOTHHANDLER_CURRENT_TIME_EXTRA, currentTime.toString());
                sendMeasurement(intent, peripheral);
                Timber.d("%s", currentTime);

                // Deal with Omron devices where we can only write currentTime under specific conditions
                if (isOmronBPM(peripheral.getName())) {
                    BluetoothGattCharacteristic bloodpressureMeasurement = peripheral.getCharacteristic(BLOOD_PRESSURE_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID);
                    if (bloodpressureMeasurement == null) return;

                    boolean isNotifying = peripheral.isNotifying(bloodpressureMeasurement);
                    if (isNotifying) currentTimeCounter++;

                    // We can set device time for Omron devices only if it is the first notification and currentTime is more than 10 min from now
                    long interval = abs(Calendar.getInstance().getTimeInMillis() - currentTime.getTime());
                    if (currentTimeCounter == 1 && interval > 10 * 60 * 1000) {
                        parser.setCurrentTime(Calendar.getInstance());
                        peripheral.writeCharacteristic(characteristic, parser.getValue(), WriteType.WITH_RESPONSE);
                    }
                }
            } else if (characteristicUUID.equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                String valueString = parser.getIntValue(FORMAT_UINT8).toString();
                Timber.i("Received battery level %s%%", valueString);
                // new in part 3
                Intent intent = new Intent(BLUETOOTHHANDLER_BATTERY_LEVEL);
                intent.putExtra(BLUETOOTHHANDLER_BATTERY_LEVEL_EXTRA, valueString);
                sendMeasurement(intent, peripheral);

            } else if (characteristicUUID.equals(MANUFACTURER_NAME_CHARACTERISTIC_UUID)) {
                String manufacturer = parser.getStringValue(0);
                Timber.i("Received manufacturer: %s", manufacturer);
            } else if (characteristicUUID.equals(MODEL_NUMBER_CHARACTERISTIC_UUID)) {
                String modelNumber = parser.getStringValue(0);
                Timber.i("Received modelnumber: %s", modelNumber);
            } else if (characteristicUUID.equals(PNP_ID_CHARACTERISTIC_UUID)) {
                String modelNumber = parser.getStringValue(0);
                Timber.i("Received pnp: %s", modelNumber);
            }
        }

        @Override
        public void onMtuChanged(@NotNull BluetoothPeripheral peripheral, int mtu, @NotNull GattStatus status) {
            Timber.i("new MTU set: %d", mtu);
        }

        private void sendMeasurement(@NotNull Intent intent, @NotNull BluetoothPeripheral peripheral) {
            intent.putExtra(MEASUREMENT_EXTRA_PERIPHERAL, peripheral.getAddress());
            context.sendBroadcast(intent);
        }

        private void writeContourClock(@NotNull BluetoothPeripheral peripheral) {
            Calendar calendar = Calendar.getInstance();
            int offsetInMinutes = calendar.getTimeZone().getRawOffset() / 60000;
            int dstSavingsInMinutes = calendar.getTimeZone().getDSTSavings() / 60000;
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
            parser.setIntValue(1, FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.YEAR), FORMAT_UINT16);
            parser.setIntValue(calendar.get(Calendar.MONTH) + 1, FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.DAY_OF_MONTH), FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.HOUR_OF_DAY), FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.MINUTE), FORMAT_UINT8);
            parser.setIntValue(calendar.get(Calendar.SECOND), FORMAT_UINT8);
            parser.setIntValue(offsetInMinutes + dstSavingsInMinutes, FORMAT_SINT16);
            peripheral.writeCharacteristic(CONTOUR_SERVICE_UUID, CONTOUR_CLOCK, parser.getValue(), WriteType.WITH_RESPONSE);
        }

        private void writeGetAllGlucoseMeasurements(@NotNull BluetoothPeripheral peripheral) {
            byte OP_CODE_REPORT_STORED_RECORDS = 1;
            byte OPERATOR_ALL_RECORDS = 1;
            final byte[] command = new byte[]{OP_CODE_REPORT_STORED_RECORDS, OPERATOR_ALL_RECORDS};
            peripheral.writeCharacteristic(GLUCOSE_SERVICE_UUID, GLUCOSE_RECORD_ACCESS_POINT_CHARACTERISTIC_UUID, command, WriteType.WITH_RESPONSE);
        }
    };

    // Callback for central
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            Timber.i("connected to '%s'", peripheral.getName());
            Intent intent = new Intent(BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS);
            String returnString = peripheral.getAddress() + " (" +
                    peripheral.getName() + ")";
            intent.putExtra(BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS_EXTRA, returnString);
            context.sendBroadcast(intent);
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Timber.e("connection '%s' failed with status %s", peripheral.getName(), status);
            Intent intent = new Intent(BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS);
            String returnString = "";
            intent.putExtra(BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS_EXTRA, returnString);
            context.sendBroadcast(intent);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {
            Timber.i("disconnected '%s' with status %s", peripheral.getName(), status);
            Intent intent = new Intent(BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS);
            String returnString = "";
            intent.putExtra(BLUETOOTHHANDLER_PERIPHERAL_MAC_ADDRESS_EXTRA, returnString);
            context.sendBroadcast(intent);

            // do not reconnect to this device when it becomes available again
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //central.autoConnectPeripheral(peripheral, peripheralCallback);
                    central.connectPeripheral(peripheral, peripheralCallback);
                }
            }, 5000);
        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            Timber.i("Found peripheral '%s'", peripheral.getName());
            central.stopScan();

            if (peripheral.getName().contains("Contour") && peripheral.getBondState() == BondState.NONE) {
                // Create a bond immediately to avoid double pairing popups
                central.createBond(peripheral, peripheralCallback);
            } else {
                central.connectPeripheral(peripheral, peripheralCallback);
                //central.autoConnectPeripheral(peripheral, peripheralCallback);
            }
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            Timber.i("bluetooth adapter changed state to %d", state);
            if (state == BluetoothAdapter.STATE_ON) {
                // Bluetooth is on now, start scanning again
                // Scan for peripherals with a certain service UUIDs
                central.startPairingPopupHack();
                // changed in part 2
                //startScan();
                startScanHrs();
            }
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            Timber.i("scanning failed with error %s", scanFailure);
        }
    };

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context.getApplicationContext());
        }
        return instance;
    }

    private BluetoothHandler(Context context) {
        this.context = context;

        // Plant a tree
        Timber.plant(new Timber.DebugTree());

        // Create BluetoothCentral
        central = new BluetoothCentralManager(context, bluetoothCentralManagerCallback, new Handler());

        // Scan for peripherals with a certain service UUIDs
        central.startPairingPopupHack();

        // changed in part 2
        // the scanning is commented out here, it will be done by calling connectToHeartRateServiceDevice
        // startScan();

    }

    /*
    private void startScan() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // ### add HRS = HeartRate service to scan UUIDs
                central.scanForPeripheralsWithServices(new UUID[]{HEART_RATE_SERVICE_UUID, BLOOD_PRESSURE_SERVICE_UUID, HEALTH_THERMOMETER_SERVICE_UUID, PULSE_OXIMETER_SERVICE_UUID, WEIGHT_SCALE_SERVICE_UUID, GLUCOSE_SERVICE_UUID});
                //central.scanForPeripheralsWithServices(new UUID[]{BLP_SERVICE_UUID, HTS_SERVICE_UUID, PLX_SERVICE_UUID, WSS_SERVICE_UUID, GLUCOSE_SERVICE_UUID});
            }
        },1000);

    }
    */

    // new in part 2
    // this will connect to HeartRateService devices only
    private void startScanHrs() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                central.scanForPeripheralsWithServices(new UUID[]{HEART_RATE_SERVICE_UUID});
            }
        }, 1000);
    }


    private boolean isOmronBPM(final String name) {
        return name.contains("BLESmart_") || name.contains("BLEsmart_");
    }
}
