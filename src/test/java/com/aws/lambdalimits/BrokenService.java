package com.aws.lambdalimits;

import com.amazonaws.services.lambda.invoke.LambdaFunction;

public interface BrokenService
{
    @LambdaFunction(functionName="lambdalimits-brokenlambda")
    String breakMe(String input) throws LambdaLimitsError;
}