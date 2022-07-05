#import "FlutterThermalPrinterPlugin.h"
#if __has_include(<flutter_thermal_printer/flutter_thermal_printer-Swift.h>)
#import <flutter_thermal_printer/flutter_thermal_printer-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_thermal_printer-Swift.h"
#endif

@implementation FlutterThermalPrinterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterThermalPrinterPlugin registerWithRegistrar:registrar];
}
@end
