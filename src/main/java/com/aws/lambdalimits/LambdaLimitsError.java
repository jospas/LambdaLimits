package com.aws.lambdalimits;

public class LambdaLimitsError extends RuntimeException
{
    public LambdaLimitsError(String message, Throwable t)
    {
        super(message, t);
    }
}
