#include "DateTime.h"
#include "java/lang/IllegalArgumentException.h"

@implementation SLDateTime
{
    int64_t timeStamp_;
}

static NSRegularExpression *parseRegex;
static NSCalendar *calendar;


+ (void)initialize
{
    if (self == [SLDateTime class]) {
        NSString *pattern = @"(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})([.,](\\d{1,6}))?Z";
        parseRegex = [[NSRegularExpression alloc] initWithPattern:pattern options:0 error:nil];

        calendar = [[NSCalendar alloc] initWithCalendarIdentifier:NSGregorianCalendar];
        [calendar setTimeZone:[NSTimeZone timeZoneWithName:@"UTC"]];
    }
}


- (id)initWithTimeStamp:(long long int)timeStamp
{
    self = [super init];

    if (self) {
        timeStamp_ = timeStamp;
    }

    return self;
}


+ (SLDateTime *)FromTimeStampWithLong:(long long int)timeStamp
{
    return [[SLDateTime alloc] initWithTimeStamp:timeStamp];
}


+ (SLDateTime *)NowUTC
{
    NSTimeInterval seconds = [[NSDate date] timeIntervalSince1970];
    long long secondsLong = seconds;

    return [[SLDateTime alloc] initWithTimeStamp:secondsLong * 1000000LL];
}


- (long long int)getTimeStamp
{
    return timeStamp_;
}


- (NSString *)formatISO8601
{
    NSTimeInterval seconds = [self getTimeStamp] / 1000000LL;
    NSDate *date = [NSDate dateWithTimeIntervalSince1970:seconds];

    NSUInteger unitFlags =
        NSYearCalendarUnit |
        NSMonthCalendarUnit |
        NSDayCalendarUnit |
        NSHourCalendarUnit |
        NSMinuteCalendarUnit |
        NSSecondCalendarUnit;

    NSDateComponents *dateComponents = [calendar components:unitFlags fromDate:date];

    long year = dateComponents.year;
    long month = dateComponents.month;
    long day = dateComponents.day;
    long hour = dateComponents.hour;
    long minute = dateComponents.minute;
    long second = dateComponents.second;

    long long microseconds = [self getTimeStamp] % 1000000LL;

    return [NSString stringWithFormat:@"%04ld-%02ld-%02ldT%02ld:%02ld:%02ld.%06lldZ", year, month, day, hour, minute, second, microseconds];
}


+ (SLDateTime *)ParseISO8601WithNSString:(NSString *)input
{
    if (!input) {
        @throw [[JavaLangIllegalArgumentException alloc] initWithNSString:@"input cannot be null"];
    }

    NSArray *results = [parseRegex matchesInString:input options:0 range:NSMakeRange(0, input.length)];

    if (results.count < 1) {
        return nil;
    }

    NSTextCheckingResult *result = [results objectAtIndex:0];

    NSInteger year = [[input substringWithRange:[result rangeAtIndex:1]] integerValue];
    NSInteger month = [[input substringWithRange:[result rangeAtIndex:2]] integerValue];
    NSInteger day = [[input substringWithRange:[result rangeAtIndex:3]] integerValue];
    NSInteger hour = [[input substringWithRange:[result rangeAtIndex:4]] integerValue];
    NSInteger minute = [[input substringWithRange:[result rangeAtIndex:5]] integerValue];
    NSInteger second = [[input substringWithRange:[result rangeAtIndex:6]] integerValue];

    NSDateComponents *dateComponents = [[NSDateComponents alloc] init];
    [dateComponents setYear:year];
    [dateComponents setMonth:month];
    [dateComponents setDay:day];
    [dateComponents setHour:hour];
    [dateComponents setMinute:minute];
    [dateComponents setSecond:second];

    NSRange fractionalSecondsRange = [result rangeAtIndex:8];

    long long microseconds;

    if (fractionalSecondsRange.location == NSNotFound) {
        microseconds = 0;
    } else {
        NSString *fractionalSeconds = [input substringWithRange:fractionalSecondsRange];
        microseconds = [fractionalSeconds longLongValue];
        for (int i = 0; i < 6 - fractionalSeconds.length; ++i) {
            microseconds *= 10;
        }
    }

    NSDate *date = [calendar dateFromComponents:dateComponents];

    NSTimeInterval epoch = [date timeIntervalSince1970];

    long long epochSeconds = epoch;

    return [[SLDateTime alloc] initWithTimeStamp:epochSeconds * 1000000LL + microseconds];
}


- (void)copyAllFieldsTo:(SLDateTime *)other
{
    [super copyAllFieldsTo:other];
    other->timeStamp_ = timeStamp_;
}


@end
