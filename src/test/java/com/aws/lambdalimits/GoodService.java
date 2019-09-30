package com.aws.lambdalimits;

import com.amazonaws.services.lambda.invoke.LambdaFunction;

public interface GoodService
{
    @LambdaFunction(functionName="lambdalimits-goodlambda")
    String testMe(String input) throws LambdaLimitsError;
}