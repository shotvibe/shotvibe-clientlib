#include "HashSet.h"
#include "IOSObjectArray.h"
#include "java/util/Collection.h"
#include "java/util/Iterator.h"

@implementation SLHashSet
{
    NSHashTable *table_;
}


- (id)init
{
    self = [super init];

    if (self) {
        NSUInteger defaultCapacity = 16;

        table_ = [[NSHashTable alloc] initWithOptions:NSHashTableStrongMemory capacity:defaultCapacity];
    }

    return self;
}


- (id)initWithJavaUtilCollection:(id<JavaUtilCollection>)c
{
    self = [super init];

    if (self) {
        table_ = [[NSHashTable alloc] initWithOptions:NSHashTableStrongMemory capacity:[c size]];

        [self addAllWithJavaUtilCollection:c];
    }

    return self;
}


- (id)initWithInt:(int)initialCapacity
{
    self = [super init];

    if (self) {
        table_ = [[NSHashTable alloc] initWithOptions:NSHashTableStrongMemory capacity:initialCapacity];
    }

    return self;
}


- (BOOL)addWithId:(id)object
{
    BOOL alreadyExists = [table_ containsObject:object];
    if (!alreadyExists) {
        [table_ addObject:object];
        return YES;
    } else {
        return NO;
    }
}


- (BOOL)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    BOOL changed = NO;
    for (NSObject *o in collection) {
        if (![table_ containsObject:o]) {
            [table_ addObject:o];
            changed = YES;
        }
    }
    return changed;
}


- (void)clear
{
    [table_ removeAllObjects];
}


- (BOOL)containsWithId:(id)object
{
    return [table_ containsObject:object];
}


- (BOOL)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    for (NSObject *o in collection) {
        if (![table_ containsObject:o]) {
            return NO;
        }
    }

    return YES;
}


- (BOOL)isEmpty
{
    return table_.count == 0;
}


- (BOOL)removeWithId:(id)object
{
    BOOL alreadyExists = [table_ containsObject:object];
    if (alreadyExists) {
        [table_ removeObject:object];
        return YES;
    } else {
        return NO;
    }
}


- (BOOL)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    BOOL changed = NO;

    for (NSObject *o in collection) {
        if ([table_ containsObject:o]) {
            [table_ removeObject:o];
            changed = YES;
        }
    }

    return changed;
}


- (BOOL)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    NSMutableArray *toDelete = [[NSMutableArray alloc] init];

    for (NSObject *o in table_) {
        if (![collection containsWithId:o]) {
            [toDelete addObject:o];
        }
    }

    for (NSObject *o in toDelete) {
        [table_ removeObject:o];
    }

    return toDelete.count > 0;
}


- (int)size
{
    return table_.count;
}


- (void)copyAllFieldsTo:(SLHashSet *)other
{
    // TODO ?
    [super copyAllFieldsTo:other];
}


- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len
{
    return [table_ countByEnumeratingWithState:state objects:stackbuf count:len];
}


@end
