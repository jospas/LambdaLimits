## Testing Lambda Limits through use of HttpClient

Test code for replicating too many open files errors in AWS Lambda

### Introduction

Improper file handling and socket bleed can cause issues with java Lambda functions resulting in:

	java.io.IOException: Too many open files
	
See: [https://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html#d5e145]()

### Implementation

The project aims to provide a minimal replication of the error and logs open files and sockets on the target system.


### Building

Build using Maven without tests:


```bash
mvn -Dmaven.test.skip=true package
```

See: [https://whirlysworld.blogspot.com/2016/03/aws-lambda-java-deployment-maven-build.html]()

### Installing

Use the AWS CLI to prepare your AWS SAM template (note do not provide a trailing slash for s3-prefix). You will need a bucket in the deployment region to land the deployment assets:

```bash
aws cloudformation package \
    --template-file lambdalimits.yaml \
    --output-template-file lambdalimits-output.yaml \
    --s3-bucket <s3 bucket> \
    --s3-prefix <s3 key prefix> \
    --profile <profile name> \
    --force-upload
```

For example:

```bash
aws cloudformation package \
    --template-file lambdalimits.yaml \
    --output-template-file lambdalimits-output.yaml \
    --s3-bucket aws-joshpas-home \
    --s3-prefix apps/lambdalimits/deploy \
    --profile aws-josh \
    --force-upload
```

The deploy your packaged AWS CloudFormation stack with:

```bash
aws cloudformation deploy \
    --template-file lambdalimits-output.yaml \
    --profile <profile name> \
    --capabilities CAPABILITY_NAMED_IAM \
    --region <aws region> \
    --stack-name <stack name>
```

For example:

```bash
aws cloudformation deploy \
    --template-file lambdalimits-output.yaml \
    --profile aws-josh \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ap-southeast-2 \
    --stack-name lambdalimits
```

Note: this needs --capabilities CAPABILITY_NAMED_IAM as it creates a role for AWS Lambda.

### Sample outputs

The following is written to CloudWatch and returned to the caller:

```
BrokenLambda: [76ec1dfe-cc27-4167-abd3-3d8087d21693]	Invocation: [3]	Cost: [527] millis
	Inbound: [0]	Outbound: [6]	Established: [3]	Time wait: [3]	Open Files: [190]	Max Open Files: [1024]
	Connected sockets:
		172.217.167.67	tcp	ESTABLISHED	2
		172.217.167.67	tcp	TIME_WAIT	1
		172.217.167.68	tcp	ESTABLISHED	1
		172.217.167.68	tcp	TIME_WAIT	2
	Open file count: 85
		/dev/null 1
		/dev/random 3
		/dev/urandom 3
		/proc/1/fd/1 1
		/proc/1/fd/13 1
		/proc/1/fd/17 1
		...
		/tmp/broken1046512570113405320.txt 1
		/tmp/broken1117137188070690525.txt 1
		/tmp/broken1280529507462017689.txt 1
		/tmp/broken1362261266219634364.txt 1
		...
		/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.201.b09-0.43.amzn1.x86_64/jre/lib/ext/sunec.jar 1
		/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.201.b09-0.43.amzn1.x86_64/jre/lib/ext/sunjce_provider.jar 1
		...
		/var/runtime/lib/LambdaJavaRTEntry-1.0.jar 1
		/var/runtime/lib/LambdaSandboxJava-1.0.jar 1
		/var/runtime/lib/aws-lambda-java-core-1.2.0.jar 1
		/var/runtime/lib/gson-2.3.1.jar 1
		/var/runtime/lib/jackson-annotations-2.6.x.jar 1
		/var/runtime/lib/jackson-core-2.6.x.jar 1
		/var/runtime/lib/jackson-databind-2.6.x.jar 1
		/var/runtime/lib/joda-time-2.8.2.jar 1
		/var/runtime/lib/json-20160810-javadoc.jar 1
		/var/runtime/lib/json-20160810-sources.jar 1
		/var/task/lib/commons-codec-1.11.jar 1
		/var/task/lib/commons-logging-1.2.jar 1
		/var/task/lib/httpclient-4.5.10.jar 1
		/var/task/lib/httpcore-4.4.12.jar 1
		/var/task/lib/sigar.jar 1
```

### Uninstalling

Using the AWS Console, identify and delete the deployed AWS CloudFormation stack.

Or using the AWS CLI:

```bash
aws cloudformation delete-stack \
    --stack-name <stack name> \
    --profile <profile name> \
    --region <aws region>
```

For example:

```bash
aws cloudformation delete-stack \
    --stack-name lambdalimits \
    --profile aws-josh \
    --region ap-southeast-2
```

Additionally remove deployed code bundles from S3.

### Authors

**Josh Passenger** AWS Solutions Architect - [jospas@amazon.com](mailto:jospas@amazon.com)

### License

This project is licensed under the Apache 2 License - see the [LICENSE.txt](LICENSE.txt) file for details

### Warranty

No warranty is provided or implied with this software, 
it is provided as a Proof of Concept (POC) example and will require additional error checking code and testing.
