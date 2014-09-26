#ifndef _SLThreadUtil_H_
#define _SLThreadUtil_H_

@protocol SLThreadUtil_Executor;
@protocol SLThreadUtil_Runnable;

#import "JreEmulation.h"

@interface SLThreadUtil : NSObject {
}

+ (void)runInBackgroundThreadWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable;
+ (void)runInMainThreadWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable;
+ (BOOL)isMainThread;
+ (id<SLThreadUtil_Executor>)createSingleThreadExecutor;
+ (void)sleepWithInt:(int)milliseconds;
- (id)init;
@end

typedef SLThreadUtil ComShotvibeShotvibelibThreadUtil;

@protocol SLThreadUtil_Runnable < NSObject, JavaObject >
- (void)run;
@end

@protocol SLThreadUtil_Executor < NSObject, JavaObject >
- (void)executeWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable;
@end
#endif // _SLThreadUtil_H_
