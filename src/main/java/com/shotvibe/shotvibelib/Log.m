#include "Log.h"

@implementation SLLog

+ (void)dWithNSString:(NSString *)message {
    NSLog(@"%@", message);
}

- (id)init {
  return [super init];
}

@end
