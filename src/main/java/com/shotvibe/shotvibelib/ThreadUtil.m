#include "ThreadUtil.h"

@implementation SLThreadUtil


+ (void)runInBackgroundThreadWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
        // TODO Maybe also catch any exceptions
        [runnable run];
    });
}


+ (void)runInMainThreadWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [runnable run];
    });
}


+ (BOOL)isMainThread
{
    return [NSThread isMainThread];
}


+ (void)sleepWithInt:(int)milliseconds
{
    double time = (double)milliseconds / 1000.0;
    [NSThread sleepForTimeInterval:time];
}


- (id)init
{
    // Not used
    return [super init];
}


@end
