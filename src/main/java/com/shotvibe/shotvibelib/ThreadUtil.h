#ifndef _SLThreadUtil_H_
#define _SLThreadUtil_H_

@protocol SLThreadUtil_Runnable;

#import "JreEmulation.h"

@interface SLThreadUtil : NSObject {
}

+ (void)runInBackgroundThreadWithSLThreadUtil_Runnable:(id<SLThreadUtil_Runnable>)runnable;
+ (BOOL)isMainThread;
- (id)init;
@end

typedef SLThreadUtil ComShotvibeShotvibelibThreadUtil;

@protocol SLThreadUtil_Runnable < NSObject, JavaObject >
- (void)run;
@end
#endif // _SLThreadUtil_H_
