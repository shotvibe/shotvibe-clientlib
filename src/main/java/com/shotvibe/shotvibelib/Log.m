#include "Log.h"

@implementation SLLog

+ (void)dWithNSString:(NSString *)tag withNSString:(NSString *)message
{
    NSLog(@"D/%@: %@", tag, message);
}


- (id)init
{
    return [super init];
}


@end
