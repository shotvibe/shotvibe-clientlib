#include "ConditionVar.h"

@implementation SLConditionVar
{
    NSCondition *condition_;
}


- (id)init
{
    if (self = [super init]) {
        condition_ = [[NSCondition alloc] init];
    }
    return self;
}


- (void)lock
{
    [condition_ lock];
}


- (void)unlock
{
    [condition_ unlock];
}


- (void)await
{
    [condition_ wait];
}


- (void)signal
{
    [condition_ signal];
}


- (void)signalAll
{
    [condition_ broadcast];
}


@end
