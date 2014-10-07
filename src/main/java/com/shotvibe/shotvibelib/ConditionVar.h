#ifndef _SLConditionVar_H_
#define _SLConditionVar_H_

@protocol JavaUtilConcurrentLocksCondition;
@protocol JavaUtilConcurrentLocksLock;

#import "JreEmulation.h"

@interface SLConditionVar : NSObject

- (id)init;
- (void)lock;
- (void)unlock;
- (void)await;
- (void)signal;
- (void)signalAll;

@end

typedef SLConditionVar ComShotvibeShotvibelibConditionVar;
#endif // _SLConditionVar_H_
