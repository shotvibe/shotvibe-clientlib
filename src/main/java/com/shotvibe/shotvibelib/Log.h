#ifndef _SLLog_H_
#define _SLLog_H_

#import "JreEmulation.h"

@interface SLLog : NSObject {
}

+ (void)dWithNSString:(NSString *)tag withNSString:(NSString *)message;
+ (void)CLSLogWithNSString:(NSString *)message;
- (id)init;
@end

typedef SLLog ComShotvibeShotvibelibLog;

#endif // _SLLog_H_
