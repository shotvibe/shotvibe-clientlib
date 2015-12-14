#include "JSONArray.h"
#include "JSONException.h"
#include "JSONObject.h"
#include "java/lang/IllegalStateException.h"

// Same as in JSONArray.m
static inline SLJSONObject * toSLJSONObject(id value)
{
    if (![value isKindOfClass:[NSMutableDictionary class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Dictionary, got: %@", [value description]]];
    }

    return [[SLJSONObject alloc] initWithDictionary:value];
}


// Same as in JSONArray.m
static inline SLJSONArray * toSLJSONArray(id value)
{
    if (![value isKindOfClass:[NSMutableArray class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Array, got: %@", [value description]]];
    }

    return [[SLJSONArray alloc] initWithArray:value];
}


// Same as in JSONArray.m
static inline NSString * toString(id value)
{
    if (![value isKindOfClass:[NSString class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON String, got: %@", [value description]]];
    }

    return value;
}


// Same as in JSONArray.m
static inline NSNumber * toNumber(id value)
{
    if (![value isKindOfClass:[NSNumber class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Number, got: %@", [value description]]];
    }

    return value;
}


@implementation SLJSONObject


- (id)init
{
    self = [super init];

    if (self) {
        dict_ = [[NSMutableDictionary alloc] init];
    }

    return self;
}


- (id)initWithDictionary:(NSMutableDictionary *)dictionary
{
    self = [super init];

    if (self) {
        dict_ = dictionary;
    }

    return self;
}


// Private method
- (id)getValue:(NSString *)key
{
    id result = [dict_ objectForKey:key];

    if (!result) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Key \"%@\" does not exist in %@", key, dict_]];
    }

    return result;
}


- (BOOL)hasWithNSString:(NSString *)key
{
    return [dict_ objectForKey:key] != nil;
}


- (BOOL)isNullWithNSString:(NSString *)key
{
    id val = [self getValue:key];
    if (!val) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Key \"%@\" does not exist in %@", key, dict_]];
    }

    return val == [NSNull null];
}


- (BOOL)getBooleanWithNSString:(NSString *)key
{
    NSNumber *num = toNumber([self getValue:key]);
    return [num boolValue];
}


- (double)getDoubleWithNSString:(NSString *)key
{
    NSNumber *num = toNumber([self getValue:key]);
    return [num doubleValue];
}


- (int)getIntWithNSString:(NSString *)key
{
    NSNumber *num = toNumber([self getValue:key]);
    return [num intValue];
}


- (long long int)getLongWithNSString:(NSString *)key
{
    NSNumber *num = toNumber([self getValue:key]);
    return [num longLongValue];
}


- (NSString *)getStringWithNSString:(NSString *)key
{
    return toString([self getValue:key]);
}


- (SLJSONArray *)getJSONArrayWithNSString:(NSString *)key
{
    return toSLJSONArray([self getValue:key]);
}


- (SLJSONObject *)getJSONObjectWithNSString:(NSString *)key
{
    return toSLJSONObject([self getValue:key]);
}


- (SLJSONObject *)putNullWithNSString:(NSString *)key
{
    [dict_ setObject:[NSNull null] forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                      withBoolean:(BOOL)value
{
    [dict_ setObject:[[NSNumber alloc] initWithBool:value] forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                       withDouble:(double)value
{
    [dict_ setObject:[[NSNumber alloc] initWithDouble:value] forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                          withInt:(int)value
{
    [dict_ setObject:[[NSNumber alloc] initWithInt:value] forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                         withLong:(long long int)value
{
    [dict_ setObject:[[NSNumber alloc] initWithLongLong:value] forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                     withNSString:(NSString *)value
{
    [dict_ setObject:value forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                 withSLJSONObject:(SLJSONObject *)value
{
    [dict_ setObject:value->dict_ forKey:key];
    return self;
}


- (SLJSONObject *)putWithNSString:(NSString *)key
                  withSLJSONArray:(SLJSONArray *)value
{
    [dict_ setObject:value->array_ forKey:key];
    return self;
}


+ (SLJSONObject *)ParseWithNSString:(NSString *)data
{
    NSData *d = [data dataUsingEncoding:NSUTF8StringEncoding];
    return [SLJSONObject Parse:d];
}


- (NSString *)description
{
    NSError *jsonError;
    NSData *data = [NSJSONSerialization dataWithJSONObject:dict_ options:kNilOptions error:&jsonError];
    if (!data) {
        // This should never happen, since SLJSONObject is built using a safe API that enforces correct JSON
        @throw [[JavaLangIllegalStateException alloc] initWithNSString:[NSString stringWithFormat:@"Impossible happened: %@", jsonError.description]];
    }

    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}


+ (SLJSONObject *)Parse:(NSData *)data
{
    NSError *error;
    id result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&error];

    if (!result) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Malformed JSON: %@", [error description]]];
    }

    if (![result isKindOfClass:[NSMutableDictionary class]]) {
        @throw [[SLJSONException alloc] initWithNSString:[NSString stringWithFormat:@"Expected a JSON Dictionary, got: %@", [result description]]];
    }

    return [[SLJSONObject alloc] initWithDictionary:result];
}


@end
