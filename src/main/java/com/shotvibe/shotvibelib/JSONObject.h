#ifndef _SLJSONObject_H_
#define _SLJSONObject_H_

@class SLJSONArray;

#import "JreEmulation.h"

@interface SLJSONObject : NSObject
{
    @public
    NSMutableDictionary *dict_;
}

- (id)init;
- (BOOL)hasWithNSString:(NSString *)key;
- (BOOL)isNullWithNSString:(NSString *)key;
- (BOOL)getBooleanWithNSString:(NSString *)key;
- (double)getDoubleWithNSString:(NSString *)key;
- (int)getIntWithNSString:(NSString *)key;
- (long long int)getLongWithNSString:(NSString *)key;
- (NSString *)getStringWithNSString:(NSString *)key;
- (SLJSONArray *)getJSONArrayWithNSString:(NSString *)key;
- (SLJSONObject *)getJSONObjectWithNSString:(NSString *)key;
- (SLJSONObject *)putNullWithNSString:(NSString *)key;
- (SLJSONObject *)putWithNSString:(NSString *)key
                      withBoolean:(BOOL)value;
- (SLJSONObject *)putWithNSString:(NSString *)key
                       withDouble:(double)value;
- (SLJSONObject *)putWithNSString:(NSString *)key
                          withInt:(int)value;
- (SLJSONObject *)putWithNSString:(NSString *)key
                         withLong:(long long int)value;
- (SLJSONObject *)putWithNSString:(NSString *)key
                     withNSString:(NSString *)value;
- (SLJSONObject *)putWithNSString:(NSString *)key
                 withSLJSONObject:(SLJSONObject *)value;
- (SLJSONObject *)putWithNSString:(NSString *)key
                  withSLJSONArray:(SLJSONArray *)value;
+ (SLJSONObject *)ParseWithNSString:(NSString *)data;
- (NSString *)description;

// Should be called only from Objective-C code
- (id)initWithDictionary:(NSMutableDictionary *)dictionary;
+ (SLJSONObject *)Parse:(NSData *)data;
@end

typedef SLJSONObject ComShotvibeShotvibelibJSONObject;
#endif // _SLJSONObject_H_
