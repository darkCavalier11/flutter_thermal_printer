# flutter_thermal_printer

Plugin to connect and print receipt in thermal printer.

## Methods
`getAllPairedDevices` -> get all the paired printers earlier. A printer needs to be paired to be connected and print receipt

`connectToPrinterByAddress(String address)` used to connect to the printer having the given address.

`isConnected()` returns if a printer is connected.

`disconnect()` disconnect to printer if a printer is connected.
