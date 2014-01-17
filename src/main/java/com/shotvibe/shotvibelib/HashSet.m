#include "HashSet.h"
#include "IOSObjectArray.h"
#include "IOSClass.h"
#include "java/util/Collection.h"
#include "java/util/Iterator.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/NullPointerException.h"


@interface SLHashSet_Iterator : NSObject < JavaUtilIterator >
{
    NSHashTable *hashTable_;
    NSEnumerator *enumerator_;
    id prev_;
    id next_;
}

- (id)initWithHashTable:(NSHashTable *)hashTable;

@end


@implementation SLHashSet_Iterator


- (id)initWithHashTable:(NSHashTable *)hashTable
{
    self = [super init];

    if (self) {
        hashTable_ = hashTable;
        enumerator_ = [hashTable objectEnumerator];
        prev_ = nil;
        next_ = [enumerator_ nextObject];
    }

    return self;
}


- (BOOL)hasNext
{
    return next_ != nil;
}


- (id)next
{
    prev_ = next_;
    next_ = [enumerator_ nextObject];
    return prev_;
}


- (void)remove
{
    if (!prev_) {
        @throw [[JavaLangIllegalStateException alloc] initWithNSString:@"next has not been called, or remove has already been called"];
    }

    [hashTable_ removeObject:prev_];

    enumerator_ = [hashTable_ objectEnumerator];
    prev_ = nil;

    while (next_ != [enumerator_ nextObject]) {
        // Nothing to do, just advance
    }
}


@end


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


- (id<JavaUtilIterator>)iterator
{
    return [[SLHashSet_Iterator alloc] initWithHashTable:table_];
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


- (IOSObjectArray *)toArray
{
    IOSObjectArray *result = [[IOSObjectArray alloc] initWithLength:table_.count type:[IOSClass objectClass]];

    NSUInteger i = 0;
    for (NSObject *o in table_) {
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

    if (array.count < table_.count) {
        return [self toArray];
    }

    NSUInteger i = 0;
    for (NSObject *o in table_) {
        [array replaceObjectAtIndex:i withObject:o];
        i++;
    }

    // According to the spec: Add a "null" element at the end of the array if there is room
    if (array.count >= table_.count + 1) {
        [array replaceObjectAtIndex:i withObject:nil];
    }

    return array;
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
