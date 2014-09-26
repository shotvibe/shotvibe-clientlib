#ifndef _SLCollectionUtils_H_
#define _SLCollectionUtils_H_

@class SLArrayList;
@protocol SLCollectionUtils_Comparator;

#import "JreEmulation.h"

@interface SLCollectionUtils : NSObject {
}

+ (void)sortArrayListWithSLArrayList:(SLArrayList *)list
    withSLCollectionUtils_Comparator:(id<SLCollectionUtils_Comparator>)comparator;
- (id)init;
@end

typedef SLCollectionUtils ComShotvibeShotvibelibCollectionUtils;

@protocol SLCollectionUtils_Comparator < NSObject, JavaObject >
- (int)compareWithId:(id)lhs
              withId:(id)rhs;
@end
#endif // _SLCollectionUtils_H_
