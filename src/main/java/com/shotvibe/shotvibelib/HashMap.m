#include "IOSClass.h"
#include "HashMap.h"
#include "java/util/Collection.h"
#include "java/util/Map.h"
#include "java/util/Set.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/NullPointerException.h"


@interface SLHashMap_KeySet : NSObject < JavaUtilSet >

- (id)initWithMapTable:(NSMapTable *)mapTable;

@end


@implementation SLHashMap_KeySet
{
    NSMapTable *map_;
}

- (id)initWithMapTable:(NSMapTable *)mapTable
{
    self = [super init];

    if (self) {
        map_ = mapTable;
    }

    return self;
}


- (BOOL)addWithId:(id)object
{
    // TODO This should be changed to JavaLangUnsupportedOperationException
    @throw [[JavaLangIllegalStateException alloc] init];
}


- (BOOL)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    // TODO This should be changed to JavaLangUnsupportedOperationException
    @throw [[JavaLangIllegalStateException alloc] init];
}


- (void)clear
{
    [map_ removeAllObjects];
}


- (BOOL)containsWithId:(id)object
{
    return [map_ objectForKey:object] != nil;
}


- (BOOL)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    for (NSObject *o in collection) {
        if (![map_ objectForKey:o]) {
            return NO;
        }
    }
    return YES;
}


- (BOOL)isEmpty
{
    return map_.count == 0;
}


- (id<JavaUtilIterator>)iterator
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)removeWithId:(id)object
{
    BOOL existed = [map_ objectForKey:object] != nil;
    [map_ removeObjectForKey:object];
    return existed;
}


- (BOOL)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    BOOL changed = NO;
    for (NSObject *o in collection) {
        if ([map_ objectForKey:o]) {
            [map_ removeObjectForKey:o];
            changed = YES;
        }
    }
    return changed;
}


- (BOOL)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (int)size
{
    return map_.count;
}


- (IOSObjectArray *)toArray
{
    IOSObjectArray *result = [[IOSObjectArray alloc] initWithLength:map_.count type:[IOSClass objectClass]];

    NSUInteger i = 0;
    for (NSObject *o in [map_ keyEnumerator]) {
        [result replaceObjectAtIndex:i withObject:o];
        i++;
    }

    return result;
}


- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray *)array
{
    if (!array) {
        @throw [[JavaLangNullPointerException alloc] init];
    }

    if (array.count < map_.count) {
        return [self toArray];
    }

    NSUInteger i = 0;
    for (NSObject *o in [map_ keyEnumerator]) {
        [array replaceObjectAtIndex:i withObject:o];
        i++;
    }

    // According to the spec: Add a "null" element at the end of the array if there is room
    if (array.count >= map_.count + 1) {
        [array replaceObjectAtIndex:i withObject:nil];
    }

    return array;
}


- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len
{
    return [map_ countByEnumeratingWithState:state objects:stackbuf count:len];
}


@end


@interface SLHashMap_Entry : NSObject < JavaUtilMap_Entry >

- (id)initWithMapTable:(NSMapTable *)mapTable key:(NSObject *)key;

@end


@implementation SLHashMap_Entry
{
    NSMapTable *map_;
    NSObject *key_;
    NSObject *value_;
}

- (id)initWithMapTable:(NSMapTable *)mapTable key:(NSObject *)key
{
    self = [super init];

    if (self) {
        map_ = mapTable;
        key_ = key;
    }

    return self;
}


- (id)getKey
{
    return key_;
}


- (id)getValue
{
    return [map_ objectForKey:key_];
}


- (id)setValueWithId:(id)object
{
    NSObject *prev = [map_ objectForKey:key_];
    [map_ setObject:object forKey:key_];
    return prev;
}


@end


@interface SLHashMap_EntrySet : NSObject < JavaUtilSet >


- (id)initWithMapTable:(NSMapTable *)mapTable;


@end

@implementation SLHashMap_EntrySet
{
    NSMapTable *map_;
}


- (id)initWithMapTable:(NSMapTable *)mapTable
{
    self = [super init];

    if (self) {
        map_ = mapTable;
    }

    return self;
}


- (BOOL)addWithId:(id)object
{
    // TODO This should be changed to JavaLangUnsupportedOperationException
    @throw [[JavaLangIllegalStateException alloc] init];
}


- (BOOL)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    // TODO This should be changed to JavaLangUnsupportedOperationException
    @throw [[JavaLangIllegalStateException alloc] init];
}


- (void)clear
{
    [map_ removeAllObjects];
}


- (BOOL)containsWithId:(id)object
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)isEmpty
{
    return map_.count == 0;
}


- (id<JavaUtilIterator>)iterator
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)removeWithId:(id)object
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (int)size
{
    return map_.count;
}


- (IOSObjectArray *)toArray
{
    IOSObjectArray *result = [[IOSObjectArray alloc] initWithLength:map_.count type:[IOSClass objectClass]];

    NSUInteger i = 0;
    for (NSObject *o in map_) {
        [result replaceObjectAtIndex:i withObject:[[SLHashMap_Entry alloc] initWithMapTable:map_ key:o]];
        i++;
    }

    return result;
}


- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray *)array
{
    if (!array) {
        @throw [[JavaLangNullPointerException alloc] init];
    }

    if (array.count < map_.count) {
        return [self toArray];
    }

    NSUInteger i = 0;
    for (NSObject *o in map_) {
        [array replaceObjectAtIndex:i withObject:[[SLHashMap_Entry alloc] initWithMapTable:map_ key:o]];
        i++;
    }

    // According to the spec: Add a "null" element at the end of the array if there is room
    if (array.count >= map_.count + 1) {
        [array replaceObjectAtIndex:i withObject:nil];
    }

    return array;
}


- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len
{
    NSUInteger n = [map_ countByEnumeratingWithState:state objects:stackbuf count:len];

    for (NSUInteger i = 0; i < n; ++i) {
        NSString *key = state->itemsPtr[i];
        SLHashMap_Entry *__autoreleasing tmp = [[SLHashMap_Entry alloc] initWithMapTable:map_ key:key];
        state->itemsPtr[i] = tmp;
    }

    return n;
}


@end


@implementation SLHashMap
{
    NSMapTable *map_;
}


- (id)init
{
    self = [super init];

    if (self) {
        NSUInteger defaultCapacity = 16;

        map_ = [[NSMapTable alloc] initWithKeyOptions:NSMapTableStrongMemory valueOptions:NSMapTableStrongMemory capacity:defaultCapacity];
    }

    return self;
}


- (id)initWithInt:(int)capacity
{
    self = [super init];

    if (self) {
        map_ = [[NSMapTable alloc] initWithKeyOptions:NSMapTableStrongMemory valueOptions:NSMapTableStrongMemory capacity:capacity];
    }

    return self;
}


- (id)initWithJavaUtilMap:(id<JavaUtilMap>)map
{
    self = [super init];

    if (self) {
        map_ = [[NSMapTable alloc] initWithKeyOptions:NSMapTableStrongMemory valueOptions:NSMapTableStrongMemory capacity:[map size]];

        [self putAllWithJavaUtilMap:map];
    }

    return self;
}


- (void)clear
{
    [map_ removeAllObjects];
}


- (BOOL)containsKeyWithId:(id)key
{
    return [map_ objectForKey:key] != nil;
}


- (BOOL)containsValueWithId:(id)value
{
    for (NSObject *i in [map_ objectEnumerator]) {
        if ([i isEqual:value]) {
            return YES;
        }
    }

    return NO;
}


- (id<JavaUtilSet>)entrySet
{
    return [[SLHashMap_EntrySet alloc] initWithMapTable:map_];
}


- (id)getWithId:(id)key
{
    return [map_ objectForKey:key];
}


- (BOOL)isEmpty
{
    return map_.count == 0;
}


- (id<JavaUtilSet>)keySet
{
    return [[SLHashMap_KeySet alloc] initWithMapTable:map_];
}


- (id)putWithId:(id)key
         withId:(id)value
{
    NSObject *prev = [map_ objectForKey:key];
    [map_ setObject:value forKey:key];
    return prev;
}


- (void)putAllWithJavaUtilMap:(id<JavaUtilMap>)map
{
    for (id<JavaUtilMap_Entry> entry in [map entrySet]) {
        [map_ setObject:[entry getValue] forKey:[entry getKey]];
    }
}


- (id)removeWithId:(id)key
{
    NSObject *prev = [map_ objectForKey:key];
    [map_ removeObjectForKey:key];
    return prev;
}


- (int)size
{
    return map_.count;
}


- (id<JavaUtilCollection>)values
{
    // TODO ...
    @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"NOT IMPLEMENTED YET!"];
}


- (BOOL)isEqual:(id)param0
{
    return [map_ isEqual:param0];
}


- (NSUInteger)hash
{
    // TODO ...
    return [super hash];
}


- (void)copyAllFieldsTo:(SLHashMap *)other
{
    // TODO ?
    [super copyAllFieldsTo:other];
}


@end
