#ifndef _SLHashSet_H_
#define _SLHashSet_H_

@class IOSObjectArray;
@protocol JavaUtilCollection;
@protocol JavaUtilIterator;

#import "JreEmulation.h"
#include "java/util/Set.h"

@interface SLHashSet : NSObject < JavaUtilSet >

- (id)init;
- (id)initWithJavaUtilCollection:(id<JavaUtilCollection>)c;
- (id)initWithInt:(int)initialCapacity;
- (BOOL)addWithId:(id)object;
- (BOOL)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (void)clear;
- (BOOL)containsWithId:(id)object;
- (BOOL)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (BOOL)isEmpty;
- (id<JavaUtilIterator>)iterator;
- (BOOL)removeWithId:(id)object;
- (BOOL)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (BOOL)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)collection;
- (int)size;
- (IOSObjectArray *)toArray;
- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray *)array;
- (void)copyAllFieldsTo:(SLHashSet *)other;
@end

typedef SLHashSet ComShotvibeShotvibelibHashSet;
#endif // _SLHashSet_H_
