#import <Foundation/Foundation.h>

#include "ArrayList.h"
#include "IOSObjectArray.h"
#include "IOSClass.h"
#include "java/lang/Iterable.h"
#include "java/util/Collection.h"
#include "java/util/Iterator.h"
#include "java/util/List.h"
#include "java/util/ListIterator.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/NullPointerException.h"


@interface SLArrayList_Iterator : NSObject < JavaUtilListIterator >

- (id)initWithNSMutableArray:(NSMutableArray *)array atIndex:(NSUInteger)index;

- (void)addWithId:(id)object;
- (BOOL)hasNext;
- (BOOL)hasPrevious;
- (id)next;
- (int)nextIndex;
- (id)previous;
- (int)previousIndex;
- (void)remove;
- (void)setWithId:(id)object;

@end


@implementation SLArrayList_Iterator
{
    NSMutableArray *array_;
    NSUInteger i_;

    // -1: previous
    //  1: next
    //  0: Illegal
    int mostRecentCall_;
}


- (id)initWithNSMutableArray:(NSMutableArray *)array atIndex:(NSUInteger)index
{
    self = [super init];

    if (self) {
        array_ = array;
        i_ = index;
        mostRecentCall_ = 0;
    }

    return self;
}


- (void)addWithId:(id)object
{
    [array_ insertObject:object atIndex:i_];
    i_++;
    mostRecentCall_ = 0;
}


- (BOOL)hasNext
{
    return i_ < array_.count;
}


- (BOOL)hasPrevious
{
    return i_ > 0;
}


- (id)next
{
    NSObject *result = [array_ objectAtIndex:i_];
    i_++;
    mostRecentCall_ = 1;
    return result;
}


- (int)nextIndex
{
    return i_;
}


- (id)previous
{
    NSObject *result = [array_ objectAtIndex:i_ - 1];
    i_--;
    mostRecentCall_ = -1;
    return result;
}


- (int)previousIndex
{
    return i_ - 1;
}


- (void)remove
{
    switch (mostRecentCall_) {
        case 1:
            [array_ removeObjectAtIndex:i_ - 1];
            i_--;
            break;

        case -1:
            [array_ removeObjectAtIndex:i_];
            break;

        case 0:
            @throw [[JavaLangIllegalStateException alloc] init];
            break;
    }
    mostRecentCall_ = 0;
}


- (void)setWithId:(id)object
{
    switch (mostRecentCall_) {
        case 1:
            [array_ replaceObjectAtIndex:i_ - 1 withObject:object];
            break;

        case -1:
            [array_ replaceObjectAtIndex:i_ withObject:object];
            break;

        case 0:
            @throw [[JavaLangIllegalStateException alloc] init];
            break;
    }
    mostRecentCall_ = 0;
}


@end


@implementation SLArrayList
{
    NSMutableArray *elems_;
}


- (id)init
{
    if (self = [super init]) {
        elems_ = [[NSMutableArray alloc] init];
    }
    return self;
}


- (id)initWithInt:(int)initialCapacity
{
    if (self = [super init]) {
        elems_ = [[NSMutableArray alloc] initWithCapacity:initialCapacity];
    }
    return self;
}


- (id)initWithInitialArray:(NSMutableArray *)array
{
    if (self = [super init]) {
        elems_ = array;
    }
    return self;
}


- (id)initWithJavaUtilCollection:(id<JavaUtilCollection>)c
{
    if (self = [super init]) {
        elems_ = [[NSMutableArray alloc] initWithCapacity:c.size];
        [self addAllWithJavaUtilCollection:c];
    }
    return self;
}


- (void)addWithInt:(int)location
            withId:(id)object
{
    [elems_ insertObject:object atIndex:location];
}


- (BOOL)addWithId:(id)object
{
    [elems_ addObject:object];
    return YES;
}


- (BOOL)addAllWithInt:(int)location withJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    int i = location;
    BOOL changed = NO;
    for (NSObject *o in collection) {
        [elems_ insertObject:o atIndex:i];
        changed = YES;
        i++;
    }
    return changed;
}


- (BOOL)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    BOOL changed = NO;
    for (NSObject *o in collection) {
        [elems_ addObject:o];
        changed = YES;
    }
    return changed;
}


- (void)clear
{
    [elems_ removeAllObjects];
}


- (BOOL)containsWithId:(id)object
{
    return [elems_ containsObject:object];
}


- (BOOL)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    for (NSObject *o in collection) {
        if (![elems_ containsObject:o]) {
            return NO;
        }
    }

    return YES;
}


- (id)getWithInt:(int)location
{
    return [elems_ objectAtIndex:location];
}


- (int)indexOfWithId:(id)object
{
    NSUInteger result = [elems_ indexOfObject:object];
    if (result != NSNotFound) {
        return result;
    } else {
        return -1;
    }
}


- (BOOL)isEmpty
{
    return elems_.count == 0;
}


- (id<JavaUtilIterator>)iterator
{
    return [[SLArrayList_Iterator alloc] initWithNSMutableArray:elems_ atIndex:0];
}


- (int)lastIndexOfWithId:(id)object
{
    NSUInteger index = [elems_ indexOfObjectWithOptions:NSEnumerationReverse
                                            passingTest:^(id obj, NSUInteger i, BOOL *stop) {
        return [object isEqual:obj];
    }];

    if (index != NSNotFound) {
        return index;
    } else {
        return -1;
    }
}


- (id<JavaUtilListIterator>)listIterator
{
    return [[SLArrayList_Iterator alloc] initWithNSMutableArray:elems_ atIndex:0];
}


- (id<JavaUtilListIterator>)listIteratorWithInt:(int)location
{
    return [[SLArrayList_Iterator alloc] initWithNSMutableArray:elems_ atIndex:location];
}


- (id)removeWithInt:(int)location
{
    NSObject *prev = [elems_ objectAtIndex:location];
    [elems_ removeObjectAtIndex:location];
    return prev;
}


- (BOOL)removeWithId:(id)object
{
    NSUInteger result = [elems_ indexOfObject:object];
    if (result != NSNotFound) {
        [elems_ removeObjectAtIndex:result];
        return YES;
    } else {
        return NO;
    }
}


- (BOOL)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    BOOL changed = NO;

    id<JavaUtilListIterator> iter = [self listIterator];
    while ([iter hasNext]) {
        NSObject *val = [iter next];
        if ([collection containsWithId:val]) {
            [iter remove];
            changed = YES;
        }
    }

    return changed;
}


- (BOOL)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection
{
    BOOL changed = NO;

    id<JavaUtilListIterator> iter = [self listIterator];
    while ([iter hasNext]) {
        NSObject *val = [iter next];
        if (![collection containsWithId:val]) {
            [iter remove];
            changed = YES;
        }
    }

    return changed;
}


- (id)setWithInt:(int)location
          withId:(id)object
{
    NSObject *old = [elems_ objectAtIndex:location];
    [elems_ replaceObjectAtIndex:location withObject:object];
    return old;
}


- (int)size
{
    return elems_.count;
}


- (id<JavaUtilList>)subListWithInt:(int)start
                           withInt:(int)end
{
    @throw [[JavaLangRuntimeException alloc] initWithNSString:@"Not Implemented"];
}


- (IOSObjectArray *)toArray
{
    IOSObjectArray *result = [[IOSObjectArray alloc] initWithLength:elems_.count type:[IOSClass objectClass]];

    NSUInteger i = 0;
    for (NSObject *o in elems_) {
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

    if (array.count < elems_.count) {
        return [self toArray];
    }

    NSUInteger i = 0;
    for (NSObject *o in elems_) {
        [array replaceObjectAtIndex:i withObject:o];
        i++;
    }

    // According to the spec: Add a "null" element at the end of the array if there is room
    if (array.count >= elems_.count + 1) {
        [array replaceObjectAtIndex:i withObject:nil];
    }

    return array;
}


- (BOOL)isEqual:(id)param0
{
    return [elems_ isEqual:param0];
}


- (NSUInteger)hash
{
    // TODO ...
    return [super hash];
}


- (void)copyAllFieldsTo:(SLArrayList *)other
{
    [super copyAllFieldsTo:other];
    // TODO
    //other->mElems_ = mElems_;
}


- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len
{
    return [elems_ countByEnumeratingWithState:state objects:stackbuf count:len];
}


- (NSMutableArray *)array
{
    return elems_;
}


@end
