#ifndef _SLJSONArray_H_
#define _SLJSONArray_H_

@class SLJSONObject;

#import "JreEmulation.h"

@interface SLJSONArray : NSObject
{
    @public
    NSMutableArray *array_;
}

- (id)init;
- (int)length;
- (BOOL)isNullWithInt:(int)index;
- (BOOL)getBooleanWithInt:(int)index;
- (double)getDoubleWithInt:(int)index;
- (int)getIntWithInt:(int)index;
- (long long int)getLongWithInt:(int)index;
- (NSString *)getStringWithInt:(int)index;
- (SLJSONArray *)getJSONArrayWithInt:(int)index;
- (SLJSONObject *)getJSONObjectWithInt:(int)index;
- (SLJSONArray *)putNull;
- (SLJSONArray *)putWithBoolean:(BOOL)value;
- (SLJSONArray *)putWithDouble:(double)value;
- (SLJSONArray *)putWithInt:(int)value;
- (SLJSONArray *)putWithLong:(long long int)value;
- (SLJSONArray *)putWithNSString:(NSString *)value;
- (SLJSONArray *)putWithSLJSONObject:(SLJSONObject *)value;
- (SLJSONArray *)putWithSLJSONArray:(SLJSONArray *)value;
+ (SLJSONArray *)ParseWithNSString:(NSString *)data;
- (NSString *)description;

// Should be called only from Objective-C code
- (id)initWithArray:(NSMutableArray *)array;
+ (SLJSONArray *)Parse:(NSData *)data;
@end

typedef SLJSONArray ComShotvibeShotvibelibJSONArray;
#endif // _SLJSONArray_H_
