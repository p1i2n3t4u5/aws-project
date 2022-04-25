package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.ses.actions.Lambda;
import software.amazon.awscdk.services.ses.actions.LambdaInvocationType;
import software.constructs.Construct;

public class AwsProjectStack extends Stack {
	
	private static String imageBucket = "cdk-rekn-imagebucket";
	
    public AwsProjectStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AwsProjectStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        Bucket bucket = Bucket.Builder.create(this, imageBucket).versioned(true).removalPolicy(RemovalPolicy.DESTROY)
				.autoDeleteObjects(true).build();
		CfnOutput.Builder.create(this, "MyBucketOutput").description("The Name of the bucket")
				.value(bucket.getBucketName()).build();
		CfnOutput.Builder.create(this, "MyBucketOutput1").description("The Arn of the bucket")
		.value(bucket.getBucketArn()).build();
		
		Role role = Role.Builder.create(this, "Role")
		        .assumedBy(new ServicePrincipal("lambda.amazonaws.com")).build();
		
		PolicyStatement policyStatement = new PolicyStatement();
		policyStatement.setEffect(Effect.ALLOW);
		policyStatement.addActions("rekognition:*", "logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents");
		policyStatement.addAllResources();
		role.addToPolicy(policyStatement);
		
		TableProps tableProps;
		Attribute partitionKey = Attribute.builder().name("Image").type(AttributeType.STRING).build();
		tableProps = TableProps.builder().tableName("cdk-rekn-imagetable").partitionKey(partitionKey)
				// The default removal policy is RETAIN, which means that cdk destroy will not
				// attempt to delete
				// the new table, and it will remain in your account until manually deleted. By
				// setting the policy to
				// DESTROY, cdk destroy will delete the table (even if it has data in it)
				.removalPolicy(RemovalPolicy.DESTROY).build();
		Table dynamodbTable = new Table(this, "cdk-rekn-imagetable", tableProps);
		
		
		Map<String, String> environment = new HashMap<>();
		environment.put("TABLE", dynamodbTable.getTableName());
		environment.put("BUCKET", bucket.getBucketName());

		Function function = Function.Builder.create(this, "cdk-rekn-function")
				.runtime(software.amazon.awscdk.services.lambda.Runtime.PYTHON_3_8).handler("index.handler")
				.role(role)
				.code(Code.fromAsset("src\\main\\java\\com\\myorg\\lambda")).environment(environment).build();
		
		function.addEventSource(S3EventSource.Builder.create(bucket)
		         .events(Arrays.asList(EventType.OBJECT_CREATED))
		         .build());

		Lambda.Builder.create().function(function)
				.invocationType(LambdaInvocationType.EVENT).build();

		bucket.grantReadWrite(function);
		dynamodbTable.grantFullAccess(function);
		
    }
}
