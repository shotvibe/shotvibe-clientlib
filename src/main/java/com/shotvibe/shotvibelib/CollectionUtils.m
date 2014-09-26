#include "ArrayList.h"
#include "CollectionUtils.h"

@implementation SLCollectionUtils


+ (void)sortArrayListWithSLArrayList:(SLArrayList *)list
    withSLCollectionUtils_Comparator:(id<SLCollectionUtils_Comparator>)comparator
{
    NSMutableArray *arr = [list array];
    [arr sortUsingComparator:^NSComparisonResult (id obj1, id obj2) {
        int result = [comparator compareWithId:obj1 withId:obj2];
        if (result < 0) {
            return NSOrderedAscending;
        } else if (result > 0) {
            return NSOrderedDescending;
        } else {
            return NSOrderedSame;
        }
    }];
}


- (id)init
{
    return [super init];
}


@end
