#ifndef _SLArrayList_H_
#define _SLArrayList_H_

#import <Foundation/Foundation.h>

@class IOSObjectArray;
@protocol JavaUtilIterator;
@protocol JavaUtilListIterator;

#import "JreEmulation.h"
#include "java/lang/Iterable.h"
#include "java/util/Collection.h"
#include "java/util/List.h"

@interface SLArrayList : NSObject < JavaLangIterable, JavaUtilCollection, JavaUtilList >

- (id)init;
- (id)initWithInt:(int)initialCapacity;
- (id)initWithJavaUtilCollection:(id<JavaUtilCollection>)c;
- (void)addWithInt:(int)location
            withId:(id)object;
- (BOOL)addWithId:(id)object;
- (BOOL)addAllWithInt:(int)location withJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (BOOL)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (void)clear;
- (BOOL)containsWithId:(id)object;
- (BOOL)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (id)getWithInt:(int)location;
- (int)indexOfWithId:(id)object;
- (BOOL)isEmpty;
- (id<JavaUtilIterator>)iterator;
- (int)lastIndexOfWithId:(id)object;
- (id<JavaUtilListIterator>)listIterator;
- (id<JavaUtilListIterator>)listIteratorWithInt:(int)location;
- (id)removeWithInt:(int)location;
- (BOOL)removeWithId:(id)object;
- (BOOL)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (BOOL)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (id)setWithInt:(int)location
          withId:(id)object;
- (int)size;
- (id<JavaUtilList>)subListWithInt:(int)start
                           withInt:(int)end;
- (IOSObjectArray *)toArray;
- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray *)array;
- (BOOL)isEqual:(id)param0;
- (NSUInteger)hash;
- (void)copyAllFieldsTo:(SLArrayList *)other;
@end

typedef SLArrayList ComShotvibeShotvibelibArrayList;
#endif // _SLArrayList_H_
