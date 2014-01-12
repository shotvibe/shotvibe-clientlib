#ifndef _SLDateTime_H_
#define _SLDateTime_H_

#import "JreEmulation.h"

@interface SLDateTime : NSObject

+ (SLDateTime *)FromTimeStampWithLong:(long long int)timeStamp;
- (long long int)getTimeStamp;
- (NSString *)formatISO8601;
+ (SLDateTime *)ParseISO8601WithNSString:(NSString *)input;
- (void)copyAllFieldsTo:(SLDateTime *)other;
@end

typedef SLDateTime ComShotvibeShotvibelibDateTime;
#endif // _SLDateTime_H_
