#include "JSONArray.h"
#include "JSONException.h"
#include "JSONObject.h"

// Same as in JSONObject.m
static inline SLJSONObject * toSLJSONObject(id value)
{
    if (![value isKindOfClass:[NSMutableDictionary class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Dictionary, got: %@", [value description]]];
    }

    return [[SLJSONObject alloc] initWithDictionary:value];
}


// Same as in JSONObject.m
static inline SLJSONArray * toSLJSONArray(id value)
{
    if (![value isKindOfClass:[NSMutableArray class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Array, got: %@", [value description]]];
    }

    return [[SLJSONArray alloc] initWithArray:value];
}


// Same as in JSONObject.m
static inline NSString * toString(id value)
{
    if (![value isKindOfClass:[NSString class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON String, got: %@", [value description]]];
    }

    return value;
}


// Same as in JSONObject.m
static inline NSNumber * toNumber(id value)
{
    if (![value isKindOfClass:[NSNumber class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Number, got: %@", [value description]]];
    }

    return value;
}


@implementation SLJSONArray


- (id)init
{
    self = [super init];

    if (self) {
        array_ = [[NSMutableArray alloc] init];
    }

    return self;
}


- (id)initWithArray:(NSMutableArray *)array
{
    self = [super init];

    if (self) {
        array_ = array;
    }

    return self;
}


// Private method
- (id)getValue:(int)index
{
    if (index < 0 || index >= array_.count) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Index %d out of range [0..%lu)", index, (unsigned long)array_.count]];
    }

    return [array_ objectAtIndex:index];
}


- (int)length
{
    return array_.count;
}


- (BOOL)isNullWithInt:(int)index
{
    return [self getValue:index] == [NSNull null];
}


- (BOOL)getBooleanWithInt:(int)index
{
    NSNumber *num = toNumber([self getValue:index]);
    return [num boolValue];
}


- (double)getDoubleWithInt:(int)index
{
    NSNumber *num = toNumber([self getValue:index]);
    return [num doubleValue];
}


- (int)getIntWithInt:(int)index
{
    NSNumber *num = toNumber([self getValue:index]);
    return [num intValue];
}


- (long long int)getLongWithInt:(int)index
{
    NSNumber *num = toNumber([self getValue:index]);
    return [num longLongValue];
}


- (NSString *)getStringWithInt:(int)index
{
    return toString([self getValue:index]);
}


- (SLJSONArray *)getJSONArrayWithInt:(int)index
{
    return toSLJSONArray([self getValue:index]);
}


- (SLJSONObject *)getJSONObjectWithInt:(int)index
{
    return toSLJSONObject([self getValue:index]);
}


- (SLJSONArray *)putNull
{
    [array_ addObject:[NSNull null]];
    return self;
}


- (SLJSONArray *)putWithBoolean:(BOOL)value
{
    [array_ addObject:[[NSNumber alloc] initWithBool:value]];
    return self;
}


- (SLJSONArray *)putWithDouble:(double)value
{
    [array_ addObject:[[NSNumber alloc] initWithDouble:value]];
    return self;
}


- (SLJSONArray *)putWithInt:(int)value
{
    [array_ addObject:[[NSNumber alloc] initWithInt:value]];
    return self;
}


- (SLJSONArray *)putWithLong:(long long int)value
{
    [array_ addObject:[[NSNumber alloc] initWithLongLong:value]];
    return self;
}


- (SLJSONArray *)putWithNSString:(NSString *)value
{
    [array_ addObject:value];
    return self;
}


- (SLJSONArray *)putWithSLJSONObject:(SLJSONObject *)value
{
    [array_ addObject:value->dict_];
    return self;
}


- (SLJSONArray *)putWithNSString:(NSString *)key
                 withSLJSONArray:(SLJSONArray *)value
{
    [array_ addObject:value->array_];
    return self;
}


+ (SLJSONArray *)Parse:(NSData *)data
{
    NSError *error;
    id result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&error];

    if (!result) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Malformed JSON: %@", [error description]]];
    }

    if (![result isKindOfClass:[NSMutableArray class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Array, got: %@", [result description]]];
    }

    return [[SLJSONArray alloc] initWithArray:result];
}


@end
