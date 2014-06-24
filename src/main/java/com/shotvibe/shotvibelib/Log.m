#include "Log.h"

OBJC_EXTERN void CLSLog(NSString *format, ...) NS_FORMAT_FUNCTION(1,2);

@implementation SLLog

+ (void)dWithNSString:(NSString *)tag withNSString:(NSString *)message {
    NSLog(@"D/%@: %@", tag, message);
}

+ (void)CLSLogWithNSString:(NSString *)message {
    CLSLog(@"%@", message);
}

- (id)init {
  return [super init];
}

@end
