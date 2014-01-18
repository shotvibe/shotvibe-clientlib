#ifndef _SLHashMap_H_
#define _SLHashMap_H_

@protocol JavaUtilCollection;
@protocol JavaUtilSet;

#import "JreEmulation.h"
#include "java/util/Map.h"

@interface SLHashMap : NSObject < JavaUtilMap >

- (id)init;
- (id)initWithInt:(int)capacity;
- (id)initWithJavaUtilMap:(id<JavaUtilMap>)map;
- (void)clear;
- (BOOL)containsKeyWithId:(id)key;
- (BOOL)containsValueWithId:(id)value;
- (id<JavaUtilSet>)entrySet;
- (id)getWithId:(id)key;
- (BOOL)isEmpty;
- (id<JavaUtilSet>)keySet;
- (id)putWithId:(id)key
         withId:(id)value;
- (void)putAllWithJavaUtilMap:(id<JavaUtilMap>)map;
- (id)removeWithId:(id)key;
- (int)size;
- (id<JavaUtilCollection>)values;
- (BOOL)isEqual:(id)param0;
- (NSUInteger)hash;
- (void)copyAllFieldsTo:(SLHashMap *)other;
@end

typedef SLHashMap ComShotvibeShotvibelibHashMap;
#endif // _SLHashMap_H_
