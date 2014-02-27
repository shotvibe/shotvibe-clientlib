#include "Log.h"

@implementation SLLog

+ (void)dWithNSString:(NSString *)tag withNSString:(NSString *)message {
    NSLog(@"D/%@:%@", message);
}

- (id)init {
  return [super init];
}

@end
