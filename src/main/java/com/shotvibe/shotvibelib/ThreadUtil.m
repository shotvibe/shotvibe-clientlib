#include "ThreadUtil.h"

@interface IosExecutor : NSObject <SLThreadUtil_Executor>

@end

@implementation IosExecutor
{
    dispatch_queue_t queue_;
}

- (id)init
{
    self = [super init];

    if (self) {
        queue_ = dispatch_queue_create(0, DISPATCH_QUEUE_SERIAL);
    }

    return self;
}


- (void)executeWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable
{
    dispatch_async(queue_, ^{
        // TODO Maybe also catch any exceptions
        [runnable run];
    });
}


@end

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


+ (id<SLThreadUtil_Executor>)createSingleThreadExecutor
{
    return [[IosExecutor alloc] init];
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
