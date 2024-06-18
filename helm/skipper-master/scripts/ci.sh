#!/bin/bash

# deploy.sh logs in ECR and pushes the image built

if [ -z "$REGIONS" ]; then 
    REGIONS[0]='us-east-1'
fi


for reg in "${REGIONS[@]}"; do
    echo "REGIONS: $reg"
done

# Check if AWS command exists or not

type aws > /dev/null 2>&1 || {
    echo >&2 "AWS command not found.Aborting"; exit 1;
}

if [ -z "$SERVICE_NAME" ]; then
    echo >&2 "Service Name not set. Aborting"; exit 1;
fi
echo "SERVICE_NAME: $SERVICE_NAME"

if [ -z "$SOURCE_TAG" ]; then
    SOURCE_TAG="latest"
fi

ECR_URL="012629307706.dkr.ecr.REGION.amazonaws.com"

echo "DEFAULT ECR_URL: $ECR_URL"

# login_ecr region
# Logs into ECR for a given region
login_ecr () {
    if [ -z "$1" ]; then
        echo >&2 "Incorrect Function Call. No Region Passed"; exit 1;
    fi

    region=$1

    echo "LOGIN to REGION: $region"

    eval $(aws ecr get-login --region $region --no-include-email);
}



# push_to_aws region tag
# Pushes the docker image to ECR repo
push_to_aws () {
    if [ -z "$1" ]; then
        echo >&2 "Incorrect Function Call. No Region Passed"; exit 1;
    fi

    if [ -z "$2" ]; then
        echo >&2 "Incorrect Function Call. No Tag Passed"; exit 1;
    fi


    region=$1
    if [ -z $PREFIX ]; then
       tag=$2
    else 
       tag="${PREFIX}-${2}"
    fi

    ecr_url=$(echo "${ECR_URL/REGION/$region}")

    echo "PUSHING DOCKER IMAGE"
    echo "--------------------"
    echo "REG: $reg"
    echo "TAG: $tag"
    echo "ECR: $ecr_url"

    docker tag $SERVICE_NAME:$SOURCE_TAG $ecr_url/$SERVICE_NAME:$tag

    docker push $ecr_url/$SERVICE_NAME:$tag

    echo "DONE!!"

}

# Check if build is running on github
if [ ! -z "$GITHUB_ACTIONS" ]; then

    # Login to ECR
    for reg in "${REGIONS[@]}"; do
        echo "FOR REGION: $reg"
        echo "---------------------------------------------------------"

        login_ecr $reg

        if [ -z "$GITHUB_HEAD_REF" ]; then

            push_to_aws $reg latest
            if [ -n "$TAG" ]; then
                push_to_aws $reg $TAG
            fi


        else

            pr_tag="PR_${GITHUB_ACTOR}_${GITHUB_HEAD_REF}";
            push_to_aws $reg $pr_tag

        fi

    done
fi
