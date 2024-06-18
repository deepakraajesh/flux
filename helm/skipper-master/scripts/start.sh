#!/usr/bin/env bash

# Check if Java Binary is present or not
type java >/dev/null 2>&1 || {
    echo >&2 "JAVA binary not found. Aborting."; exit 1;
}

# Check if sleep time is enabled
if [ -n "$SLEEP_TIME" ]; then
    echo "sleeping for $SLEEP_TIME seconds ... "
    sleep $SLEEP_TIME
fi

CMD="java"

if [ -n "$JAVA_OPTS" ]; then
    echo "JAVA OPTS: $JAVA_OPTS"
else
    # Default Java Opts
    JAVA_OPTS=$(echo -e "-XX:+UseStringDeduplication" \
        "-Xms2048m" \
        "-Xmx2048m")
fi


if [ -n "$SYSTEM_OPTS" ]; then
    echo "SYSTEM_OPTS: $SYSTEM_OPTS"

    CMD="$CMD $SYSTEM_OPTS"
    echo "CMD: $CMD"
else
    echo "WARN: no system property specified, picking the default properties"
fi

if [ -n "$MONGO_HOST" ]; then
   echo "MONGO_HOST: $MONGO_HOST"
   CMD="$CMD -Dmongo=$MONGO_HOST"
else
   echo "No mongo host is provided in the environmental variable"
fi

echo "----------------------------------------------------------------"

CMD="$CMD $JAVA_OPTS -jar /skipper/skipper.jar"

echo "FINAL CMD: $CMD"
echo "----------------------------------------------------------------"

eval $CMD
