#import "BarcodeScannerPlugin.h"
#import <barcode_scanner/barcode_scanner-Swift.h>

@implementation BarcodeScannerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBarcodeScannerPlugin registerWithRegistrar:registrar];
}
@end
